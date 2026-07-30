package cn.anitabi.navigator.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSettingsStoreMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val current by lazy {
        context.getSharedPreferences(AppSettingsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    private val legacy by lazy {
        context.getSharedPreferences(AppSettingsStore.LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    @Before
    fun setUp() {
        current.edit().clear().commit()
        legacy.edit().clear().commit()
    }

    @After
    fun tearDown() {
        current.edit().clear().commit()
        legacy.edit().clear().commit()
    }

    @Test
    fun preservesCompletedGuideAndDeletesLegacyOrsPayloadIdempotently() {
        legacy.edit()
            .putBoolean(AppSettingsStore.LEGACY_ONBOARDING_COMPLETE, true)
            .putString(AppSettingsStore.LEGACY_ORS_KEY, "fake-legacy-ciphertext:fake-iv")
            .commit()

        val first = AppSettingsStore(context)
        val second = AppSettingsStore(context)

        assertTrue(first.hasCompletedOnboarding())
        assertTrue(second.hasCompletedOnboarding())
        assertNull(legacy.getString(AppSettingsStore.LEGACY_ORS_KEY, null))
        assertFalse(legacy.contains(AppSettingsStore.LEGACY_ONBOARDING_COMPLETE))
    }
}
