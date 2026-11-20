package it.pagopa.wallet.scheduler.services

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import it.pagopa.wallet.documents.wallets.ContractDeactivationDlqDocument
import it.pagopa.wallet.documents.wallets.Wallet
import it.pagopa.wallet.scheduler.WalletTestUtils
import it.pagopa.wallet.scheduler.client.WalletServiceClient
import it.pagopa.wallet.scheduler.common.tracing.TracingUtils
import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementQueryConfig
import it.pagopa.wallet.scheduler.config.properties.QuerySettings
import it.pagopa.wallet.scheduler.exceptions.ContractDeactivationBadGatewayException
import it.pagopa.wallet.scheduler.exceptions.WalletServiceException
import it.pagopa.wallet.scheduler.repositories.WalletBulkRepository
import it.pagopa.wallet.scheduler.repositories.WalletContractDeactivationDlqRepository
import it.pagopa.wallet.scheduler.repositories.WalletRepository
import it.pagopa.wallet.scheduler.services.LifecycleManagementService.SetWalletsTtlResult
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.test.publisher.PublisherProbe

class LifecycleManagementServiceTest {
    private val walletRepository: WalletRepository = mock()
    private val dlqRepository: WalletContractDeactivationDlqRepository = mock()
    private val walletBulkRepository: WalletBulkRepository = mock()
    private val walletServiceClient: WalletServiceClient = mock()
    private val tracingUtils: TracingUtils = mock()
    private val endDate = Instant.parse("2026-11-19T14:00:00Z")
    private val statuses =
        setOf("CREATED", "INITIALIZED", "VALIDATION_REQUESTED", "ERROR", "DELETED", "REPLACED")
    private val invocationDelay = Duration.ofMillis(500)
    private val queryLimit = 12
    private val queryConfig =
        LifecycleManagementQueryConfig(
            QuerySettings(10, 10, 60, LocalTime.of(10, 0), LocalTime.of(12, 0)),
            contractDeactivationMaxRetries = 3,
            contractDeactivationLowRateDelayMs = invocationDelay.toMillis(),
            contractDeactivationHighRateDelayMs = invocationDelay.toMillis(),
            ttlSeconds = 600
        )
    private val service =
        LifecycleManagementService(
            walletRepository,
            dlqRepository,
            walletBulkRepository,
            walletServiceClient,
            queryConfig,
            tracingUtils
        )

    @Test
    fun `should use supplied query limit and skip writes when no wallets are found`() {
        stubSearch(Flux.empty())

        StepVerifier.create(service.setWalletsTtl(endDate, queryLimit, invocationDelay))
            .expectNext(SetWalletsTtlResult(0, ""))
            .verifyComplete()

        verify(walletRepository)
            .findWalletToDelete(
                statuses,
                endDate.toString(),
                queryConfig.contractDeactivationMaxRetries,
                queryLimit
            )
        verifyNoInteractions(walletServiceClient, walletBulkRepository, dlqRepository, tracingUtils)
    }

    @ParameterizedTest
    @CsvSource(
        "false, , , false",
        "false, EXECUTED, , false",
        "true, , , true",
        "true, EXECUTED, , true",
        "true, AUTHORIZED, CARD_VERIFICATION, true",
        "true, AUTHORIZED, , false",
        "true, DECLINED, CARD_VERIFICATION, false"
    )
    fun `should require deactivation only for eligible contracts`(
        hasContract: Boolean,
        validationResult: String?,
        validationType: String?,
        requiresDeactivation: Boolean
    ) {
        val wallet =
            wallet().let {
                it.copy(
                    contractId = it.contractId.takeIf { hasContract },
                    validationOperationResult = validationResult,
                    validationOperationType = validationType
                )
            }
        stubSearch(Flux.just(wallet))
        if (requiresDeactivation) {
            whenever(walletServiceClient.deactivateContract(wallet.id)).thenReturn(Mono.empty())
        }
        whenever(walletBulkRepository.bulkUpdateTtl(listOf(wallet.id), queryConfig.ttlSeconds))
            .thenReturn(Mono.just(1))

        StepVerifier.withVirtualTime { service.setWalletsTtl(endDate, queryLimit, invocationDelay) }
            .thenAwait(invocationDelay)
            .expectNext(SetWalletsTtlResult(1, wallet.updateDate.toString()))
            .verifyComplete()

        if (requiresDeactivation) {
            inOrder(walletServiceClient, walletBulkRepository) {
                verify(walletServiceClient).deactivateContract(wallet.id)
                verify(walletBulkRepository)
                    .bulkUpdateTtl(listOf(wallet.id), queryConfig.ttlSeconds)
            }
        } else {
            verifyNoInteractions(walletServiceClient)
            verify(walletBulkRepository).bulkUpdateTtl(listOf(wallet.id), queryConfig.ttlSeconds)
        }
        verifyNoInteractions(dlqRepository)
    }

