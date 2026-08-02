package cn.anitabi.navigator.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidLocationProviderTest {
    @Test
    fun `location exactly thirty seconds old and one hundred meters accurate qualifies`() {
        assertTrue(
            isQualifiedLocationFix(
                ageMillis = 30_000L,
                accuracyMeters = 100.0,
                maxAgeMillis = 30_000L,
                maxAccuracyMeters = 100.0,
            ),
        )
    }

    @Test
    fun `location older than thirty seconds does not qualify`() {
        assertFalse(
            isQualifiedLocationFix(
                ageMillis = 30_001L,
                accuracyMeters = 100.0,
                maxAgeMillis = 30_000L,
                maxAccuracyMeters = 100.0,
            ),
        )
    }

    @Test
    fun `location less accurate than one hundred meters does not qualify`() {
        assertFalse(
            isQualifiedLocationFix(
                ageMillis = 30_000L,
                accuracyMeters = 100.001,
                maxAgeMillis = 30_000L,
                maxAccuracyMeters = 100.0,
            ),
        )
    }

    @Test
    fun `non-finite location accuracy does not qualify`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { accuracy ->
            assertFalse(
                isQualifiedLocationFix(
                    ageMillis = 0L,
                    accuracyMeters = accuracy,
                    maxAgeMillis = 30_000L,
                    maxAccuracyMeters = 100.0,
                ),
            )
        }
    }
}
