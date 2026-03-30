package it.pagopa.wallet.scheduler.services

import it.pagopa.wallet.documents.wallets.Wallet
import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementConfiguration
import it.pagopa.wallet.scheduler.repositories.WalletBulkRepository
import it.pagopa.wallet.scheduler.repositories.WalletRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class LifecycleManagementService(
    @param:Autowired val repository: WalletRepository,
    @param:Autowired val walletBulkRepository: WalletBulkRepository,
    private val lifecycleManagementConfiguration: LifecycleManagementConfiguration
) {
    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun setWalletsTtl(endDate: Instant): Mono<Int> {
        return repository
            .findByTtlNullAndStatusNotInAndUpdateDateBefore(
                lifecycleManagementConfiguration.excludedStatuses,
                endDate.toString(),
                lifecycleManagementConfiguration.limit
            ).doFirst {
                logger.info(
                    "Searching wallets for lifecycle management. End date [{}] - Excluded statuses  [{}] - Limit [{}]",
                    endDate,
                    lifecycleManagementConfiguration.excludedStatuses,
                    lifecycleManagementConfiguration.limit
                )
            }.doOnError {
                logger.error("Wallets search query failed!", it)
            }
            .collectMap({ wallet -> wallet.id }, { wallet -> calculateTtl(wallet) })
            .flatMap { walletBulkRepository.bulkUpdateTtl(it) }
    }

    private fun calculateTtl(wallet: Wallet): Int {
        val defaultTtl =
            if (
                wallet.validationOperationResult == "EXECUTED" ||
                    wallet.status == "DELETED" ||
                    wallet.status == "REPLACED"
            ) {
                lifecycleManagementConfiguration.deletedWalletTtl
            } else {
                lifecycleManagementConfiguration.errorWalletTtl
            }

        val secondsFromLastUpdate = Duration.between(wallet.updateDate, Instant.now()).toSeconds()
        val ttl = (defaultTtl - secondsFromLastUpdate).toInt()
        return if (ttl > 0) ttl else lifecycleManagementConfiguration.instantDeleteTtl
    }
}