    @Test
    fun `should pace contract deactivation calls at the configured rate`() {
        val first = wallet()
        val second = wallet()
        stubSearch(Flux.just(first, second))
        whenever(walletServiceClient.deactivateContract(first.id)).thenReturn(Mono.empty())
        whenever(walletServiceClient.deactivateContract(second.id)).thenReturn(Mono.empty())
        whenever(
                walletBulkRepository.bulkUpdateTtl(
                    listOf(first.id, second.id),
                    queryConfig.ttlSeconds
                )
            )
            .thenReturn(Mono.just(2))

        val interval = invocationDelay
        val tick = Duration.ofMillis(1)
        StepVerifier.withVirtualTime { service.setWalletsTtl(endDate, queryLimit, invocationDelay) }
            .thenAwait(interval.minus(tick))
            .then { verifyNoInteractions(walletServiceClient) }
            .thenAwait(tick)
            .then {
                verify(walletServiceClient).deactivateContract(first.id)
                verifyNoMoreInteractions(walletServiceClient)
            }
            .thenAwait(interval.minus(tick))
            .then { verifyNoMoreInteractions(walletServiceClient) }
            .thenAwait(tick)
            .expectNext(SetWalletsTtlResult(2, first.updateDate.toString()))
            .verifyComplete()

        verify(walletServiceClient).deactivateContract(second.id)
    }

    @Test
    fun `should update and trace only eligible wallets but advance past a newer failed wallet`() {
        val direct = wallet(secondsBeforeEnd = 3).copy(contractId = null, status = "ERROR")
        val deactivated = wallet(secondsBeforeEnd = 2)
        val failed = wallet(secondsBeforeEnd = 1)
        val failure = WalletServiceException(failed.id, IllegalStateException("API unavailable"))
        stubSearch(Flux.just(deactivated, failed, direct))
        whenever(walletServiceClient.deactivateContract(deactivated.id)).thenReturn(Mono.empty())
        whenever(walletServiceClient.deactivateContract(failed.id)).thenReturn(Mono.error(failure))
        whenever(walletBulkRepository.bulkUpdateTtl(any(), eq(queryConfig.ttlSeconds)))
            .thenReturn(Mono.just(1))

        StepVerifier.withVirtualTime { service.setWalletsTtl(endDate, queryLimit, invocationDelay) }
            .thenAwait(invocationDelay.multipliedBy(2))
            .expectNext(SetWalletsTtlResult(1, failed.updateDate.toString()))
            .verifyComplete()

        val ids = argumentCaptor<List<String>>()
        verify(walletBulkRepository).bulkUpdateTtl(ids.capture(), eq(queryConfig.ttlSeconds))
        assertEquals(2, ids.firstValue.size)
        assertEquals(setOf(direct.id, deactivated.id), ids.firstValue.toSet())
        verify(tracingUtils).addSpan("payWalletLifeCycleItem", expectedAttributes(direct))
        verify(tracingUtils).addSpan("payWalletLifeCycleItem", expectedAttributes(deactivated))
        verifyNoMoreInteractions(tracingUtils)
        verifyNoInteractions(dlqRepository)
    }

    @ParameterizedTest
    @CsvSource("true, ", "true, 0", "true, 1", "true, 3", "false, 2")
    fun `should skip TTL and DLQ writes when a failed deactivation does not exhaust retries`(
        badGateway: Boolean,
        retryCount: Int?
    ) {
        val wallet = wallet().copy(deactivationRetryCount = retryCount)
        val cause = IllegalStateException("Deactivation failed")
        val failure =
            if (badGateway) ContractDeactivationBadGatewayException(wallet.id, cause)
            else WalletServiceException(wallet.id, cause)
        stubSearch(Flux.just(wallet))
        whenever(walletServiceClient.deactivateContract(wallet.id)).thenReturn(Mono.error(failure))
        stubEmptyTtlUpdate()

        StepVerifier.withVirtualTime { service.setWalletsTtl(endDate, queryLimit, invocationDelay) }
            .thenAwait(invocationDelay)
            .expectNext(SetWalletsTtlResult(0, wallet.updateDate.toString()))
            .verifyComplete()

        verify(walletBulkRepository).bulkUpdateTtl(emptyList(), queryConfig.ttlSeconds)
        verifyNoInteractions(dlqRepository, tracingUtils)
    }

