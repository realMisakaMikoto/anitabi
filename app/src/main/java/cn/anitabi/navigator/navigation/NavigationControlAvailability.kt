package cn.anitabi.navigator.navigation

import android.Manifest
import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

internal object NavigationControlAvailability {
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                NavigationService.CHANNEL_ID,
                "连续导航",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "在锁屏和后台继续提供巡礼导航"
            },
        )
    }

    fun notificationsVisible(context: Context): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.areNotificationsEnabled()) return false
        ensureChannel(context)
        return manager.getNotificationChannel(NavigationService.CHANNEL_ID)?.importance !=
            NotificationManager.IMPORTANCE_NONE
    }

    fun overlayVisible(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun hasExternalTransitControl(context: Context): Boolean =
        hasVisibleExternalTransitControl(
            overlayVisible = { overlayVisible(context) },
            notificationsVisible = { notificationsVisible(context) },
        )
}

internal fun hasVisibleExternalTransitControl(
    overlayVisible: () -> Boolean,
    notificationsVisible: () -> Boolean,
): Boolean = overlayVisible() || notificationsVisible()

internal fun userVisibleActivityPendingIntent(
    context: Context,
    requestCode: Int,
    intent: Intent,
): PendingIntent {
    val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ActivityOptions.makeBasic().apply {
            pendingIntentCreatorBackgroundActivityStartMode =
                creatorBackgroundActivityStartMode()
        }.toBundle()
    } else {
        null
    }
    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        options,
    )
}

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun creatorBackgroundActivityStartMode(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
    } else {
        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
    }

internal fun navigationNotificationCategory(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Notification.CATEGORY_NAVIGATION
    } else {
        Notification.CATEGORY_SERVICE
    }
