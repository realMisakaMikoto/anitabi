package cn.anitabi.navigator.navigation

import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.data.network.ApiException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationFailureMessageTest {
    @Test
    fun `quota error is localized and explains that billing has stopped`() {
        assertEquals(
            "路线额度已用尽，暂时无法开始；不会继续产生费用",
            navigationFailureMessage(ApiException.QuotaExhausted()),
        )
    }

    @Test
    fun `network error does not expose the exception message`() {
        assertEquals(
            "无法连接路线服务，请检查网络后重试",
            navigationFailureMessage(ApiException.Network(IOException("private network detail"))),
        )
    }

    @Test
    fun `upstream failure is not described as missing transit coverage`() {
        assertEquals(
            "Google 路线服务暂时不可用，请稍后再试",
            navigationFailureMessage(ApiException.UpstreamUnavailable()),
        )
    }

    @Test
    fun `failed navigation becomes resumable instead of remaining in progress`() {
        val progress = NavigationProgress(
            tourId = "tour",
            legIndex = 3,
            state = NavigationState.NAVIGATING,
            offRouteSinceEpochMillis = 123L,
        )

        val resumable = requireNotNull(resumableProgressAfterFailure(progress))

        assertEquals(NavigationState.PLANNED, resumable.state)
        assertEquals(3, resumable.legIndex)
        assertNull(resumable.offRouteSinceEpochMillis)
    }

    @Test
    fun `completed navigation is never reopened after a later failure`() {
        val progress = NavigationProgress(tourId = "tour", state = NavigationState.COMPLETED)

        assertNull(resumableProgressAfterFailure(progress))
    }
}
