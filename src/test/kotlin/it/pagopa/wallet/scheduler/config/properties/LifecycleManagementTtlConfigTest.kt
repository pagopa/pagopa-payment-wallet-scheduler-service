package it.pagopa.wallet.scheduler.config.properties

import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class LifecycleManagementTtlConfigTest {

    companion object {
        @JvmStatic
        fun `Should throw error building LifecycleManagementTtlConfig for invalid values`():
            Stream<Arguments> =
            Stream.of(
                Arguments.of(0, 0, 0, "Short term retention days must be greater than zero"),
                Arguments.of(1, 0, 0, "Long term retention years must be greater than zero"),
                Arguments.of(1, 1, 0, "Increment delete ttl seconds must be greater than zero"),
                Arguments.of(
                    24855,
                    1,
                    1,
                    "Short term retention days must be less than 24855 (68 years)"
                ),
                Arguments.of(1, 68, 1, "Long term retention years must be less than 68 years"),
            )
    }

    @Test
    fun `Should construct LifecycleManagementTtlConfig for valid values`() {
        // test
        assertDoesNotThrow { LifecycleManagementTtlConfig(1, 1, 1) }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should throw error building LifecycleManagementTtlConfig for invalid values`(
        shortTermRetentionDays: Int,
        longTermRetentionYears: Int,
        instantDeleteTtlSeconds: Int,
        expectedErrorMessage: String
    ) {
        val exception =
            assertThrows<IllegalArgumentException> {
                LifecycleManagementTtlConfig(
                    shortTermRetentionDays,
                    longTermRetentionYears,
                    instantDeleteTtlSeconds
                )
            }
        assertEquals(expectedErrorMessage, exception.message)
    }
}
