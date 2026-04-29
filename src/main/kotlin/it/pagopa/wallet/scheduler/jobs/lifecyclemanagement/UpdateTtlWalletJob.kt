package it.pagopa.wallet.scheduler.jobs.lifecyclemanagement

import it.pagopa.wallet.scheduler.common.tracing.TracingUtils
import it.pagopa.wallet.scheduler.jobs.ScheduledJob
import it.pagopa.wallet.scheduler.jobs.config.LifecycleManagementJobConfiguration
import it.pagopa.wallet.scheduler.services.LifecycleManagementService
import it.pagopa.wallet.scheduler.utils.LifeCycleTracerUtils
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Lifecycle management job: this job will scan all payment wallets without the ttl field and will
 * set it based on the status and validationOperationResult
 */
@Component
class UpdateTtlWalletJob(
    private val lifecycleManagementService: LifecycleManagementService,
    private val tracingUtils: TracingUtils
) : ScheduledJob<LifecycleManagementJobConfiguration, Int> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun id(): String = "lifecycle-management-wallet-job"

    override fun process(configuration: LifecycleManagementJobConfiguration): Mono<Int> {
        val endDate = configuration.endDate
        return lifecycleManagementService
            .setWalletsTtl(endDate)
            .doFirst {
                logger.info(
                    "Starting delete lifecycle wallet processing with end date {} ",
                    endDate
                )
            }
            .elapsed()
            .map { (elapsedMs, count) ->
                val lifecycleSessionStats =
                    LifeCycleTracerUtils.WalletLifecycleSessionStats(
                        totalItem = count.toLong(),
                        elapsedTime = elapsedMs,
                        lastProcessedTimestamp = Instant.now().toString()
                    )
                tracingUtils.addSpan(
                    lifecycleSessionStats.WALLET_LIFECYCLE_SESSION_SPAN_NAME,
                    lifecycleSessionStats.getSpanAttributes(
                        lifecycleSessionStats.totalItem,
                        lifecycleSessionStats.elapsedTime,
                        lifecycleSessionStats.lastProcessedTimestamp
                    )
                )
                count
            }
    }
}
