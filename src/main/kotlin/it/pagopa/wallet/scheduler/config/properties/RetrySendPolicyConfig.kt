package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "scheduler.cdc.retry-send")
data class RetrySendPolicyConfig(val maxAttempts: Long, val intervalInMs: Long) {}
