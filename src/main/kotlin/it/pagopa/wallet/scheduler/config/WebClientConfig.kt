package it.pagopa.wallet.scheduler.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import it.pagopa.wallet.scheduler.config.properties.WalletServiceConfigProperties
import java.util.concurrent.TimeUnit
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient

@Configuration
class WebClientConfig {
    @Bean(name = ["walletServiceWebClient"])
    fun walletServiceWebClient(
        config: WalletServiceConfigProperties
    ): it.pagopa.generated.walletservice.api.WalletsApi {
        val httpClient =
            HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.connectionTimeout)
                .doOnConnected { connection: Connection ->
                    connection.addHandlerLast(
                        ReadTimeoutHandler(config.readTimeout.toLong(), TimeUnit.MILLISECONDS)
                    )
                }
                .resolver { it.ndots(1) }
        val webClient =
            it.pagopa.generated.walletservice.ApiClient.buildWebClientBuilder()
                .clientConnector(ReactorClientHttpConnector(httpClient))
                .defaultHeader("x-api-key", config.apiKey)
                .baseUrl(config.uri)
                .build()
        val apiClient =
            it.pagopa.generated.walletservice.ApiClient(webClient).setBasePath(config.uri)

        return it.pagopa.generated.walletservice.api.WalletsApi(apiClient)
    }
}
