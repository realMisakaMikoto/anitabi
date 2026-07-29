package cn.anitabi.navigator.ui.onboarding

internal data class OnboardingReadiness(
    val hasLocationPermission: Boolean,
    val hasNotificationPermission: Boolean,
    val hasOrsKey: Boolean,
) {
    val permissionsReady: Boolean
        get() = hasLocationPermission && hasNotificationPermission

    val canFinish: Boolean
        get() = permissionsReady && hasOrsKey
}

internal fun onboardingPermissionError(
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
): String? = when {
    !hasLocationPermission && !hasNotificationPermission -> "还需要定位和通知权限，请授权后继续"
    !hasLocationPermission -> "还需要定位权限，请授权后继续"
    !hasNotificationPermission -> "还需要通知权限，请授权后继续"
    else -> null
}
