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
import java.time.temporal.ChronoUnit
import java.util.stream.Stream
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
        // Configuration values
        const val LONG_TERM_RETENTION_YEARS = 10
        const val SHORT_TERM_RETENTION_DAYS = 90
        const val INSTANT_DELETE_TTL_SECONDS = 5

        // Expected value
        val EXPECTED_LONG_TERM_TTL = (LONG_TERM_RETENTION_YEARS * 365.25 * 24 * 3600).toInt()
        val EXPECTED_SHORT_TERM_TTL = (SHORT_TERM_RETENTION_DAYS * 24 * 3600).toInt()

        @JvmStatic
        fun `should successfully process wallets and calculate correct TTLs`(): Stream<Arguments> =
            Stream.of(
                // Case: DELETED/REPLACED/EXECUTED -> 10 years
                Arguments.of(WalletTestUtils.cardWalletDocument("DELETED"), EXPECTED_LONG_TERM_TTL),
                Arguments.of(
                    WalletTestUtils.cardWalletDocument("REPLACED"),
                    EXPECTED_LONG_TERM_TTL
                ),
                Arguments.of(
                    WalletTestUtils.cardWalletDocument("CREATED")
                        .copy(validationOperationResult = "EXECUTED"),
                    EXPECTED_LONG_TERM_TTL
                ),
                // Case: ERROR -> 90 days
                Arguments.of(WalletTestUtils.cardWalletDocument("ERROR"), EXPECTED_SHORT_TERM_TTL),
                // Case: Already expired -> Instant Delete
                Arguments.of(
                    WalletTestUtils.cardWalletDocument("ERROR")
                        .copy(updateDate = Instant.now().minus(36500, ChronoUnit.DAYS)),
                    INSTANT_DELETE_TTL_SECONDS
                ),
            )
    }

    private val walletRepository: WalletRepository = mock()
    private val walletBulkRepository: WalletBulkRepository = mock()

    // Inizializziamo la config con i valori usati nel calcolo delle costanti sopra
    private val ttlConfig: LifecycleManagementTtlConfig =
        LifecycleManagementTtlConfig(
            SHORT_TERM_RETENTION_DAYS,
            LONG_TERM_RETENTION_YEARS,
            INSTANT_DELETE_TTL_SECONDS
        )

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
                    any(),
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
            val actualTtl = firstValue.values.first()

            // Calculate the difference beetween what we have and what we expect
            val diff = Math.abs(actualTtl - expectedTtl)

            // Let define a tollerance of 3 days for cover leap year
            val gapDaysInSeconds = 3 * 24 * 3600

            assertTrue(
                diff <= gapDaysInSeconds,
                "The TTL calculated ($actualTtl) is to distant from the expected ($expectedTtl). Diff: $diff seconds "
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
