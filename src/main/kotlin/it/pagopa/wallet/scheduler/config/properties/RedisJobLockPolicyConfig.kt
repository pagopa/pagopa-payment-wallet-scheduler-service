package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "scheduler.redis-job-lock")
data class RedisJobLockPolicyConfig(val keyspace: String)
