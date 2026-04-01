package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("lifecycle-management-job.ttl")
class LifecycleManagementTtlConfig(
    val shortTermRetentionDays: Int,
    val longTermRetentionYears: Int,
    val instantDeleteTtlSeconds: Int
)
