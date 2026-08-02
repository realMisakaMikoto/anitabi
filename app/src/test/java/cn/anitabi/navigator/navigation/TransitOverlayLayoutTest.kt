package cn.anitabi.navigator.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TransitOverlayLayoutTest {
    private val sizing = TransitOverlaySizing(
        defaultPanelWidth = 232,
        defaultPanelHeight = 212,
        minimumPanelWidth = 216,
        minimumPanelHeight = 188,
        bubbleSize = 60,
        initialTopOffset = 72,
    )
    private val portrait = TransitOverlayViewport(left = 8, top = 32, right = 392, bottom = 792)

    @Test
    fun `default panel is compact and inside the safe viewport`() {
        val layout = defaultTransitOverlayLayout(portrait, sizing)

        assertEquals(TransitOverlayForm.PANEL, layout.form)
        assertEquals(TransitOverlayFrame(160, 104, 232, 212), frame(layout))
    }

    @Test
    fun `moving panel and bubble clamps them to every viewport edge`() {
        val panel = defaultTransitOverlayLayout(portrait, sizing)
        val panelTopLeft = moveTransitOverlay(panel, -10_000, -10_000, portrait, sizing)
        val panelBottomRight = moveTransitOverlay(panelTopLeft, 10_000, 10_000, portrait, sizing)
        assertEquals(TransitOverlayFrame(8, 32, 232, 212), frame(panelTopLeft))
        assertEquals(TransitOverlayFrame(160, 580, 232, 212), frame(panelBottomRight))

        val bubble = collapseTransitOverlay(panel, portrait, sizing)
        val bubbleTopLeft = moveTransitOverlay(bubble, -10_000, -10_000, portrait, sizing)
        val bubbleBottomRight = moveTransitOverlay(bubbleTopLeft, 10_000, 10_000, portrait, sizing)
        assertEquals(TransitOverlayForm.BUBBLE, bubbleBottomRight.form)
        assertEquals(TransitOverlayFrame(8, 32, 60, 60), frame(bubbleTopLeft))
        assertEquals(TransitOverlayFrame(332, 732, 60, 60), frame(bubbleBottomRight))
    }

    @Test
    fun `resizing keeps the origin and clamps minimum and available maximum`() {
        val layout = defaultTransitOverlayLayout(portrait, sizing)
        val minimum = resizeTransitOverlay(layout, -10_000, -10_000, portrait, sizing)
        assertEquals(TransitOverlayFrame(160, 104, 216, 188), frame(minimum))

        val moved = moveTransitOverlay(minimum, -10_000, -10_000, portrait, sizing)
        val maximum = resizeTransitOverlay(moved, 10_000, 10_000, portrait, sizing)
        assertEquals(TransitOverlayFrame(8, 32, 384, 760), frame(maximum))

        val bubble = collapseTransitOverlay(layout, portrait, sizing)
        assertEquals(bubble, resizeTransitOverlay(bubble, 100, 100, portrait, sizing))
    }

    @Test
    fun `collapse and expand preserve the panel size at every corner`() {
        val panel = defaultTransitOverlayLayout(portrait, sizing)
        listOf(
            Triple(-10_000 to -10_000, TransitOverlayFrame(8, 32, 60, 60), "top left"),
            Triple(10_000 to -10_000, TransitOverlayFrame(332, 32, 60, 60), "top right"),
            Triple(-10_000 to 10_000, TransitOverlayFrame(8, 732, 60, 60), "bottom left"),
            Triple(10_000 to 10_000, TransitOverlayFrame(332, 732, 60, 60), "bottom right"),
        ).forEach { (delta, expectedBubble, corner) ->
            val moved = moveTransitOverlay(panel, delta.first, delta.second, portrait, sizing)
            val bubble = collapseTransitOverlay(moved, portrait, sizing)
            val expanded = expandTransitOverlay(bubble, portrait, sizing)

            assertEquals(corner, expectedBubble, frame(bubble))
            assertEquals(frame(moved), frame(expanded))
            assertEquals(panel.panelWidth, expanded.panelWidth)
            assertEquals(panel.panelHeight, expanded.panelHeight)
        }
    }

    @Test
    fun `compact size toggle preserves every nearest corner and can expand again`() {
        val panel = defaultTransitOverlayLayout(portrait, sizing)
        listOf(
            Triple(-10_000 to -10_000, TransitOverlayFrame(8, 32, 216, 188), "top left"),
            Triple(10_000 to -10_000, TransitOverlayFrame(176, 32, 216, 188), "top right"),
            Triple(-10_000 to 10_000, TransitOverlayFrame(8, 604, 216, 188), "bottom left"),
            Triple(10_000 to 10_000, TransitOverlayFrame(176, 604, 216, 188), "bottom right"),
        ).forEach { (delta, expectedCompact, corner) ->
            val moved = moveTransitOverlay(panel, delta.first, delta.second, portrait, sizing)
            val compact = resizeTransitOverlayFromNearestCorner(
                moved,
                sizing.minimumPanelWidth,
                sizing.minimumPanelHeight,
                portrait,
                sizing,
            )
            val expanded = resizeTransitOverlayFromNearestCorner(
                compact,
                sizing.defaultPanelWidth,
                sizing.defaultPanelHeight,
                portrait,
                sizing,
            )

            assertEquals(corner, expectedCompact, frame(compact))
            assertEquals(corner, frame(moved), frame(expanded))
        }
    }

    @Test
    fun `restored layout preserves bubble mode and sanitizes saved geometry`() {
        val restored = restoreTransitOverlayLayout(
            form = TransitOverlayForm.BUBBLE,
            position = TransitOverlayPosition(Float.NaN, Float.POSITIVE_INFINITY),
            panelWidth = -1,
            panelHeight = 1,
            sizing = sizing,
        )

        assertEquals(TransitOverlayForm.BUBBLE, restored.form)
        assertEquals(sizing.minimumPanelWidth, restored.panelWidth)
        assertEquals(sizing.minimumPanelHeight, restored.panelHeight)
        assertEquals(TransitOverlayFrame(332, 32, 60, 60), frame(restored))
    }

    @Test
    fun `viewport change reclamps both forms without changing preferred panel size`() {
        val panel = defaultTransitOverlayLayout(portrait, sizing)
        val landscape = TransitOverlayViewport(left = 24, top = 8, right = 776, bottom = 392)
        val landscapePanel = transitOverlayFrame(panel, landscape, sizing)
        val landscapeBubble = transitOverlayFrame(
            collapseTransitOverlay(panel, portrait, sizing),
            landscape,
            sizing,
        )

        assertEquals(TransitOverlayFrame(544, 31, 232, 212), landscapePanel)
        assertEquals(TransitOverlayFrame(716, 41, 60, 60), landscapeBubble)
        assertEquals(232, panel.panelWidth)
        assertEquals(212, panel.panelHeight)
    }

    @Test
    fun `valid saved bubble position and panel size survive restore`() {
        val restored = restoreTransitOverlayLayout(
            form = TransitOverlayForm.BUBBLE,
            position = TransitOverlayPosition(0.25f, 0.75f),
            panelWidth = 260,
            panelHeight = 240,
            sizing = sizing,
        )
        val bubble = frame(restored)
        val expanded = frame(expandTransitOverlay(restored, portrait, sizing))

        assertEquals(TransitOverlayFrame(89, 557, 60, 60), bubble)
        assertEquals(TransitOverlayFrame(89, 377, 260, 240), expanded)
        assertEquals(TransitOverlayForm.BUBBLE, restored.form)
    }

    @Test
    fun `viewport smaller than every minimum still keeps both forms visible`() {
        val tiny = TransitOverlayViewport(left = 5, top = 7, right = 45, bottom = 37)
        val panel = defaultTransitOverlayLayout(tiny, sizing)
        val moved = moveTransitOverlay(panel, 10_000, -10_000, tiny, sizing)
        val resized = resizeTransitOverlay(moved, -10_000, 10_000, tiny, sizing)
        val bubble = collapseTransitOverlay(resized, tiny, sizing)
        val expanded = expandTransitOverlay(bubble, tiny, sizing)

        listOf(panel, moved, resized, bubble, expanded).forEach { layout ->
            assertEquals(TransitOverlayFrame(5, 7, 40, 30), transitOverlayFrame(layout, tiny, sizing))
        }
    }

    private fun frame(layout: TransitOverlayLayout): TransitOverlayFrame =
        transitOverlayFrame(layout, portrait, sizing)
}
