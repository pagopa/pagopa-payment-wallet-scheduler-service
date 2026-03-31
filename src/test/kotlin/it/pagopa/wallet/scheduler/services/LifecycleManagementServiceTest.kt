package it.pagopa.wallet.scheduler.services

import it.pagopa.wallet.documents.wallets.Wallet
import it.pagopa.wallet.scheduler.WalletTestUtils
import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementQueryConfig
import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementTtlConfig
import it.pagopa.wallet.scheduler.config.properties.QuerySettings
import it.pagopa.wallet.scheduler.exceptions.NoWalletFoundException
import it.pagopa.wallet.scheduler.repositories.WalletBulkRepository
import it.pagopa.wallet.scheduler.repositories.WalletRepository
import java.time.Instant
import java.time.LocalTime
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class LifecycleManagementServiceTest {
    companion object {
        const val DELETED_WALLET_TTL = 3600
        const val ERROR_WALLET_TTL = 600
        const val INSTANT_DELETE_TTL = 5

        @JvmStatic
        fun `should successfully process wallets and calculate correct TTLs`(): Stream<Arguments> =
            Stream.of(
                Arguments.of(WalletTestUtils.cardWalletDocument("DELETED"), DELETED_WALLET_TTL),
                Arguments.of(WalletTestUtils.cardWalletDocument("REPLACED"), DELETED_WALLET_TTL),
                Arguments.of(
                    WalletTestUtils.cardWalletDocument("CREATED")
                        .copy(validationOperationResult = "EXECUTED"),
                    DELETED_WALLET_TTL
                ),
                Arguments.of(WalletTestUtils.cardWalletDocument("ERROR"), ERROR_WALLET_TTL),
                Arguments.of(
                    WalletTestUtils.cardWalletDocument("ERROR")
                        .copy(updateDate = Instant.now().minusSeconds(100000)),
                    INSTANT_DELETE_TTL
                ),
            )
    }

    private val walletRepository: WalletRepository = mock()
    private val walletBulkRepository: WalletBulkRepository = mock()
    private val ttlConfig: LifecycleManagementTtlConfig =
        LifecycleManagementTtlConfig(ERROR_WALLET_TTL, DELETED_WALLET_TTL, INSTANT_DELETE_TTL)
    private val queryConfig: LifecycleManagementQueryConfig =
        LifecycleManagementQueryConfig(
            listOf(WalletTestUtils.WALLET_VALIDATED_STATUS),
            QuerySettings(1, 10, 60, LocalTime.of(10, 0), LocalTime.of(12, 0))
        )
    private val lifecycleManagementService: LifecycleManagementService =
        LifecycleManagementService(walletRepository, walletBulkRepository, ttlConfig, queryConfig)

    @ParameterizedTest
    @MethodSource
    fun `should successfully process wallets and calculate correct TTLs`(
        wallet: Wallet,
        expectedTtl: Int
    ) {
        // Arrange
        val endDate = Instant.now()

        whenever(
                walletRepository.findByTtlNullAndStatusNotInAndUpdateDateBefore(
                    eq(queryConfig.excludedStatuses),
                    eq(endDate.toString()),
                    any()
                )
            )
            .thenReturn(Flux.just(wallet))

        whenever(walletBulkRepository.bulkUpdateTtl(any())).thenReturn(Mono.just(1))

        // Act & Assert
        StepVerifier.create(lifecycleManagementService.setWalletsTtl(endDate))
            .expectNext(1)
            .verifyComplete()

        argumentCaptor<Map<String, Int>>().apply {
            verify(walletBulkRepository).bulkUpdateTtl(capture())
            val mappedTtl = firstValue

            assertEquals(1, mappedTtl.size)
            val actualTtl = mappedTtl.values.iterator().next()

            assertTrue(
                actualTtl in (expectedTtl - 60)..expectedTtl,
                "Wallet TTL $actualTtl was not in expected range near $expectedTtl"
            )
        }
    }

    @Test
    fun `should throw NoWalletFoundException when repository returns empty`() {
        // Arrange
        val endDate = Instant.now()

        whenever(
                walletRepository.findByTtlNullAndStatusNotInAndUpdateDateBefore(
                    eq(queryConfig.excludedStatuses),
                    eq(endDate.toString()),
                    any()
                )
            )
            .thenReturn(Flux.empty())

        // Act & Assert
        StepVerifier.create(lifecycleManagementService.setWalletsTtl(endDate))
            .expectError(NoWalletFoundException::class.java)
            .verify()

        verify(walletBulkRepository, never()).bulkUpdateTtl(any())
    }

    @Test
    fun `should propagate error if repository search fails`() {
        // Arrange
        val endDate = Instant.now()
        val dbError = RuntimeException("Database timeout")

        whenever(
                walletRepository.findByTtlNullAndStatusNotInAndUpdateDateBefore(
                    eq(queryConfig.excludedStatuses),
                    eq(endDate.toString()),
                    any()
                )
            )
            .thenReturn(Flux.error(dbError))

        // Act & Assert
        StepVerifier.create(lifecycleManagementService.setWalletsTtl(endDate))
            .expectErrorMatches { it == dbError }
            .verify()

        verify(walletBulkRepository, never()).bulkUpdateTtl(any())
    }
}
