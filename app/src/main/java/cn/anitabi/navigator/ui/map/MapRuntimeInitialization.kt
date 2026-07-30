package cn.anitabi.navigator.ui.map

internal fun <T> initializeGoogleMapRuntime(
    initialize: () -> Int,
    create: () -> T,
): T {
    check(initialize() == 0) { "Google Maps runtime initialization failed" }
    return create()
}
