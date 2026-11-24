package it.pagopa.wallet.scheduler.config

import it.pagopa.wallet.scheduler.config.properties.LifecycleManagementQueryConfig
import it.pagopa.wallet.scheduler.config.properties.QuerySettings
import java.time.Duration
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
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
        "23:00, 500",
        "23:00:01, 500",
        "00:00, 500",
        "00:59:59, 500",
        "01:00, 500",
        "03:00, 500",
        "03:00:01, 2000"
    )
    fun `should use high API speed throughout the query burst window`(
        time: String,
        expectedDelayMs: Long
    ) {
        assertEquals(
            Duration.ofMillis(expectedDelayMs),
            config.contractDeactivationInvocationDelay(LocalTime.parse(time))
        )
    }

    @ParameterizedTest
    @CsvSource("09:59:59, 2000", "10:00, 500", "11:00, 500", "12:00, 500", "12:00:01, 2000")
    fun `should select delays for a same day burst window`(time: String, expectedDelayMs: Long) {
        val sameDay =
            config.copy(
                settings =
                    config.settings.copy(
                        burstStartWindow = LocalTime.of(10, 0),
                        burstEndWindow = LocalTime.of(12, 0)
                    )
            )
        assertEquals(
            Duration.ofMillis(expectedDelayMs),
            sameDay.contractDeactivationInvocationDelay(LocalTime.parse(time))
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
