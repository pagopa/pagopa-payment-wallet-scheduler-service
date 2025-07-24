package it.pagopa.wallet.scheduler.jobs.paymentwallet

import it.pagopa.wallet.documents.wallets.details.CardDetails
import it.pagopa.wallet.documents.wallets.details.PayPalDetails
import it.pagopa.wallet.scheduler.WalletTestUtils
import it.pagopa.wallet.scheduler.common.cdc.AuditWallet
import it.pagopa.wallet.scheduler.common.cdc.AuditWalletApplication
import it.pagopa.wallet.scheduler.common.cdc.AuditWalletDetails
import it.pagopa.wallet.scheduler.common.cdc.WalletOnboardCompletedEvent
import it.pagopa.wallet.scheduler.exceptions.NoWalletFoundException
import it.pagopa.wallet.scheduler.jobs.config.OnboardedPaymentWalletJobConfiguration
import it.pagopa.wallet.scheduler.services.CdcEventDispatcherService
import it.pagopa.wallet.scheduler.services.RedisResumePolicyService
import it.pagopa.wallet.scheduler.services.WalletService
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class OnboardedPaymentWalletJobTest {

    private val walletService: WalletService = mock()
    private val cdcEventDispatcherService: CdcEventDispatcherService = mock()
    private val redisResumePolicyService: RedisResumePolicyService = mock()

    private val onboardedPaymentWalletJob =
        OnboardedPaymentWalletJob(
            walletService,
            cdcEventDispatcherService,
            redisResumePolicyService
        )

    @Test
    fun `should process wallet successfully in given date range for paypal wallets`() {
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf = OnboardedPaymentWalletJobConfiguration(startDate, endDate)
        val wallet = WalletTestUtils.paypalWalletDocument("VALIDATED")
        val foundWallets = listOf(wallet)

        val expectedLoggedEvent =
            WalletOnboardCompletedEvent(
                id = wallet.id,
                timestamp = wallet.creationDate.toString(),
                walletId = wallet.id,
                auditWallet =
                    AuditWallet(
                        paymentMethodId = wallet.paymentMethodId,
                        creationDate = wallet.creationDate.toString(),
                        updateDate = wallet.updateDate.toString(),
                        applications =
                            wallet.applications.map {
                                AuditWalletApplication(
                                    id = it.id,
                                    creationDate = it.creationDate,
                                    updateDate = it.updateDate,
                                    metadata = it.metadata,
                                    status = it.status
                                )
                            },
                        details =
                            AuditWalletDetails(
                                type = "PAYPAL",
                                pspId = (wallet.details as PayPalDetails).pspId,
                                cardBrand = null
                            ),
                        status = wallet.status,
                        validationOperationId = null,
                        validationOperationResult = wallet.validationOperationResult,
                        validationOperationTimestamp = wallet.creationDate.toString(),
                        validationErrorCode = wallet.validationErrorCode
                    )
            )

        given(walletService.getWalletsForCdcIngestion(any(), any()))
            .willReturn(Flux.fromIterable(foundWallets))

        given(cdcEventDispatcherService.dispatchEvent(any())).willAnswer {
            mono { it.arguments[0] }
        }

        doNothing().`when`(redisResumePolicyService).saveResumeTimestamp(any(), any())
        println(onboardedPaymentWalletJob.process(jobConf).block())
        StepVerifier.create(onboardedPaymentWalletJob.process(jobConf))
            .expectNext(wallet.creationDate.toString())
            .verifyComplete()

        verify(walletService).getWalletsForCdcIngestion(startDate, endDate)
        verify(cdcEventDispatcherService)
            .dispatchEvent(
                argThat {
                    assertEquals(expectedLoggedEvent, this)
                    true
                }
            )
    }

    @Test
    fun `should process wallet successfully in given date range for cards wallets`() {
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf = OnboardedPaymentWalletJobConfiguration(startDate, endDate)
        val wallet = WalletTestUtils.cardWalletDocument("VALIDATED")
        val foundWallets = listOf(wallet)

        val expectedLoggedEvent =
            WalletOnboardCompletedEvent(
                id = wallet.id,
                timestamp = wallet.creationDate.toString(),
                walletId = wallet.id,
                auditWallet =
                    AuditWallet(
                        paymentMethodId = wallet.paymentMethodId,
                        creationDate = wallet.creationDate.toString(),
                        updateDate = wallet.updateDate.toString(),
                        applications =
                            wallet.applications.map {
                                AuditWalletApplication(
                                    id = it.id,
                                    creationDate = it.creationDate,
                                    updateDate = it.updateDate,
                                    metadata = it.metadata,
                                    status = it.status
                                )
                            },
                        details =
                            AuditWalletDetails(
                                type = "CARDS",
                                pspId = null,
                                cardBrand = (wallet.details as CardDetails).brand
                            ),
                        status = wallet.status,
                        validationOperationId = null,
                        validationOperationResult = wallet.validationOperationResult,
                        validationOperationTimestamp = wallet.creationDate.toString(),
                        validationErrorCode = wallet.validationErrorCode
                    )
            )

        given(walletService.getWalletsForCdcIngestion(any(), any()))
            .willReturn(Flux.fromIterable(foundWallets))

        given(cdcEventDispatcherService.dispatchEvent(any())).willAnswer {
            mono { it.arguments[0] }
        }

        doNothing().`when`(redisResumePolicyService).saveResumeTimestamp(any(), any())

        StepVerifier.create(onboardedPaymentWalletJob.process(jobConf))
            .expectNext(wallet.creationDate.toString())
            .verifyComplete()
    }

    @Test
    fun `should return error for no wallet found in a time window`() {
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf = OnboardedPaymentWalletJobConfiguration(startDate, endDate)

        given(walletService.getWalletsForCdcIngestion(any(), any())).willReturn(Flux.empty())

        StepVerifier.create(onboardedPaymentWalletJob.process(jobConf))
            .expectError(NoWalletFoundException::class.java)
            .verify()

        verify(walletService).getWalletsForCdcIngestion(startDate, endDate)
        verify(cdcEventDispatcherService, never()).dispatchEvent(any())
    }

    @Test
    fun `should perform checkpoint successfully`() {
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf = OnboardedPaymentWalletJobConfiguration(startDate, endDate)
        val wallet = WalletTestUtils.cardWalletDocument("VALIDATED")
        val foundWallets = listOf(wallet)

        given(walletService.getWalletsForCdcIngestion(any(), any()))
            .willReturn(Flux.fromIterable(foundWallets))

        given(cdcEventDispatcherService.dispatchEvent(any())).willAnswer {
            mono { it.arguments[0] }
        }

        doNothing().`when`(redisResumePolicyService).saveResumeTimestamp(any(), any())

        StepVerifier.create(onboardedPaymentWalletJob.process(jobConf))
            .expectNext(wallet.creationDate.toString())
            .verifyComplete()

        verify(redisResumePolicyService)
            .saveResumeTimestamp(onboardedPaymentWalletJob.id(), wallet.creationDate)
    }

    @Test
    fun `should start from checkpoint successfully`() {
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf = OnboardedPaymentWalletJobConfiguration(startDate, endDate)
        val wallet = WalletTestUtils.cardWalletDocument("VALIDATED")
        val checkpoint = wallet.creationDate + Duration.ofSeconds(1)
        val foundWallets = listOf(wallet)

        given(redisResumePolicyService.getResumeTimestamp(any())).willReturn(Mono.just(checkpoint))

        given(walletService.getWalletsForCdcIngestion(any(), any()))
            .willReturn(Flux.fromIterable(foundWallets))

        given(cdcEventDispatcherService.dispatchEvent(any())).willAnswer {
            mono { it.arguments[0] }
        }

        doNothing().`when`(redisResumePolicyService).saveResumeTimestamp(any(), any())

        StepVerifier.create(onboardedPaymentWalletJob.process(jobConf))
            .expectNext(wallet.creationDate.toString())
            .verifyComplete()

        verify(redisResumePolicyService).getResumeTimestamp(onboardedPaymentWalletJob.id())
        verify(walletService).getWalletsForCdcIngestion(checkpoint, endDate)
    }

    @Test
    fun `should perform checkpoint using last element based on timestamp`() {
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf = OnboardedPaymentWalletJobConfiguration(startDate, endDate)
        val wallet1 = WalletTestUtils.cardWalletDocument("VALIDATED").copy(creationDate = startDate)
        val wallet2 = WalletTestUtils.cardWalletDocument("VALIDATED").copy(creationDate = endDate)
        val foundWallets = listOf(wallet2, wallet1)

        given(walletService.getWalletsForCdcIngestion(any(), any()))
            .willReturn(Flux.fromIterable(foundWallets))

        given(cdcEventDispatcherService.dispatchEvent(any())).willAnswer {
            mono { it.arguments[0] }
        }

        doNothing().`when`(redisResumePolicyService).saveResumeTimestamp(any(), any())

        StepVerifier.create(onboardedPaymentWalletJob.process(jobConf))
            .expectNext(wallet2.creationDate.toString())
            .verifyComplete()

        verify(redisResumePolicyService)
            .saveResumeTimestamp(onboardedPaymentWalletJob.id(), wallet2.creationDate)
    }
}
