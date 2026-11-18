package it.pagopa.wallet.scheduler.config.properties

import it.pagopa.wallet.scheduler.utils.TimeBasedRate
import java.time.LocalTime
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("lifecycle-management-job.query")
data class LifecycleManagementQueryConfig(
    val settings: QuerySettings,
    val contractDeactivationMaxRetries: Int,
    val ttlSeconds: Int
) {
    val lifeCycleManagementTimeBasedRate = TimeBasedRate.fromQuerySettings(settings)

    init {
        require(ttlSeconds > 0) { "TTL seconds must be greater than zero" }
    }
}

data class QuerySettings(
    val lowRate: Int,
    val highRate: Int,
    val rampUpDurationSeconds: Int,
    val burstStartWindow: LocalTime,
    val burstEndWindow: LocalTime
)
