package cn.anitabi.navigator.navigation

import kotlin.math.roundToInt

internal enum class TransitOverlayForm {
    PANEL,
    BUBBLE,
}

internal data class TransitOverlayViewport(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(right > left && bottom > top) { "Overlay viewport must have positive width and height" }
    }

    val width: Int
        get() = right - left
    val height: Int
        get() = bottom - top
}

internal data class TransitOverlaySizing(
    val defaultPanelWidth: Int,
    val defaultPanelHeight: Int,
    val minimumPanelWidth: Int,
    val minimumPanelHeight: Int,
    val bubbleSize: Int,
    val initialTopOffset: Int,
)

internal data class TransitOverlayPosition(
    val horizontalFraction: Float,
    val verticalFraction: Float,
)

internal data class TransitOverlayLayout(
    val form: TransitOverlayForm,
    val position: TransitOverlayPosition,
    val panelWidth: Int,
    val panelHeight: Int,
)

internal data class TransitOverlayFrame(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

internal fun defaultTransitOverlayLayout(
    viewport: TransitOverlayViewport,
    sizing: TransitOverlaySizing,
): TransitOverlayLayout {
    val width = sizing.defaultPanelWidth.coerceIn(
        sizing.minimumPanelWidth.coerceAtMost(viewport.width),
        viewport.width,
    )
    val height = sizing.defaultPanelHeight.coerceIn(
        sizing.minimumPanelHeight.coerceAtMost(viewport.height),
        viewport.height,
    )
    val initialFrame = clampTransitOverlayFrame(
        TransitOverlayFrame(
            x = viewport.right - width,
            y = viewport.top + sizing.initialTopOffset,
            width = width,
            height = height,
        ),
        viewport,
    )
    return TransitOverlayLayout(
        form = TransitOverlayForm.PANEL,
        position = transitOverlayPosition(initialFrame, viewport),
        panelWidth = width,
        panelHeight = height,
    )
}

internal fun restoreTransitOverlayLayout(
    form: TransitOverlayForm,
    position: TransitOverlayPosition,
    panelWidth: Int,
    panelHeight: Int,
    sizing: TransitOverlaySizing,
): TransitOverlayLayout = TransitOverlayLayout(
    form = form,
    position = TransitOverlayPosition(
        horizontalFraction = position.horizontalFraction.sanitizedFraction(defaultValue = 1f),
        verticalFraction = position.verticalFraction.sanitizedFraction(defaultValue = 0f),
    ),
    panelWidth = panelWidth.coerceAtLeast(sizing.minimumPanelWidth),
    panelHeight = panelHeight.coerceAtLeast(sizing.minimumPanelHeight),
)

internal fun transitOverlayFrame(
    layout: TransitOverlayLayout,
    viewport: TransitOverlayViewport,
    sizing: TransitOverlaySizing,
): TransitOverlayFrame {
    val width = when (layout.form) {
        TransitOverlayForm.PANEL -> layout.panelWidth.coerceIn(
            sizing.minimumPanelWidth.coerceAtMost(viewport.width),
            viewport.width,
        )
        TransitOverlayForm.BUBBLE -> sizing.bubbleSize.coerceAtMost(viewport.width)
    }
    val height = when (layout.form) {
        TransitOverlayForm.PANEL -> layout.panelHeight.coerceIn(
            sizing.minimumPanelHeight.coerceAtMost(viewport.height),
            viewport.height,
        )
        TransitOverlayForm.BUBBLE -> sizing.bubbleSize.coerceAtMost(viewport.height)
    }
    val horizontalFraction = layout.position.horizontalFraction.sanitizedFraction(defaultValue = 1f)
    val verticalFraction = layout.position.verticalFraction.sanitizedFraction(defaultValue = 0f)
    return TransitOverlayFrame(
        x = viewport.left + ((viewport.width - width) * horizontalFraction).roundToInt(),
        y = viewport.top + ((viewport.height - height) * verticalFraction).roundToInt(),
        width = width,
        height = height,
    )
}

internal fun moveTransitOverlay(
    layout: TransitOverlayLayout,
    deltaX: Int,
    deltaY: Int,
    viewport: TransitOverlayViewport,
    sizing: TransitOverlaySizing,
): TransitOverlayLayout {
    val current = transitOverlayFrame(layout, viewport, sizing)
    val moved = clampTransitOverlayFrame(
        current.copy(x = current.x + deltaX, y = current.y + deltaY),
        viewport,
    )
    return layout.copy(position = transitOverlayPosition(moved, viewport))
}

internal fun resizeTransitOverlay(
    layout: TransitOverlayLayout,
    deltaWidth: Int,
    deltaHeight: Int,
    viewport: TransitOverlayViewport,
    sizing: TransitOverlaySizing,
): TransitOverlayLayout {
    if (layout.form != TransitOverlayForm.PANEL) return layout
    val current = transitOverlayFrame(layout, viewport, sizing)
    val maximumWidth = (viewport.right - current.x).coerceAtLeast(1)
    val maximumHeight = (viewport.bottom - current.y).coerceAtLeast(1)
    val minimumWidth = sizing.minimumPanelWidth.coerceAtMost(maximumWidth)
    val minimumHeight = sizing.minimumPanelHeight.coerceAtMost(maximumHeight)
    val resized = current.copy(
        width = (current.width + deltaWidth).coerceIn(minimumWidth, maximumWidth),
        height = (current.height + deltaHeight).coerceIn(minimumHeight, maximumHeight),
    )
    return layout.copy(
        position = transitOverlayPosition(resized, viewport),
        panelWidth = resized.width,
        panelHeight = resized.height,
    )
}

internal fun resizeTransitOverlayFromNearestCorner(
    layout: TransitOverlayLayout,
    targetWidth: Int,
    targetHeight: Int,
    viewport: TransitOverlayViewport,
    sizing: TransitOverlaySizing,
): TransitOverlayLayout {
    if (layout.form != TransitOverlayForm.PANEL) return layout
    val current = transitOverlayFrame(layout, viewport, sizing)
    val width = targetWidth.coerceIn(
        sizing.minimumPanelWidth.coerceAtMost(viewport.width),
        viewport.width,
    )
    val height = targetHeight.coerceIn(
        sizing.minimumPanelHeight.coerceAtMost(viewport.height),
        viewport.height,
    )
    val resized = clampTransitOverlayFrame(
        TransitOverlayFrame(
            x = if (current.centerX() <= viewport.centerX()) current.x else current.x + current.width - width,
            y = if (current.centerY() <= viewport.centerY()) current.y else current.y + current.height - height,
            width = width,
            height = height,
        ),
        viewport,
    )
    return layout.copy(
        position = transitOverlayPosition(resized, viewport),
        panelWidth = resized.width,
        panelHeight = resized.height,
    )
}

internal fun collapseTransitOverlay(
    layout: TransitOverlayLayout,
    viewport: TransitOverlayViewport,
    sizing: TransitOverlaySizing,
): TransitOverlayLayout {
    if (layout.form == TransitOverlayForm.BUBBLE) return layout
    val panel = transitOverlayFrame(layout, viewport, sizing)
    val bubbleWidth = sizing.bubbleSize.coerceAtMost(viewport.width)
    val bubbleHeight = sizing.bubbleSize.coerceAtMost(viewport.height)
    val bubble = clampTransitOverlayFrame(
        TransitOverlayFrame(
            x = if (panel.centerX() <= viewport.centerX()) panel.x else panel.x + panel.width - bubbleWidth,
            y = if (panel.centerY() <= viewport.centerY()) panel.y else panel.y + panel.height - bubbleHeight,
            width = bubbleWidth,
            height = bubbleHeight,
        ),
        viewport,
    )
    return layout.copy(
        form = TransitOverlayForm.BUBBLE,
        position = transitOverlayPosition(bubble, viewport),
    )
}

internal fun expandTransitOverlay(
    layout: TransitOverlayLayout,
    viewport: TransitOverlayViewport,
    sizing: TransitOverlaySizing,
): TransitOverlayLayout {
    if (layout.form == TransitOverlayForm.PANEL) return layout
    val bubble = transitOverlayFrame(layout, viewport, sizing)
    val panelWidth = layout.panelWidth.coerceIn(
        sizing.minimumPanelWidth.coerceAtMost(viewport.width),
        viewport.width,
    )
    val panelHeight = layout.panelHeight.coerceIn(
        sizing.minimumPanelHeight.coerceAtMost(viewport.height),
        viewport.height,
    )
    val panel = clampTransitOverlayFrame(
        TransitOverlayFrame(
            x = if (bubble.centerX() <= viewport.centerX()) bubble.x else bubble.x + bubble.width - panelWidth,
            y = if (bubble.centerY() <= viewport.centerY()) bubble.y else bubble.y + bubble.height - panelHeight,
            width = panelWidth,
            height = panelHeight,
        ),
        viewport,
    )
    return layout.copy(
        form = TransitOverlayForm.PANEL,
        position = transitOverlayPosition(panel, viewport),
    )
}

private fun transitOverlayPosition(
    frame: TransitOverlayFrame,
    viewport: TransitOverlayViewport,
): TransitOverlayPosition {
    val availableX = viewport.width - frame.width
    val availableY = viewport.height - frame.height
    return TransitOverlayPosition(
        horizontalFraction = if (availableX <= 0) 0f else (frame.x - viewport.left).toFloat() / availableX,
        verticalFraction = if (availableY <= 0) 0f else (frame.y - viewport.top).toFloat() / availableY,
    )
}

private fun clampTransitOverlayFrame(
    frame: TransitOverlayFrame,
    viewport: TransitOverlayViewport,
): TransitOverlayFrame {
    val width = frame.width.coerceIn(1, viewport.width)
    val height = frame.height.coerceIn(1, viewport.height)
    return TransitOverlayFrame(
        x = frame.x.coerceIn(viewport.left, viewport.right - width),
        y = frame.y.coerceIn(viewport.top, viewport.bottom - height),
        width = width,
        height = height,
    )
}

private fun TransitOverlayFrame.centerX(): Int = x + width / 2

private fun TransitOverlayFrame.centerY(): Int = y + height / 2

private fun TransitOverlayViewport.centerX(): Int = left + width / 2

private fun TransitOverlayViewport.centerY(): Int = top + height / 2

private fun Float.sanitizedFraction(defaultValue: Float): Float =
    if (isFinite()) coerceIn(0f, 1f) else defaultValue
