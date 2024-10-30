package it.pagopa.wallet.scheduler.services

import it.pagopa.wallet.scheduler.WalletTestUtils
import it.pagopa.wallet.scheduler.config.WalletSearchConfig
import it.pagopa.wallet.scheduler.exceptions.WalletInvalidRangeException
import it.pagopa.wallet.scheduler.repositories.WalletRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.domain.Limit
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class WalletServiceTest {

    private val walletRepository: WalletRepository = mock()
    private val walletSearchConfig: WalletSearchConfig =
        WalletSearchConfig(WalletTestUtils.WALLET_CREATED_STATUS, 10)
    private val walletService: WalletService = WalletService(walletRepository, walletSearchConfig)

    @Test
    fun `Should return wallets by valid date range`() {
        val startDate: Instant = Instant.now().minus(10, ChronoUnit.DAYS)
        val endDate: Instant = Instant.now()

        given {
                walletRepository.findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
            .willReturn(
                Flux.just(
                    WalletTestUtils.walletDocument(walletSearchConfig.status),
                    WalletTestUtils.walletDocument(walletSearchConfig.status)
                )
            )

        StepVerifier.create(walletService.getWalletsForCdcIngestion(startDate, endDate))
            .assertNext { list ->
                assertEquals(list.size, 2)
                assertEquals(list.get(0), WalletTestUtils.walletDocument(walletSearchConfig.status))
            }
            .verifyComplete()

        verify(walletRepository, times(1))
            .findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(
                eq(startDate.toString()),
                eq(endDate.toString()),
                eq(walletSearchConfig.status),
                eq(Limit.of(walletSearchConfig.limit))
            )
    }

    @Test
    fun `Should return empty wallet list without error`() {
        val startDate: Instant = Instant.now().minus(10, ChronoUnit.DAYS)
        val endDate: Instant = Instant.now()

        given {
                walletRepository.findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(
                    any(),
                    any(),
                    any(),
                    any()
                )
            }
            .willReturn(Flux.empty())

        StepVerifier.create(walletService.getWalletsForCdcIngestion(startDate, endDate))
            .assertNext { list -> assertEquals(list.size, 0) }
            .verifyComplete()

        verify(walletRepository, times(1))
            .findByCreationDateBetweenAndStatusOrderByUpdateDateAsc(
                eq(startDate.toString()),
                eq(endDate.toString()),
                eq(walletSearchConfig.status),
                eq(Limit.of(walletSearchConfig.limit))
            )
    }

    @Test
    fun `Should throw exception if receives invalid date range`() {
        val startDate: Instant = Instant.now()
        val endDate: Instant = Instant.now().minus(10, ChronoUnit.DAYS)

        StepVerifier.create(walletService.getWalletsForCdcIngestion(startDate, endDate))
            .expectError(WalletInvalidRangeException::class.java)
            .verify()
    }
}
