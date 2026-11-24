package it.pagopa.wallet.scheduler.services

import it.pagopa.wallet.documents.wallets.ContractDeactivationDlqDocument
import it.pagopa.wallet.documents.wallets.Wallet
import it.pagopa.wallet.scheduler.client.WalletServiceClient
import it.pagopa.wallet.scheduler.common.tracing.TracingUtils
import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementQueryConfig
import it.pagopa.wallet.scheduler.repositories.WalletBulkRepository
import it.pagopa.wallet.scheduler.repositories.WalletContractDeactivationDlqRepository
import it.pagopa.wallet.scheduler.repositories.WalletRepository
import it.pagopa.wallet.scheduler.utils.LifeCycleTracerUtils
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class LifecycleManagementService(
    @param:Autowired val repository: WalletRepository,
    @param:Autowired
    val walletContractDeactivationDlqRepository: WalletContractDeactivationDlqRepository,
    @param:Autowired val walletBulkRepository: WalletBulkRepository,
    @param:Autowired val walletServiceClient: WalletServiceClient,
    private val queryConfig: LifecycleManagementQueryConfig,
    private val tracingUtils: TracingUtils
) {
    companion object {
        private val ALLOWED_STATUSES =
            setOf(
                "CREATED",
                "INITIALIZED",
                "VALIDATION_REQUESTED",
                "ERROR",
                "DELETED",
                "REPLACED"
            ) // All possible statuses except for "VALIDATED"
    }

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    data class SetWalletsTtlResult(val updatedWallets: Int, val lastProcessedTimestamp: String)

    private data class WalletEvaluation(val walletToDelete: Wallet?, val updateDate: String)

    fun setWalletsTtl(
        endDate: Instant,
        queryLimit: Int,
        invocationDelay: Duration
    ): Mono<SetWalletsTtlResult> {
        return repository
            .findWalletToDelete(
                ALLOWED_STATUSES,
                endDate.toString(),
                queryConfig.contractDeactivationMaxRetries,
                queryLimit
            )
            .doOnError { logger.error("Wallets search query failed!", it) }
            .publish { shared ->
                val directDeletes =
                    shared
                        .filter { !it.requiresContractDeactivation() }
                        .map {
                            WalletEvaluation(
                                walletToDelete = it,
                                updateDate = it.updateDate.toString()
                            )
                        }

                val apiDeletes =
                    shared
                        .filter { it.requiresContractDeactivation() }
                        .delayElements(invocationDelay)
                        .flatMap(::deactivateContract)

                Flux.merge(directDeletes, apiDeletes)
            }
            .collectList()
            .filter { it.isNotEmpty() }
            .flatMap { evaluations ->
                val lastProcessedTimestamp = evaluations.maxOf { it.updateDate }
                val walletsToDelete = evaluations.mapNotNull { it.walletToDelete }
                val idsToDelete = walletsToDelete.map { it.id }
                val ttl = queryConfig.ttlSeconds

                walletBulkRepository
                    .bulkUpdateTtl(idsToDelete, ttl)
                    .doOnNext {
                        walletsToDelete.forEach { wallet ->
                            val stat =
                                LifeCycleTracerUtils.WalletLifecycleItemStats(
                                    wallet.status,
                                    ttl.toLong(),
                                    wallet.id
                                )
                            tracingUtils.addSpan(
                                stat.WALLET_LIFECYCLE_ITEM_SPAN_NAME,
                                stat.getSpanAttributes(stat.status, stat.ttlApplied, stat.walletId)
                            )
                        }
                    }
                    .map { updatedCount ->
                        SetWalletsTtlResult(
                            updatedWallets = updatedCount,
                            lastProcessedTimestamp = lastProcessedTimestamp
                        )
                    }
            }
            .defaultIfEmpty(SetWalletsTtlResult(updatedWallets = 0, lastProcessedTimestamp = ""))
    }

    private fun deactivateContract(wallet: Wallet): Mono<WalletEvaluation> {
        val updateDate = wallet.updateDate.toString()
        return Mono.defer { walletServiceClient.deactivateContract(wallet.id) }
            .thenReturn(WalletEvaluation(wallet, updateDate))
            .onErrorResume {
                recordDeactivationFailure(wallet).thenReturn(WalletEvaluation(null, updateDate))
            }
    }

    private fun recordDeactivationFailure(wallet: Wallet): Mono<Void> {
        val attemptNumber = (wallet.deactivationRetryCount ?: 0) + 1
        val maxRetries = queryConfig.contractDeactivationMaxRetries
        return repository
            .updateDeactivationRetryCount(wallet.id, wallet.version, attemptNumber)
            .doOnError {
                logger.error(
                    "Could not update deactivation retry count for wallet ${wallet.id}",
                    it
                )
            }
            .flatMap { updatedCount ->
                if (updatedCount > 0L && attemptNumber == maxRetries)
                    saveDeactivationDlqEntry(wallet.id)
                else Mono.empty()
            }
    }

    private fun saveDeactivationDlqEntry(walletId: String): Mono<Void> =
        walletContractDeactivationDlqRepository
            .save(ContractDeactivationDlqDocument(walletId, OffsetDateTime.now().toString()))
            .doOnNext {
                logger.info(
                    "Wallet $walletId reached the deactivation retry limit and was added to the DLQ"
                )
            }
            .doOnError { logger.error("Could not write DLQ entry for wallet $walletId", it) }
            .then()

    private fun Wallet.requiresContractDeactivation(): Boolean =
        contractId != null &&
            (validationOperationResult == null ||
                validationOperationResult == "EXECUTED" ||
                (validationOperationType == "CARD_VERIFICATION" &&
                    validationOperationResult == "AUTHORIZED"))
}
