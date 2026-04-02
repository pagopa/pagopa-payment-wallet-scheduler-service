package it.pagopa.wallet.scheduler.scheduledjob

import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementExecutionConfig
import it.pagopa.wallet.scheduler.jobs.config.LifecycleManagementJobConfiguration
import it.pagopa.wallet.scheduler.jobs.lifecyclemanagement.UpdateTtlWalletJob
import it.pagopa.wallet.scheduler.service.SchedulerLockService
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class LifeCycleManagementScheduledJob(
    private val updateTtlWalletJob: UpdateTtlWalletJob,
    private val jobConfiguration: LifecycleManagementExecutionConfig,
    private val schedulerLockService: SchedulerLockService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${lifecycle-management-job.execution.cron}")
    fun processLifeCycleWallets() {
        val startTime = Instant.now()
        schedulerLockService
            .acquireJobSemaphore(jobName = updateTtlWalletJob.id())
            .doOnError { logger.error("Unable to start job without semaphore acquiring", it) }
            .flatMap { semaphoreId ->
                updateTtlWalletJob
                    .process(
                        LifecycleManagementJobConfiguration(
                            Instant.now().minus(jobConfiguration.daysThreshold, ChronoUnit.DAYS)
                        )
                    )
                    .doOnSuccess {
                        logger.info("Lifecycle management job completed. Updated wallets: {}", it)
                    }
                    .doOnError { logger.error("Exception processing lifecycle process", it) }
                    .doFinally {
                        logger.info(
                            "Overall processing completed. Elapsed time: [{}]",
                            Duration.between(startTime, Instant.now())
                        )
                    }
                    .onErrorResume { Mono.empty() }
                    .thenReturn(semaphoreId)
            }
            .flatMap {
                schedulerLockService.releaseJobSemaphore(
                    jobName = updateTtlWalletJob.id(),
                    semaphoreId = it
                )
            }
            .subscribe()
    }
}
