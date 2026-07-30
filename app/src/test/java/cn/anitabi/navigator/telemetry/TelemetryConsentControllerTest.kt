package cn.anitabi.navigator.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryConsentControllerTest {
    @Test
    fun `stored opt out is applied and unsent crashes are deleted`() {
        val store = FakeStore()
        val runtime = FakeRuntime()

        TelemetryConsentController(store, runtime).applyStoredConsent()

        assertEquals(
            listOf("analytics:false", "crashlytics:false", "crashlytics:delete"),
            runtime.calls,
        )
    }

    @Test
    fun `analytics consent is persisted and withdrawal clears local analytics`() {
        val store = FakeStore()
        val runtime = FakeRuntime()
        val controller = TelemetryConsentController(store, runtime)

        controller.setAnalyticsConsent(true)
        controller.setAnalyticsConsent(false)

        assertEquals(false, store.consent.analyticsEnabled)
        assertEquals(
            listOf(
                "analytics:reset",
                "analytics:true",
                "analytics:false",
                "analytics:reset",
            ),
            runtime.calls,
        )
    }

    @Test
    fun `crash consent never uploads reports created before consent`() {
        val store = FakeStore()
        val runtime = FakeRuntime()
        val controller = TelemetryConsentController(store, runtime)

        controller.setCrashlyticsConsent(true)
        controller.setCrashlyticsConsent(false)

        assertEquals(false, store.consent.crashlyticsEnabled)
        assertEquals(
            listOf(
                "crashlytics:delete",
                "crashlytics:true",
                "crashlytics:false",
                "crashlytics:delete",
            ),
            runtime.calls,
        )
    }

    private class FakeStore : TelemetryConsentStore {
        var consent = TelemetryConsent()

        override fun telemetryConsent(): TelemetryConsent = consent

        override fun setAnalyticsConsent(enabled: Boolean) {
            consent = consent.copy(analyticsEnabled = enabled)
        }

        override fun setCrashlyticsConsent(enabled: Boolean) {
            consent = consent.copy(crashlyticsEnabled = enabled)
        }
    }

    private class FakeRuntime : TelemetryRuntime {
        val calls = mutableListOf<String>()

        override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
            calls += "analytics:$enabled"
        }

        override fun resetAnalyticsData() {
            calls += "analytics:reset"
        }

        override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
            calls += "crashlytics:$enabled"
        }

        override fun deleteUnsentCrashReports() {
            calls += "crashlytics:delete"
        }
    }
}
