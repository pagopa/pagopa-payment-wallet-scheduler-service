package it.pagopa.wallet.scheduler.config

import com.azure.core.http.netty.NettyAsyncHttpClientBuilder
import com.azure.core.serializer.json.jackson.JacksonJsonSerializerBuilder
import com.azure.core.util.serializer.JsonSerializerProvider
import com.azure.storage.queue.QueueClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import it.pagopa.wallet.scheduler.client.WalletQueueClient
import it.pagopa.wallet.scheduler.common.cdc.LoggingEvent
import it.pagopa.wallet.scheduler.common.serialization.LoggingEventMixin
import it.pagopa.wallet.scheduler.config.properties.CdcQueueConfig
import java.time.Duration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import reactor.netty.http.client.HttpClient

@Configuration
class AzureStorageConfiguration {

    @Bean
    fun cdcObjectMapperBuilder(): Jackson2ObjectMapperBuilder =
        Jackson2ObjectMapperBuilder()
            .modules(Jdk8Module(), JavaTimeModule(), kotlinModule())
            .mixIn(LoggingEvent::class.java, LoggingEventMixin::class.java)

    @Bean
    fun cdcObjectMapper(cdcObjectMapperBuilder: Jackson2ObjectMapperBuilder): ObjectMapper =
        cdcObjectMapperBuilder.build()

    @Bean
    fun jsonSerializerProvider(objectMapper: ObjectMapper): JsonSerializerProvider =
        JsonSerializerProvider {
            JacksonJsonSerializerBuilder().serializer(objectMapper).build()
        }

    @Bean
    fun cdcQueueClient(
        cdcQueueConfig: CdcQueueConfig,
        jsonSerializerProvider: JsonSerializerProvider,
    ): WalletQueueClient {
        val serializer = jsonSerializerProvider.createInstance()
        val queue =
            QueueClientBuilder()
                .connectionString(cdcQueueConfig.storageConnectionString)
                .queueName(cdcQueueConfig.storageQueueName)
                .httpClient(
                    NettyAsyncHttpClientBuilder(
                            HttpClient.create().resolver { nameResolverSpec ->
                                nameResolverSpec.ndots(1)
                            }
                        )
                        .build()
                )
                .buildAsyncClient()
        return WalletQueueClient(queue, serializer, Duration.ofSeconds(cdcQueueConfig.ttlSeconds))
    }
}
