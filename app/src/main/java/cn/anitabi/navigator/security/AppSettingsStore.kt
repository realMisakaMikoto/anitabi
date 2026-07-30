package cn.anitabi.navigator.security

import android.content.Context
import androidx.core.content.edit
import java.security.KeyStore

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyRoutingSettings(context)
    }

    fun hasCompletedOnboarding(): Boolean =
        preferences.getBoolean(PREFERENCE_ONBOARDING_COMPLETE, false)

    fun markOnboardingComplete() {
        preferences.edit(commit = true) { putBoolean(PREFERENCE_ONBOARDING_COMPLETE, true) }
    }

    private fun migrateLegacyRoutingSettings(context: Context) {
        val legacy = context.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (!preferences.contains(PREFERENCE_ONBOARDING_COMPLETE) &&
            legacy.getBoolean(LEGACY_ONBOARDING_COMPLETE, false)
        ) {
            preferences.edit(commit = true) { putBoolean(PREFERENCE_ONBOARDING_COMPLETE, true) }
        }
        legacy.edit(commit = true) {
            remove(LEGACY_ORS_KEY)
            remove(LEGACY_ONBOARDING_COMPLETE)
        }
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.let { keyStore ->
                if (keyStore.containsAlias(LEGACY_KEY_ALIAS)) keyStore.deleteEntry(LEGACY_KEY_ALIAS)
            }
        }
    }

    companion object {
        internal const val PREFERENCES_NAME = "anitabi_settings_v2"
        internal const val PREFERENCE_ONBOARDING_COMPLETE = "onboarding_complete"
        internal const val LEGACY_PREFERENCES_NAME = "secure_routing_settings"
        internal const val LEGACY_ORS_KEY = "ors_key_encrypted"
        internal const val LEGACY_ONBOARDING_COMPLETE = "onboarding_complete"
        internal const val LEGACY_KEY_ALIAS = "anitabi_ors_key_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}
