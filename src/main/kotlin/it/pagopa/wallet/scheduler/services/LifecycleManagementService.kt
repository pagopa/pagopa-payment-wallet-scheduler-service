package it.pagopa.wallet.scheduler.services

import it.pagopa.wallet.documents.wallets.ContractDeactivationDlqDocument
import it.pagopa.wallet.documents.wallets.Wallet
import it.pagopa.wallet.scheduler.client.WalletServiceClient
import it.pagopa.wallet.scheduler.common.tracing.TracingUtils
import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementQueryConfig
import it.pagopa.wallet.scheduler.exceptions.ContractDeactivationBadGatewayException
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
        return walletServiceClient
            .deactivateContract(wallet.id)
            .thenReturn(WalletEvaluation(wallet, updateDate))
            .onErrorResume { ex ->
                val failedEvaluation = WalletEvaluation(null, updateDate)
                if (
                    ex is ContractDeactivationBadGatewayException &&
                        wallet.deactivationRetryCount ==
                            queryConfig.contractDeactivationMaxRetries - 1
                ) {
                    // Explicit 502 received from wallet-service, the NPG contract deactivation API
                    // failed and the Wallet's deactivation retry counter has been incremented
                    val dlqDocument =
                        ContractDeactivationDlqDocument(
                            id = wallet.id,
                            timestamp = OffsetDateTime.now().toString()
                        )
                    return@onErrorResume walletContractDeactivationDlqRepository
                        .save(dlqDocument)
                        .doOnNext {
                            logger.info(
                                "Wallet ${wallet.id} has reached the maximum number of contract deactivation retries. Added to DLQ.",
                            )
                        }
                        .thenReturn(failedEvaluation)
                }
                Mono.just(failedEvaluation)
            }
    }

    private fun Wallet.requiresContractDeactivation(): Boolean =
        contractId != null &&
            (validationOperationResult == null ||
                validationOperationResult == "EXECUTED" ||
                (validationOperationType == "CARD_VERIFICATION" &&
                    validationOperationResult == "AUTHORIZED"))
}
