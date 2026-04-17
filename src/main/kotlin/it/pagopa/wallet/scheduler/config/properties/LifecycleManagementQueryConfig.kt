package it.pagopa.wallet.scheduler.config.properties

import it.pagopa.wallet.scheduler.utils.TimeBasedRate
import java.time.LocalTime
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("lifecycle-management-job.query")
data class LifecycleManagementQueryConfig(val settings: QuerySettings) {
    val lifeCycleManagementTimeBasedRate = TimeBasedRate.fromQuerySettings(settings)
}

data class QuerySettings(
    val lowRate: Int,
    val highRate: Int,
    val rampUpDurationSeconds: Int,
    val burstStartWindow: LocalTime,
    val burstEndWindow: LocalTime
)
