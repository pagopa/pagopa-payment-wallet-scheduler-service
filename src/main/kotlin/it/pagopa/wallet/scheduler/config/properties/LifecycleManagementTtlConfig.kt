package it.pagopa.wallet.scheduler.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("lifecycle-management-job.ttl")
class LifecycleManagementTtlConfig(
    val shortTermRetentionDays: Int,
    val longTermRetentionYears: Int,
    val instantDeleteTtlSeconds: Int
) {
    init {
        require(shortTermRetentionDays > 0) {
            "Short term retention days must be greater than zero"
        }
        require(longTermRetentionYears > 0) {
            "Long term retention years must be greater than zero"
        }
        require(instantDeleteTtlSeconds > 0) {
            "Increment delete ttl seconds must be greater than zero"
        }
        require(shortTermRetentionDays < 24855) {
            "Short term retention days must be less than 24855 (68 years)"
        }
        require(longTermRetentionYears < 68) {
            "Long term retention years must be less than 68 years"
        }
    }
}
