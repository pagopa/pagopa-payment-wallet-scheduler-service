package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "scheduler.redis-job-lock")
data class RedisJobLockPolicyConfig(val keyspace: String, val ttlSec: Long, val waitTimeMs: Long) {

    fun getLockNameByJob(jobName: String): String = "%s:%s:%s".format(keyspace, "lock", jobName)

    fun getSemNameByJob(jobName: String): String = "%s:%s:%s".format(keyspace, "sem", jobName)
}
