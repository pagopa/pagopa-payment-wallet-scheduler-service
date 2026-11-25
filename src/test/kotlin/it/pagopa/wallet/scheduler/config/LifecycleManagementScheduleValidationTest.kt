package it.pagopa.wallet.scheduler.config

import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementQueryConfig
import it.pagopa.wallet.scheduler.config.properties.QuerySettings
import java.time.LocalTime
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class LifecycleManagementScheduleValidationTest {
    @ParameterizedTest
    @CsvSource(
        "0 */1 * * * *, 60, 120",
        "15 */1 * * * *, 60, 120",
        "*/10 * * * * *, 10, 20",
        "0 */5 * * * *, 300, 600"
    )
    fun `should accept the exact capacity of the fixed interval`(
        cron: String,
        lowRate: Int,
        highRate: Int
    ) {
        LifecycleManagementScheduleValidation(cron, query(lowRate, highRate))
    }

    @ParameterizedTest
    @CsvSource(
        "0 */1 * * * *, 61, 120, lowRate, 60",
        "0 */1 * * * *, 60, 121, highRate, 120",
        "0 */5 * * * *, 301, 600, lowRate, 300",
        "*/10 * * * * *, 10, 21, highRate, 20"
    )
    fun `should reject batches that exceed either rate budget`(
        cron: String,
        lowRate: Int,
        highRate: Int,
        setting: String,
        capacity: Int
    ) {
        val error =
            assertFailsWith<IllegalArgumentException> {
                LifecycleManagementScheduleValidation(cron, query(lowRate, highRate))
            }
        assertContains(error.message.orEmpty(), "Lifecycle $setting=")
        assertContains(error.message.orEmpty(), "maximum of $capacity wallets")
    }

    @Test
    fun `should skip validation when scheduling is disabled`() {
        LifecycleManagementScheduleValidation("-", query(1000, 1000))
    }

    @Test
    fun `should reject an invalid cron expression`() {
        assertFailsWith<IllegalArgumentException> {
            LifecycleManagementScheduleValidation("0 60 * * * *", query(1, 1))
        }
    }

    private fun query(lowRate: Int, highRate: Int) =
        LifecycleManagementQueryConfig(
            QuerySettings(lowRate, highRate, 60, LocalTime.of(22, 0), LocalTime.of(23, 0)),
            contractDeactivationMaxRetries = 2,
            contractDeactivationLowRateDelayMs = 1000,
            contractDeactivationHighRateDelayMs = 500,
            ttlSeconds = 5
        )
}
