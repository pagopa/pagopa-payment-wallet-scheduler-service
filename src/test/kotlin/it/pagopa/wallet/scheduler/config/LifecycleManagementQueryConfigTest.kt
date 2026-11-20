package it.pagopa.wallet.scheduler.config

import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementQueryConfig
import it.pagopa.wallet.scheduler.config.properties.QuerySettings
import java.time.Duration
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class LifecycleManagementQueryConfigTest {
    private val config =
        LifecycleManagementQueryConfig(
            QuerySettings(10, 20, 7200, LocalTime.of(23, 0), LocalTime.of(3, 0)),
            contractDeactivationMaxRetries = 2,
            contractDeactivationLowRateDelayMs = 2000,
            contractDeactivationHighRateDelayMs = 500,
            ttlSeconds = 600
        )

    @ParameterizedTest
    @CsvSource(
        "22:59:59, 2000",
        "23:00, 2000",
        "00:00, 1250",
        "01:00, 500",
        "03:00, 500",
        "03:00:01, 2000"
    )
    fun `should use the query burst window and ramp up the API speed`(
        time: String,
        expectedDelayMs: Long
    ) {
        assertEquals(
            Duration.ofMillis(expectedDelayMs),
            config.contractDeactivationInvocationDelay(LocalTime.parse(time))
        )
    }

    @ParameterizedTest
    @CsvSource("1000, 0", "1000, -1", "500, 1000")
    fun `should reject invalid API delays`(lowDelayMs: Long, highDelayMs: Long) {
        assertFailsWith<IllegalArgumentException> {
            config.copy(
                contractDeactivationLowRateDelayMs = lowDelayMs,
                contractDeactivationHighRateDelayMs = highDelayMs
            )
        }
    }
}
