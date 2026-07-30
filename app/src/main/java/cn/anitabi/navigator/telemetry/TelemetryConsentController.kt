package cn.anitabi.navigator.telemetry

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

data class TelemetryConsent(
    val analyticsEnabled: Boolean = false,
    val crashlyticsEnabled: Boolean = false,
)

interface TelemetryConsentStore {
    fun telemetryConsent(): TelemetryConsent

    fun setAnalyticsConsent(enabled: Boolean)

    fun setCrashlyticsConsent(enabled: Boolean)
}

interface TelemetryRuntime {
    fun setAnalyticsCollectionEnabled(enabled: Boolean)

    fun resetAnalyticsData()

    fun setCrashlyticsCollectionEnabled(enabled: Boolean)

    fun deleteUnsentCrashReports()
}

class TelemetryConsentController(
    private val store: TelemetryConsentStore,
    private val runtime: TelemetryRuntime,
) {
    fun currentConsent(): TelemetryConsent = store.telemetryConsent()

    fun applyStoredConsent() {
        val consent = store.telemetryConsent()
        runtime.setAnalyticsCollectionEnabled(consent.analyticsEnabled)
        runtime.setCrashlyticsCollectionEnabled(consent.crashlyticsEnabled)
        if (!consent.crashlyticsEnabled) runtime.deleteUnsentCrashReports()
    }

    fun setAnalyticsConsent(enabled: Boolean) {
        store.setAnalyticsConsent(enabled)
        if (enabled) {
            runtime.resetAnalyticsData()
            runtime.setAnalyticsCollectionEnabled(true)
        } else {
            runtime.setAnalyticsCollectionEnabled(false)
            runtime.resetAnalyticsData()
        }
    }

    fun setCrashlyticsConsent(enabled: Boolean) {
        store.setCrashlyticsConsent(enabled)
        if (enabled) {
            runtime.deleteUnsentCrashReports()
            runtime.setCrashlyticsCollectionEnabled(true)
        } else {
            runtime.setCrashlyticsCollectionEnabled(false)
            runtime.deleteUnsentCrashReports()
        }
    }
}

class FirebaseTelemetryRuntime(context: Context) : TelemetryRuntime {
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    override fun resetAnalyticsData() {
        analytics.resetAnalyticsData()
    }

    override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }

    override fun deleteUnsentCrashReports() {
        crashlytics.deleteUnsentReports()
    }
}
