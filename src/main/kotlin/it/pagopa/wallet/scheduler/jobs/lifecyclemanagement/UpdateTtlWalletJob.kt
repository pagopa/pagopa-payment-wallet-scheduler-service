package it.pagopa.wallet.scheduler.jobs.lifecyclemanagement

import it.pagopa.wallet.scheduler.jobs.ScheduledJob
import it.pagopa.wallet.scheduler.jobs.config.UpdateJobConfiguration
import it.pagopa.wallet.scheduler.services.LifeCycleService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Payment wallet job: this job will scan all payment wallets in the given configuration retrieving
 * all the onboarded ones and populating the CDC queue
 */
@Component
class UpdateTtlWalletJob(
    @Autowired private val lifeCycleService: LifeCycleService,
) : ScheduledJob<UpdateJobConfiguration, Int> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun id(): String = "lifecycle-udpate-ttl-wallet-job"

    override fun process(configuration: UpdateJobConfiguration): Mono<Int> {
        val limit = configuration.limit
        logger.info("Starting delete lifecycle wallet processing with limit {} ", limit)
        return lifeCycleService.updateTtlBulk(100)
    }



}
