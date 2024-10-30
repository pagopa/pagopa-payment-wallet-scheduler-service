package it.pagopa.wallet.scheduler.service

import com.azure.core.http.rest.Response
import com.azure.storage.queue.models.SendMessageResult
import it.pagopa.wallet.client.WalletQueueClient
import it.pagopa.wallet.scheduler.common.cdc.*
import it.pagopa.wallet.scheduler.common.tracing.TracedMono
import it.pagopa.wallet.scheduler.config.properties.CdcQueueConfig
import it.pagopa.wallet.scheduler.config.properties.RetrySendPolicyConfig
import it.pagopa.wallet.scheduler.util.TracingUtilsTest
import java.time.Duration
import java.util.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import reactor.test.StepVerifier

class CdcEventDispatcherServiceTest {

    private val config = CdcQueueConfig("", "", 100, 100)
    private val retrySendPolicyConfig: RetrySendPolicyConfig = RetrySendPolicyConfig(1, 100)
    private val walletQueueClient: WalletQueueClient = mock()
    private val tracingUtils = TracingUtilsTest.getMock()
    private val loggingEventDispatcherService =
        CdcEventDispatcherService(
            walletQueueClient,
            tracingUtils,
            config,
            retrySendPolicyConfig
        )

    @Test
    fun `should dispatch WalletOnboardCompletedEvent event`() {
        val eventId = UUID.randomUUID().toString()
        val onboardCompletedEvent = getOnboardCompletedEvent(eventId)

        given { walletQueueClient.sendWalletEvent(any(), any(), any()) }
            .willAnswer { Mono.just(mock() as Response<SendMessageResult>) }

        loggingEventDispatcherService
            .dispatchEvent(onboardCompletedEvent)
            .test()
            .assertNext { Assertions.assertEquals(onboardCompletedEvent, it) }
            .verifyComplete()

        argumentCaptor<LoggingEvent> {
            verify(walletQueueClient, times(1))
                .sendWalletEvent(
                    capture(),
                    eq(Duration.ofSeconds(config.visibilityTimeoutWalletCdc)),
                    any()
                )
            Assertions.assertEquals(
                onboardCompletedEvent.id,
                lastValue.id
            )
            verify(tracingUtils, times(1)).traceMonoQueue(any(), any<TracedMono<Any>>())
        }
    }

    @Test
    fun `should dispatch WalletOnboardCompletedEvent event on second retry`() {
        val eventId = UUID.randomUUID().toString()
        val onboardCompletedEvent = getOnboardCompletedEvent(eventId)

        given { walletQueueClient.sendWalletEvent(any(), any(), any()) }
            .willAnswer {
                Mono.error<Response<SendMessageResult>>(RuntimeException("First attempt failed"))
            }
            .willAnswer { Mono.just(mock() as Response<SendMessageResult>) }

        StepVerifier.create(loggingEventDispatcherService.dispatchEvent(onboardCompletedEvent))
            .expectSubscription()
            .expectNextCount(1)
            .verifyComplete()
    }

    private fun getOnboardCompletedEvent(eventId: String): WalletOnboardCompletedEvent{
        return WalletOnboardCompletedEvent(
                id = eventId,
                timestamp = "2024-10-16T15:03:36.527818530Z",
                walletId = "a527e843-9d1c-4531-ae5b-3809cc7abe7a",
                auditWallet =
                AuditWallet(
                    paymentMethodId =
                    "9d735400-9450-4f7e-9431-8c1e7fa2a339",
                    creationDate = "2024-10-16T15:03:18.541220633Z",
                    updateDate = "2024-10-16T15:03:36.447051359Z",
                    applications =
                    listOf(
                        AuditWalletApplication(
                            "PAGOPA",
                            "ENABLED",
                            "2024-10-16T15:03:18.378746985Z",
                            "2024-10-16T15:03:18.378747385Z",
                            emptyMap()
                        )
                    ),
                    details = AuditWalletDetails("CARDS", "VISA", null),
                    status = "VALIDATED",
                    validationOperationId = "618534471407042909",
                    validationOperationResult = "EXECUTED",
                    validationOperationTimestamp =
                    "2024-10-16T15:03:35.841Z",
                    validationErrorCode = null
                )
            )

    }
}
