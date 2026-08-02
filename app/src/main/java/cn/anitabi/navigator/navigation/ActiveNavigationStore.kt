package cn.anitabi.navigator.navigation

import android.content.Context
import androidx.core.content.edit

internal object ActiveNavigationStore {
    private const val PREFERENCES = "active_navigation"
    private const val KEY_TOUR_ID = "tour_id"

    fun get(context: Context): String? = context
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .getString(KEY_TOUR_ID, null)

    fun set(context: Context, tourId: String) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            putString(KEY_TOUR_ID, tourId)
        }
    }

    fun clear(context: Context, expectedTourId: String? = null) {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (expectedTourId != null && preferences.getString(KEY_TOUR_ID, null) != expectedTourId) return
        preferences.edit { remove(KEY_TOUR_ID) }
    }

    @Synchronized
    fun replaceIfCurrent(context: Context, expectedTourId: String?, tourId: String?): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (preferences.getString(KEY_TOUR_ID, null) != expectedTourId) return false
        preferences.edit {
            if (tourId == null) remove(KEY_TOUR_ID) else putString(KEY_TOUR_ID, tourId)
        }
        return true
    }
}
