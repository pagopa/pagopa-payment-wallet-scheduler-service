package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wallet-service")
data class WalletServiceConfigProperties(
    val uri: String,
    val readTimeout: Int,
    val connectionTimeout: Int,
    val apiKey: String
)
