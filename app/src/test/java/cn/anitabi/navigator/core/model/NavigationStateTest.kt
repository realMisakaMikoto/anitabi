package cn.anitabi.navigator.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateTest {
    @Test
    fun `documented navigation sequence is accepted`() {
        val sequence = listOf(
            NavigationState.PLANNED,
            NavigationState.NAVIGATING,
            NavigationState.ARRIVING,
            NavigationState.DWELLING,
            NavigationState.NEXT_STOP,
            NavigationState.COMPLETED,
        )

        assertTrue(sequence.zipWithNext().all { (current, next) -> current.canTransitionTo(next) })
    }

    @Test
    fun `states cannot skip directly to completed`() {
        assertFalse(NavigationState.NAVIGATING.canTransitionTo(NavigationState.COMPLETED))
    }
}
