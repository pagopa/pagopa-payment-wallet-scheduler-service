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
        val calculatedTtl: TemporalAmount? =
            if (
                wallet.validationOperationResult == "EXECUTED" ||
                    wallet.status == "DELETED" ||
                    wallet.status == "REPLACED"
            ) {
                Period.ofYears(ttlConfig.longTermRetentionYears)
            } else if(
                wallet.status == "CREATED" ||
                wallet.status == "INITIALIZED" ||
                wallet.status == "VALIDATION_REQUESTED" ||
                wallet.status == "ERROR"
            ){
                Duration.ofDays(ttlConfig.shortTermRetentionDays.toLong())
            } else {
                // Other case we are not allow to delete
                return -1
            }
        logger.info("{}",)
        val whenToDelete = wallet.updateDate.atZone(ZoneOffset.UTC).plus(calculatedTtl)
        val ttlLong = Duration.between(Instant.now(), whenToDelete).toSeconds()
        val ttlInt =
            if (ttlLong < 0) {
                // Overflow case, this can happen if the value of 'whenToDelete' is more distant
                // than 68 years in the past
                // The convertion to Int of a Long too big will cause an overflow and change the
                // sign of the number
                if (ttlLong < Int.MIN_VALUE.toLong()) {
                    Int.MIN_VALUE
                } else {
                    -(ttlLong.toInt())
                }
            } else {
                // Overflow case, can happen if the value 'whenToDelete' is to distant in the
                // future, this could not be possible
                // only in case of error on the wallet data, the updateDate plus the calculcatedTtl
                // must be major that 68 years
                // The convertion to Int of a Long too big will cause an overflow and change the
                // sign of the number
                if (ttlLong > Int.MAX_VALUE.toLong()) {
                    Int.MAX_VALUE
                } else {
                    ttlLong.toInt()
                }
            }
        return if (ttlInt > 0) ttlInt else ttlConfig.instantDeleteTtlSeconds
    }
}
