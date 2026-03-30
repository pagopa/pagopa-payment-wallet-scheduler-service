package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("lifecycle-management-job.execution")
class LifecycleManagementConfiguration(
    val excludedPeriodDays: Long,
)
