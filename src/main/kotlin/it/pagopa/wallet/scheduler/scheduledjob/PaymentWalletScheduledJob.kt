package it.pagopa.wallet.scheduler.scheduledjob

import it.pagopa.wallet.scheduler.config.properties.PaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.jobs.config.OnboardedPaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.jobs.paymentwallet.OnboardedPaymentWalletJob
import it.pagopa.wallet.scheduler.service.SchedulerLockService
import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PaymentWalletScheduledJob(
    @Autowired private val onboardedPaymentWalletJob: OnboardedPaymentWalletJob,
    @Autowired private val paymentWalletJobConfiguration: PaymentWalletJobConfiguration,
    @Autowired private val schedulerLockService: SchedulerLockService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${payment-wallet-job.execution.cron}")
    fun processOnboardedPaymentWallets() {
        val startTime = Instant.now()
        schedulerLockService
            .acquireJobSemaphore(
                jobName = onboardedPaymentWalletJob.id()
            )
            .doOnError { logger.error("Unable to start job without semaphore acquiring", it) }
            .flatMap {
                val semaphoreId = it
                onboardedPaymentWalletJob
                    .process(
                        OnboardedPaymentWalletJobConfiguration(
                            startDate = paymentWalletJobConfiguration.startDate,
                            endDate = paymentWalletJobConfiguration.endDate
                        )
                    )
                    .doOnSuccess {
                        logger.info(
                            "Payment wallets processed successfully. Latest created date processed wallet: [{}]",
                            it
                        )
                    }
                    .doOnError { logger.error("Exception processing payment wallets", it) }
                    .doFinally {
                        logger.info(
                            "Overall processing completed. Elapsed time: [{}]",
                            Duration.between(startTime, Instant.now())
                        )
                        schedulerLockService.releaseJobSemaphore(
                            jobName = onboardedPaymentWalletJob.id(),
                            semaphoreId = semaphoreId
                        )
                    }
            }
            .subscribe()
    }
}
