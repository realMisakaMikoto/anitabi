package cn.anitabi.navigator.ui.map

internal inline fun <T> withPositiveMapViewport(
    width: Int,
    height: Int,
    block: (width: Int, height: Int) -> T,
): T? {
    if (width <= 0 || height <= 0) return null
    return block(width, height)
}
