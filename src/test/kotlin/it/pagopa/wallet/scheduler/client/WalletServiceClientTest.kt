package it.pagopa.wallet.scheduler.client

import it.pagopa.generated.walletservice.api.WalletsApi
import it.pagopa.wallet.scheduler.exceptions.WalletServiceException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class WalletServiceClientTest {

    private val walletServiceWebClient: WalletsApi = mock()
    private val walletServiceClient = WalletServiceClient(walletServiceWebClient)

    @Test
    fun `Should deactivate contract for wallet`() {
        val walletId = UUID.randomUUID()
        whenever(walletServiceWebClient.deactivateContractByWalletId(eq(walletId)))
            .thenReturn(Mono.empty())

        StepVerifier.create(walletServiceClient.deactivateContract(walletId.toString()))
            .verifyComplete()

        verify(walletServiceWebClient).deactivateContractByWalletId(eq(walletId))
    }

    @Test
    fun `Should wrap wallet service errors`() {
        val walletId = UUID.randomUUID()
        val cause = IllegalStateException("Wallet service unavailable")
        whenever(walletServiceWebClient.deactivateContractByWalletId(eq(walletId)))
            .thenReturn(Mono.error(cause))

        StepVerifier.create(walletServiceClient.deactivateContract(walletId.toString()))
            .expectErrorSatisfies { error ->
                val walletServiceException = assertIs<WalletServiceException>(error)
                assertEquals("Wallet service exception for walletId [$walletId]", error.message)
                assertSame(cause, walletServiceException.cause)
            }
            .verify()
    }
}
