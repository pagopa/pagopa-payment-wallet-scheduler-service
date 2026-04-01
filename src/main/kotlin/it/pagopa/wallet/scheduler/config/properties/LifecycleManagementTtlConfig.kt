package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("lifecycle-management-job.ttl")
class LifecycleManagementTtlConfig(
    val shortTermRetentionSeconds: Int,
    val longTermRetentionSeconds: Int,
    val instantDeleteTtlSeconds: Int
)
