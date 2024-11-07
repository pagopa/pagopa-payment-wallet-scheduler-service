package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "scheduler.redis-resume")
data class RedisResumePolicyConfig(val keyspace: String, val ttlInMin: Long)
