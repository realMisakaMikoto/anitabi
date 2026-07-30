package cn.anitabi.navigator.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewportTest {
    @Test
    fun `fit waits until the hosted map has positive dimensions`() {
        var invoked = false

        val zeroWidth = withPositiveMapViewport(width = 0, height = 640) { _, _ ->
            invoked = true
        }
        val zeroHeight = withPositiveMapViewport(width = 360, height = 0) { _, _ ->
            invoked = true
        }

        assertNull(zeroWidth)
        assertNull(zeroHeight)
        assertFalse(invoked)
    }

    @Test
    fun `fit receives the exact laid out map dimensions`() {
        var invoked = false

        val result = withPositiveMapViewport(width = 360, height = 640) { width, height ->
            invoked = true
            width to height
        }

        assertTrue(invoked)
        assertEquals(360 to 640, result)
    }
}
