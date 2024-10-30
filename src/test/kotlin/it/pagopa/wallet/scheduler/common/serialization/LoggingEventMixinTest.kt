package it.pagopa.wallet.scheduler.common.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.wallet.scheduler.common.cdc.*
import it.pagopa.wallet.scheduler.common.queue.CdcQueueEvent
import it.pagopa.wallet.scheduler.common.queue.TracingInfo
import it.pagopa.wallet.scheduler.config.AzureStorageConfiguration
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource

class LoggingEventMixinTest {
    companion object {
        private val mockedTracingInfo =
            TracingInfo(baggage = "baggage", tracestate = "tracestate", traceparent = "traceparent")

        @JvmStatic
        fun roundTripEventMethodSource(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "WalletDeletedEvent.json",
                    CdcQueueEvent(
                        tracingInfo = mockedTracingInfo,
                        data =
                            WalletDeletedEvent(
                                id = "bcfb7296-c53f-4840-9977-84a597fca1a0",
                                timestamp = "2024-06-12T15:50:47.231210Z",
                                walletId = "a21e0037-251d-413b-b121-8899e368df7e"
                            )
                    )
                ),
                Arguments.of(
                    "WalletOnboardCompletedEventPayPal.json",
                    CdcQueueEvent(
                        tracingInfo = TracingInfo(),
                        data =
                            WalletOnboardCompletedEvent(
                                id = "d283cbc5-cc48-4bc4-8f00-ddba4e24fc91",
                                timestamp = "2024-10-16T15:03:36.527818530Z",
                                walletId = "a527e843-9d1c-4531-ae5b-3809cc7abe7a",
                                auditWallet =
                                    AuditWallet(
                                        paymentMethodId = "9d735400-9450-4f7e-9431-8c1e7fa2a339",
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
                                        details = AuditWalletDetails("PAYPAL", null, "BCITITMM"),
                                        status = "VALIDATED",
                                        validationOperationId = "618534471407042909",
                                        validationOperationResult = "EXECUTED",
                                        validationOperationTimestamp = "2024-10-16T15:03:35.841Z",
                                        validationErrorCode = null
                                    )
                            )
                    )
                ),
                Arguments.of(
                    "WalletOnboardCompletedEventCards.json",
                    CdcQueueEvent(
                        tracingInfo = TracingInfo(),
                        data =
                            WalletOnboardCompletedEvent(
                                id = "d283cbc5-cc48-4bc4-8f00-ddba4e24fc91",
                                timestamp = "2024-10-16T15:03:36.527818530Z",
                                walletId = "a527e843-9d1c-4531-ae5b-3809cc7abe7a",
                                auditWallet =
                                    AuditWallet(
                                        paymentMethodId = "9d735400-9450-4f7e-9431-8c1e7fa2a339",
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
                                        validationOperationTimestamp = "2024-10-16T15:03:35.841Z",
                                        validationErrorCode = null
                                    )
                            )
                    )
                )
            )
    }
    private val serializationConfiguration = AzureStorageConfiguration()
    private val objectMapperBuilder = serializationConfiguration.cdcObjectMapperBuilder()
    private val objectMapper: ObjectMapper =
        serializationConfiguration.cdcObjectMapper(objectMapperBuilder)
    private val azureJsonSerializer =
        serializationConfiguration.jsonSerializerProvider(objectMapper).createInstance()

    @ParameterizedTest
    @MethodSource("roundTripEventMethodSource")
    fun `Can round trip events successfully`(
        filename: String,
        expectedDeserializedEvent: CdcQueueEvent<LoggingEvent>
    ) {
        val serializedString = serializeEvent(expectedDeserializedEvent)
        val expectedSerializedString = getStringFromJsonFile(filename)

        assertEquals(expectedSerializedString, serializedString)
    }

    private fun <T> serializeEvent(event: T): String {
        val baos = ByteArrayOutputStream()
        azureJsonSerializer.serialize(baos, event)
        return baos.toString(StandardCharsets.UTF_8)
    }

    private fun getStringFromJsonFile(fileName: String): String {
        val resource: Resource = ClassPathResource(fileName)
        val file: File = resource.getFile()
        val bufferedReader = BufferedReader(FileReader(file))

        return bufferedReader.lines().collect(Collectors.joining())
    }
}
