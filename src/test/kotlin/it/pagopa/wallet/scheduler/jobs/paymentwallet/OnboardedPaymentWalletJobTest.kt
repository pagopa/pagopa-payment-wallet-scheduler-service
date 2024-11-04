package it.pagopa.wallet.scheduler.jobs.paymentwallet

import it.pagopa.wallet.scheduler.WalletTestUtils
import it.pagopa.wallet.scheduler.jobs.config.OnboardedPaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.services.CdcEventDispatcherService
import it.pagopa.wallet.scheduler.services.WalletService
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import reactor.test.StepVerifier

class OnboardedPaymentWalletJobTest {

    private val walletService: WalletService = mock()

    private val cdcEventDispatcherService: CdcEventDispatcherService = mock()

    private val onboardedPaymentWalletJob =
        OnboardedPaymentWalletJob(
            walletService = walletService,
            cdcEventDispatcherService = cdcEventDispatcherService
        )

    @Test
    fun `should process wallet successfully in given date range`() {
        // pre-requisites
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf =
            OnboardedPaymentWalletJobConfiguration(startDate = startDate, endDate = endDate)
        val foundWallets = listOf(WalletTestUtils.paypalWalletDocument("VALIDATED"))
        given(walletService.getWalletsForCdcIngestion(any(), any()))
            .willReturn(mono { foundWallets })
        given(cdcEventDispatcherService.dispatchEvent(any())).willAnswer {
            mono { it.arguments[0] }
        }

        // Test
        StepVerifier.create(onboardedPaymentWalletJob.process(configuration = jobConf))
            .expectNext(foundWallets.last().creationDate.toString())
            .verifyComplete()
        verify(walletService, times(1))
            .getWalletsForCdcIngestion(startDate = startDate, endDate = endDate)
        verify(cdcEventDispatcherService, times(1))
            .dispatchEvent(
                event =
                    argThat {
                        assertEquals("", it.id)
                        true
                    }
            )
    }
}
