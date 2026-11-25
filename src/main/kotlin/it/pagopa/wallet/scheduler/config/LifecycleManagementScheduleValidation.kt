package it.pagopa.wallet.scheduler.config

import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementQueryConfig
import java.time.Duration
import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Component

@Component
class LifecycleManagementScheduleValidation(
    @Value("\${lifecycle-management-job.execution.cron}") cron: String,
    query: LifecycleManagementQueryConfig
) {
    init {
        if (cron != Scheduled.CRON_DISABLED) {
            val intervalMs = executionIntervalMs(cron)
            validateBatch(
                "lowRate",
                query.settings.lowRate,
                query.contractDeactivationLowRateDelayMs,
                intervalMs
            )
            validateBatch(
                "highRate",
                query.settings.highRate,
                query.contractDeactivationHighRateDelayMs,
                intervalMs
            )
        }
    }

    private fun validateBatch(setting: String, limit: Int, delayMs: Long, intervalMs: Long) {
        val capacity = intervalMs / delayMs
        require(limit <= capacity) {
            "Lifecycle $setting=$limit exceeds the maximum of $capacity wallets per execution (deactivation delay: $delayMs ms, cron interval: $intervalMs ms)"
        }
    }

    private fun executionIntervalMs(cron: String): Long {
        val schedule = CronExpression.parse(cron)
        val firstExecution =
            requireNotNull(schedule.next(LocalDateTime.now())) {
                "Lifecycle cron has no scheduled executions: '$cron'"
            }
        val secondExecution =
            requireNotNull(schedule.next(firstExecution)) {
                "Lifecycle cron has no further scheduled executions: '$cron'"
            }
        return Duration.between(firstExecution, secondExecution).toMillis()
    }
}
