package it.pagopa.wallet.scheduler.config.properties

import it.pagopa.wallet.scheduler.utils.TimeBasedRate
import java.time.Duration
import java.time.LocalTime
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("lifecycle-management-job.query")
data class LifecycleManagementQueryConfig(
    val settings: QuerySettings,
    val contractDeactivationMaxRetries: Int,
    val contractDeactivationLowRateDelayMs: Long,
    val contractDeactivationHighRateDelayMs: Long,
    val ttlSeconds: Int
) {
    val lifeCycleManagementTimeBasedRate = TimeBasedRate.fromQuerySettings(settings)
    private val lowRateDelay = Duration.ofMillis(contractDeactivationLowRateDelayMs)
    private val highRateDelay = Duration.ofMillis(contractDeactivationHighRateDelayMs)

    init {
        require(contractDeactivationMaxRetries > 0) {
            "The contract deactivation retry limit must be greater than zero"
        }
        require(ttlSeconds > 0) { "TTL seconds must be greater than zero" }
        require(contractDeactivationHighRateDelayMs > 0) {
            "The high-rate contract deactivation delay must be positive"
        }
        require(contractDeactivationLowRateDelayMs >= contractDeactivationHighRateDelayMs) {
            "The low-rate contract deactivation delay must be at least the high-rate delay"
        }
    }

    fun contractDeactivationInvocationDelay(at: LocalTime): Duration {
        val elapsed =
            lifeCycleManagementTimeBasedRate.elapsedInBurstWindow(at) ?: return lowRateDelay
        val rampUp = lifeCycleManagementTimeBasedRate.rampUp
        if (elapsed >= rampUp) return highRateDelay

        val delayReduction = lowRateDelay.minus(highRateDelay)
        return lowRateDelay.minus(
            delayReduction.multipliedBy(elapsed.seconds).dividedBy(rampUp.seconds)
        )
    }
}

data class QuerySettings(
    val lowRate: Int,
    val highRate: Int,
    val rampUpDurationSeconds: Int,
    val burstStartWindow: LocalTime,
    val burstEndWindow: LocalTime
)