    @Test
    fun `should save the wallet in the DLQ before finishing when bad gateway exhausts retries`() {
        val wallet = stubLastRetryFailure()
        val save = PublisherProbe.empty<ContractDeactivationDlqDocument>()
        whenever(dlqRepository.save(any<ContractDeactivationDlqDocument>())).thenAnswer {
            val document = it.getArgument<ContractDeactivationDlqDocument>(0)
            save.mono().thenReturn(document)
        }
        whenever(walletBulkRepository.bulkUpdateTtl(emptyList(), queryConfig.ttlSeconds))
            .thenAnswer {
                save.assertWasSubscribed()
                Mono.just(0)
            }
        val before = Instant.now()

        StepVerifier.withVirtualTime { service.setWalletsTtl(endDate, queryLimit, invocationDelay) }
            .thenAwait(invocationDelay)
            .expectNext(SetWalletsTtlResult(0, wallet.updateDate.toString()))
            .verifyComplete()

        val document = argumentCaptor<ContractDeactivationDlqDocument>()
        verify(dlqRepository).save(document.capture())
        assertEquals(wallet.id, document.firstValue.id)
        val timestamp = OffsetDateTime.parse(document.firstValue.timestamp).toInstant()
        assertTrue(timestamp in before..Instant.now())
        verify(walletBulkRepository).bulkUpdateTtl(emptyList(), queryConfig.ttlSeconds)
        verifyNoInteractions(tracingUtils)
    }

    @Test
    fun `should propagate a DLQ save failure without applying TTL`() {
        stubLastRetryFailure()
        val failure = IllegalStateException("DLQ write failed")
        whenever(dlqRepository.save(any<ContractDeactivationDlqDocument>()))
            .thenReturn(Mono.error(failure))

        StepVerifier.withVirtualTime { service.setWalletsTtl(endDate, queryLimit, invocationDelay) }
            .thenAwait(invocationDelay)
            .expectErrorMatches { it === failure }
            .verify()

        verifyNoInteractions(walletBulkRepository, tracingUtils)
    }

    @Test
    fun `should propagate a wallet search failure without processing any wallets`() {
        val failure = IllegalStateException("Wallet search failed")
        stubSearch(Flux.error(failure))

        StepVerifier.create(service.setWalletsTtl(endDate, queryLimit, invocationDelay))
            .expectErrorMatches { it === failure }
            .verify()

        verifyNoInteractions(walletServiceClient, walletBulkRepository, dlqRepository, tracingUtils)
    }

    @Test
    fun `should propagate a bulk TTL update failure without tracing wallets`() {
        val wallet = wallet().copy(contractId = null)
        val failure = IllegalStateException("TTL update failed")
        stubSearch(Flux.just(wallet))
        whenever(walletBulkRepository.bulkUpdateTtl(listOf(wallet.id), queryConfig.ttlSeconds))
            .thenReturn(Mono.error(failure))

        StepVerifier.create(service.setWalletsTtl(endDate, queryLimit, invocationDelay))
            .expectErrorMatches { it === failure }
            .verify()

        verifyNoInteractions(walletServiceClient, dlqRepository, tracingUtils)
    }

    private fun wallet(secondsBeforeEnd: Long = 1): Wallet =
        WalletTestUtils.cardWalletDocument("DELETED")
            .copy(updateDate = endDate.minusSeconds(secondsBeforeEnd), ttl = null)

    private fun stubSearch(wallets: Flux<Wallet>) {
        whenever(walletRepository.findWalletToDelete(any(), any(), any(), any()))
            .thenReturn(wallets)
    }

    private fun stubEmptyTtlUpdate() {
        whenever(walletBulkRepository.bulkUpdateTtl(emptyList(), queryConfig.ttlSeconds))
            .thenReturn(Mono.just(0))
    }

    private fun stubLastRetryFailure(): Wallet {
        val wallet =
            wallet().copy(deactivationRetryCount = queryConfig.contractDeactivationMaxRetries - 1)
        val failure =
            ContractDeactivationBadGatewayException(wallet.id, IllegalStateException("Bad gateway"))
        stubSearch(Flux.just(wallet))
        whenever(walletServiceClient.deactivateContract(wallet.id)).thenReturn(Mono.error(failure))
        return wallet
    }

    private fun expectedAttributes(wallet: Wallet): Attributes =
        Attributes.of(
            AttributeKey.stringKey("payWallet.lifeCycle.item.walletStatus"),
            wallet.status,
            AttributeKey.longKey("payWallet.lifeCycle.item.ttlApplied"),
            queryConfig.ttlSeconds.toLong(),
            AttributeKey.stringKey("payWallet.lifeCycle.item.walletId"),
            wallet.id
        )
}
