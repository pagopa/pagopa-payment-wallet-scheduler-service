package it.pagopa.wallet.scheduler.client

import com.azure.core.http.rest.Response
import com.azure.core.util.BinaryData
import com.azure.core.util.serializer.JsonSerializer
import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.models.SendMessageResult
import it.pagopa.wallet.scheduler.common.cdc.LoggingEvent
import it.pagopa.wallet.scheduler.common.queue.CdcQueueEvent
import it.pagopa.wallet.scheduler.common.queue.TracingInfo
import java.time.Duration
import reactor.core.publisher.Mono

class WalletQueueClient(
    private val cdcQueueClient: QueueAsyncClient,
    private val jsonSerializer: JsonSerializer,
    private val ttl: Duration
) {
    fun sendWalletEvent(
        event: LoggingEvent,
        delay: Duration,
        tracingInfo: TracingInfo,
    ): Mono<Response<SendMessageResult>> {
        val queueEvent = CdcQueueEvent(event, tracingInfo)
        return BinaryData.fromObjectAsync(queueEvent, jsonSerializer).flatMap {
            cdcQueueClient.sendMessageWithResponse(it, delay, ttl)
        }
    }
}
