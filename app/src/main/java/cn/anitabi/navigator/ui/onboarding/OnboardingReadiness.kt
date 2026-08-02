package cn.anitabi.navigator.ui.onboarding

internal data class OnboardingReadiness(
    val hasLocationPermission: Boolean,
    val hasNotificationPermission: Boolean,
) {
    val permissionsReady: Boolean
        get() = hasLocationPermission

    val canFinish: Boolean
        get() = permissionsReady
}

internal fun onboardingPermissionError(
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
): String? = when {
    !hasLocationPermission -> "还需要定位权限，请授权后继续"
    else -> null
}
