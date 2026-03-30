package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("lifecycle.job.execution")
class LifecycleManagementConfiguration(
    val limit: Int,
    val excludedStatuses: List<String>,
    val excludedPeriodDays: Long,
    val errorWalletTtl: Int,
    val deletedWalletTtl: Int,
    val instantDeleteTtl: Int
)
