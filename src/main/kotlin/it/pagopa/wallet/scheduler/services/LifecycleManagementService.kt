package it.pagopa.wallet.scheduler.services

import it.pagopa.wallet.documents.wallets.Wallet
import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementQueryConfig
import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementTtlConfig
import it.pagopa.wallet.scheduler.exceptions.NoWalletFoundException
import it.pagopa.wallet.scheduler.repositories.WalletBulkRepository
import it.pagopa.wallet.scheduler.repositories.WalletRepository
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.temporal.TemporalAmount
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class LifecycleManagementService(
    @param:Autowired val repository: WalletRepository,
    @param:Autowired val walletBulkRepository: WalletBulkRepository,
    private val ttlConfig: LifecycleManagementTtlConfig,
    private val queryConfig: LifecycleManagementQueryConfig
) {
    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun setWalletsTtl(endDate: Instant): Mono<Int> {
        val queryRate = queryConfig.lifeCycleManagementTimeBasedRate.calculateRate()
        return repository
            .findByTtlNullAndStatusNotInAndUpdateDateBefore(
                queryConfig.excludedStatuses,
                endDate.toString(),
                queryRate
            )
            .doFirst {
                logger.info(
                    "Searching wallets for lifecycle management. End date [{}] - Excluded statuses  [{}] - Limit [{}]",
                    endDate,
                    queryConfig.excludedStatuses,
                    queryRate
                )
            }
            .doOnError { logger.error("Wallets search query failed!", it) }
            .switchIfEmpty(Mono.error(NoWalletFoundException()))
            .collectMap({ wallet -> wallet.id }, { wallet -> calculateTtl(wallet) })
            .flatMap { walletBulkRepository.bulkUpdateTtl(it) }
    }

    private fun calculateTtl(wallet: Wallet): Int {
        val calculatedTtl: TemporalAmount =
            if (
                wallet.validationOperationResult == "EXECUTED" ||
                    wallet.status == "DELETED" ||
                    wallet.status == "REPLACED"
            ) {
                Period.ofYears(ttlConfig.longTermRetentionYears)
            } else {
                Duration.ofDays(ttlConfig.shortTermRetentionDays.toLong())
            }

        val whenToDelete = wallet.updateDate + calculatedTtl
        val ttl = Duration.between(Instant.now(), whenToDelete).toSeconds().toInt()
        return if (ttl > 0) ttl else ttlConfig.instantDeleteTtlSeconds
    }
}
