package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "scheduler.redis-job-lock")
data class RedisJobLockPolicyConfig(val keyspace: String, val ttlSec: Long, val waitTimeSec: Long) {

    fun getLockByTarget(target: String): String = "%s:%s:%s".format(keyspace, "lock", target)
}
