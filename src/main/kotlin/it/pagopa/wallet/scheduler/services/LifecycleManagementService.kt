package it.pagopa.wallet.scheduler.services

import it.pagopa.wallet.documents.wallets.Wallet
import it.pagopa.wallet.scheduler.common.tracing.TracingUtils
import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementQueryConfig
import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementTtlConfig
import it.pagopa.wallet.scheduler.exceptions.NoWalletFoundException
import it.pagopa.wallet.scheduler.repositories.WalletBulkRepository
import it.pagopa.wallet.scheduler.repositories.WalletRepository
import it.pagopa.wallet.scheduler.utils.LifeCycleTracerUtils
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneOffset
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
    private val queryConfig: LifecycleManagementQueryConfig,
    private val tracingUtils: TracingUtils
) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    val shortTermAllowedStatuses = setOf("CREATED", "INITIALIZED", "VALIDATION_REQUESTED", "ERROR")
    val longTermAllowedStatuses = setOf("DELETED", "REPLACED")
    val npgValidOperationResult = setOf("EXECUTED")

    fun setWalletsTtl(endDate: Instant): Mono<Int> {
        val queryRate = queryConfig.lifeCycleManagementTimeBasedRate.calculateRate()
        val searchedStatuses = shortTermAllowedStatuses union longTermAllowedStatuses
        return repository
            .findByTtlNullAndStatusInAndUpdateDateBefore(
                searchedStatuses,
                endDate.toString(),
                queryRate
            )
            .doFirst {
                logger.info(
                    "Searching wallets for lifecycle management. End date [{}] - Statuses  [{}] - Limit [{}]",
                    endDate,
                    searchedStatuses,
                    queryRate
                )
            }
            .doOnError { logger.error("Wallets search query failed!", it) }
            .switchIfEmpty(Mono.error(NoWalletFoundException()))
            .collectMap(
                { wallet -> wallet.id },
                { wallet ->
                    val ttl = calculateTtl(wallet)
                    val lifecycleItemStats =
                        LifeCycleTracerUtils.WalletLifecycleItemStats(wallet.status, ttl.toLong())
                    tracingUtils.addSpan(
                        lifecycleItemStats.WALLET_LIFECYCLE_ITEM_SPAN_NAME,
                        lifecycleItemStats.getSpanAttributes(
                            lifecycleItemStats.status,
                            lifecycleItemStats.ttlApplied
                        )
                    )
                    ttl
                }
            )
            .flatMap { walletBulkRepository.bulkUpdateTtl(it) }
    }

    private fun calculateTtl(wallet: Wallet): Int {
        val isNpgValidOperationResult = wallet.validationOperationResult in npgValidOperationResult
        val isLongTermStatus = wallet.status in longTermAllowedStatuses
        val isShortTermStatus = wallet.status in shortTermAllowedStatuses
        val calculatedTtl: TemporalAmount? =
            if (isLongTermStatus || isNpgValidOperationResult) {
                Period.ofYears(ttlConfig.longTermRetentionYears)
            } else if (isShortTermStatus) {
                Duration.ofDays(ttlConfig.shortTermRetentionDays.toLong())
            } else {
                // Other case we are not allow to delete
                null
            }
        logger.debug(
            "wallet: [{}], isNpgValidOperationResult: [{}], isLongTermStatus: [{}], isShortTermStatus: [{}] -> Calculated ttl: [{}]",
            wallet,
            isNpgValidOperationResult,
            isLongTermStatus,
            isShortTermStatus,
            calculatedTtl
        )
        if (calculatedTtl == null) {
            logger.warn(
                "Unhandled wallet status: [{}], wallet id: [{}]. TTL field will be set to -1 (not expire)",
                wallet.status,
                wallet.id
            )
            return -1
        }
        val whenToDelete = wallet.updateDate.atZone(ZoneOffset.UTC).plus(calculatedTtl)
        // safe cast to int here: configuration values range checked during service startup, here
        // cannot be greater than 68 years
        val ttl = Duration.between(Instant.now(), whenToDelete).toSeconds().toInt()
        return if (ttl > 0) ttl else ttlConfig.instantDeleteTtlSeconds
    }
}
