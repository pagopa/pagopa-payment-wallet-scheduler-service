package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("scheduler.cdc.queue")
data class CdcQueueConfig(
    val storageConnectionString: String,
    val storageQueueName: String,
    val ttlSeconds: Long,
    val visibilityTimeoutWalletCdc: Long
)
