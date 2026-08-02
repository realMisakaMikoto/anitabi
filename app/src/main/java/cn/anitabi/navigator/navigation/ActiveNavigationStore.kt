package cn.anitabi.navigator.navigation

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit

internal object ActiveNavigationStore {
    private const val PREFERENCES = "active_navigation"
    private const val KEY_TOUR_ID = "tour_id"

    fun get(context: Context): String? = context
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .getString(KEY_TOUR_ID, null)

    @Synchronized
    fun set(context: Context, tourId: String) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            putString(KEY_TOUR_ID, tourId)
        }
    }

    @Synchronized
    @SuppressLint("UseKtx") // Native Editor preserves the durable commit result.
    fun clear(
        context: Context,
        expectedTourId: String? = null,
        durable: Boolean = false,
    ): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (expectedTourId != null && preferences.getString(KEY_TOUR_ID, null) != expectedTourId) return false
        val editor = preferences.edit().remove(KEY_TOUR_ID)
        if (durable) return editor.commit()
        editor.apply()
        return true
    }

    @Synchronized
    @SuppressLint("UseKtx") // Native Editor preserves the durable commit result.
    fun replaceIfCurrent(
        context: Context,
        expectedTourId: String?,
        tourId: String?,
        durable: Boolean = false,
    ): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (preferences.getString(KEY_TOUR_ID, null) != expectedTourId) return false
        val editor = preferences.edit()
        if (tourId == null) editor.remove(KEY_TOUR_ID) else editor.putString(KEY_TOUR_ID, tourId)
        if (durable) return editor.commit()
        editor.apply()
        return true
    }
}
