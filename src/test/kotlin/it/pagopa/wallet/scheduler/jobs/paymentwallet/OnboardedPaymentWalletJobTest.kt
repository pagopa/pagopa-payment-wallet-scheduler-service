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
import java.util.*
import kotlin.test.assertEquals
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class OnboardedPaymentWalletJobTest {

    private val walletService: WalletService = mock()

    private val cdcEventDispatcherService: CdcEventDispatcherService = mock()

    private val redisResumePolicyService: RedisResumePolicyService = mock()

    private val onboardedPaymentWalletJob =
        OnboardedPaymentWalletJob(
            walletService = walletService,
            cdcEventDispatcherService = cdcEventDispatcherService,
            redisResumePolicyService = redisResumePolicyService
        )

    @Test
    fun `should process wallet successfully in given date range for paypal wallets`() {
        // pre-requisites
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf =
            OnboardedPaymentWalletJobConfiguration(startDate = startDate, endDate = endDate)
        val wallet = WalletTestUtils.paypalWalletDocument("VALIDATED")
        val foundWallets = listOf(wallet)
        val expectedLoggedEvent =
            wallet.let {
                WalletOnboardCompletedEvent(
                    id = it.id,
                    timestamp = it.creationDate.toString(),
                    walletId = it.id,
                    auditWallet =
                        AuditWallet(
                            paymentMethodId = it.paymentMethodId,
                            creationDate = it.creationDate.toString(),
                            updateDate = it.updateDate.toString(),
                            applications =
                                it.applications.map { application ->
                                    AuditWalletApplication(
                                        id = application.id,
                                        creationDate = application.creationDate,
                                        updateDate = application.updateDate,
                                        metadata = application.metadata,
                                        status = application.status
                                    )
                                },
                            details =
                                it.details?.let { walletDetails ->
                                    AuditWalletDetails(
                                        type = "PAYPAL",
                                        pspId = (walletDetails as PayPalDetails).pspId,
                                        cardBrand = null
                                    )
                                },
                            status = it.status,
                            validationOperationId = null,
                            validationOperationResult = it.validationOperationResult,
                            validationOperationTimestamp = it.creationDate.toString(),
                            validationErrorCode = it.validationErrorCode,
                        )
                )
            }
        given(walletService.getWalletsForCdcIngestion(any(), any()))
            .willReturn(Flux.fromIterable(foundWallets))
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
                    org.mockito.kotlin.argThat {
                        assertEquals(expectedLoggedEvent, this)
                        true
                    }
            )
    }

    @Test
    fun `should process wallet successfully in given date range for cards wallets`() {
        // pre-requisites
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf =
            OnboardedPaymentWalletJobConfiguration(startDate = startDate, endDate = endDate)
        val wallet = WalletTestUtils.cardWalletDocument("VALIDATED")
        val foundWallets = listOf(wallet)
        val expectedLoggedEvent =
            wallet.let {
                WalletOnboardCompletedEvent(
                    id = it.id,
                    timestamp = it.creationDate.toString(),
                    walletId = it.id,
                    auditWallet =
                        AuditWallet(
                            paymentMethodId = it.paymentMethodId,
                            creationDate = it.creationDate.toString(),
                            updateDate = it.updateDate.toString(),
                            applications =
                                it.applications.map { application ->
                                    AuditWalletApplication(
                                        id = application.id,
                                        creationDate = application.creationDate,
                                        updateDate = application.updateDate,
                                        metadata = application.metadata,
                                        status = application.status
                                    )
                                },
                            details =
                                it.details?.let { walletDetails ->
                                    AuditWalletDetails(
                                        type = "CARDS",
                                        cardBrand = (walletDetails as CardDetails).brand,
                                        pspId = null
                                    )
                                },
                            status = it.status,
                            validationOperationId = null,
                            validationOperationResult = it.validationOperationResult,
                            validationOperationTimestamp = it.creationDate.toString(),
                            validationErrorCode = it.validationErrorCode,
                        )
                )
            }
        given(walletService.getWalletsForCdcIngestion(any(), any()))
            .willReturn(Flux.fromIterable(foundWallets))
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
                    org.mockito.kotlin.argThat {
                        assertEquals(expectedLoggedEvent, this)
                        true
                    }
            )
    }

    @Test
    fun `should return error for no wallet found in a time window`() {
        // pre-requisites
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf =
            OnboardedPaymentWalletJobConfiguration(startDate = startDate, endDate = endDate)

        given(walletService.getWalletsForCdcIngestion(any(), any())).willReturn(Flux.empty())

        // Test
        StepVerifier.create(onboardedPaymentWalletJob.process(configuration = jobConf))
            .expectError(NoWalletFoundException::class.java)
            .verify()
        verify(walletService, times(1))
            .getWalletsForCdcIngestion(startDate = startDate, endDate = endDate)
        verify(cdcEventDispatcherService, times(0)).dispatchEvent(event = any())
    }

    @Test
    fun `should perform checkpoint successfully`() {
        // pre-requisites
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf =
            OnboardedPaymentWalletJobConfiguration(startDate = startDate, endDate = endDate)
        val wallet = WalletTestUtils.cardWalletDocument("VALIDATED")
        val foundWallets = listOf(wallet)
        given(walletService.getWalletsForCdcIngestion(any(), any()))
            .willReturn(Flux.fromIterable(foundWallets))
        given(cdcEventDispatcherService.dispatchEvent(any())).willAnswer {
            mono { it.arguments[0] }
        }
        doNothing()
            .`when`(redisResumePolicyService)
            .saveResumeTimestamp(eq(onboardedPaymentWalletJob.id()), eq(wallet.creationDate))
        // Test
        StepVerifier.create(onboardedPaymentWalletJob.process(configuration = jobConf))
            .expectNext(foundWallets.last().creationDate.toString())
            .verifyComplete()
        verify(redisResumePolicyService, times(1))
            .saveResumeTimestamp(eq(onboardedPaymentWalletJob.id()), eq(wallet.creationDate))
    }

    @Test
    fun `should start from checkpoint successfully`() {
        // pre-requisites
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf =
            OnboardedPaymentWalletJobConfiguration(startDate = startDate, endDate = endDate)
        val wallet = WalletTestUtils.cardWalletDocument("VALIDATED")
        val foundWallets = listOf(wallet)
        val checkpoint = wallet.creationDate + Duration.ofSeconds(1)
        given(walletService.getWalletsForCdcIngestion(any(), any()))
            .willReturn(Flux.fromIterable(foundWallets))
        given(cdcEventDispatcherService.dispatchEvent(any())).willAnswer {
            mono { it.arguments[0] }
        }
        given(redisResumePolicyService.getResumeTimestamp(any())).willAnswer {
            Optional.of(checkpoint)
        }

        doNothing()
            .`when`(redisResumePolicyService)
            .saveResumeTimestamp(eq(onboardedPaymentWalletJob.id()), eq(wallet.creationDate))
        // Test
        StepVerifier.create(onboardedPaymentWalletJob.process(configuration = jobConf))
            .expectNext(foundWallets.last().creationDate.toString())
            .verifyComplete()
        verify(redisResumePolicyService, times(1))
            .getResumeTimestamp(eq(onboardedPaymentWalletJob.id()))
        verify(walletService, times(1)).getWalletsForCdcIngestion(eq(checkpoint), eq(endDate))
    }

    @Test
    fun `should perform checkpoint using last element based on timestamp`() {
        // pre-requisites
        val startDate = Instant.now()
        val endDate = startDate + Duration.ofSeconds(10)
        val jobConf =
            OnboardedPaymentWalletJobConfiguration(startDate = startDate, endDate = endDate)
        val wallet = WalletTestUtils.cardWalletDocument("VALIDATED")
        val wallet2 = WalletTestUtils.cardWalletDocument("VALIDATED").copy(creationDate = endDate)
        val foundWallets = listOf(wallet2, wallet)
        given(walletService.getWalletsForCdcIngestion(any(), any()))
            .willReturn(Flux.fromIterable(foundWallets))
        given(cdcEventDispatcherService.dispatchEvent(any())).willAnswer {
            mono { it.arguments[0] }
        }
        doNothing()
            .`when`(redisResumePolicyService)
            .saveResumeTimestamp(eq(onboardedPaymentWalletJob.id()), eq(wallet.creationDate))
        // Test
        StepVerifier.create(onboardedPaymentWalletJob.process(configuration = jobConf))
            .expectNext(wallet2.creationDate.toString())
            .verifyComplete()
        verify(redisResumePolicyService, times(1))
            .saveResumeTimestamp(eq(onboardedPaymentWalletJob.id()), eq(wallet2.creationDate))
    }
}
