package it.pagopa.wallet.scheduler.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wallet.search")
data class WalletSearchConfig(val status: String, val limit: Int)
