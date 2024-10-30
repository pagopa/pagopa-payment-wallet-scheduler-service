package it.pagopa.wallet.scheduler.config

import com.azure.core.http.netty.NettyAsyncHttpClientBuilder
import com.azure.core.serializer.json.jackson.JacksonJsonSerializerBuilder
import com.azure.core.util.serializer.JsonSerializerProvider
import com.azure.storage.queue.QueueClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.wallet.client.WalletQueueClient
import it.pagopa.wallet.scheduler.config.properties.CdcQueueConfig
import java.time.Duration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.netty.http.client.HttpClient

@Configuration
class AzureStorageConfiguration {

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
