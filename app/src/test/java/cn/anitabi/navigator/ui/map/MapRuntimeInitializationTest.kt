package cn.anitabi.navigator.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class MapRuntimeInitializationTest {
    @Test
    fun initializesMapFactoriesBeforeCreatingTheView() {
        val calls = mutableListOf<String>()

        val result = initializeGoogleMapRuntime(
            initialize = {
                calls += "initialize"
                0
            },
            create = {
                calls += "create"
                "view"
            },
        )

        assertEquals("view", result)
        assertEquals(listOf("initialize", "create"), calls)
    }

    @Test
    fun refusesToCreateTheViewWhenFactoryInitializationFails() {
        var created = false

        assertThrows(IllegalStateException::class.java) {
            initializeGoogleMapRuntime(
                initialize = { 2 },
                create = {
                    created = true
                },
            )
        }

        assertFalse(created)
    }
}
