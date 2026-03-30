package it.pagopa.wallet.scheduler.jobs.lifecyclemanagement

import it.pagopa.wallet.scheduler.jobs.ScheduledJob
import it.pagopa.wallet.scheduler.jobs.config.LifecycleManagementJobConfiguration
import it.pagopa.wallet.scheduler.services.LifecycleManagementService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Lifecycle management job: this job will scan all payment wallets without the ttl field and will set it based on the status and validationOperationResult
 *
 */
@Component
class UpdateTtlWalletJob(
    private val lifecycleManagementService: LifecycleManagementService,
) : ScheduledJob<LifecycleManagementJobConfiguration, Int> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun id(): String = "lifecycle-management-wallet-job"

    override fun process(configuration: LifecycleManagementJobConfiguration): Mono<Int> {
        val endDate = configuration.endDate
        return lifecycleManagementService.setWalletsTtl(endDate).doFirst {
            logger.info("Starting delete lifecycle wallet processing with end date {} ", endDate)
        }
    }
}
