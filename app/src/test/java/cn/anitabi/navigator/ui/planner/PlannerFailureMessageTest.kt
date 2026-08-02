package cn.anitabi.navigator.ui.planner

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.data.network.ApiException
import cn.anitabi.navigator.core.routing.TransitSegmentUnavailableException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlannerFailureMessageTest {
    @Test
    fun `transport failure identifies the route service and blocked network exit`() {
        val message = plannerFailureMessage(ApiException.Network(IOException("reset")))

        assertEquals(
            "无法连接路线服务；当前网络出口可能被拦截，请切换网络后重试",
            message,
        )
    }

    @Test
    fun `missing transit and walk route names the failed segment without blaming the region`() {
        val message = plannerFailureMessage(
            TransitSegmentUnavailableException(
                segmentNumber = 2,
                segmentCount = 5,
                from = GeoPoint(1.0, 2.0),
                to = GeoPoint(3.0, 4.0),
            ),
        )

        assertEquals(
            "第 2/5 段在所选时间未找到公交或步行路线，请调整时间、顺序或出行方式",
            message,
        )
    }

    @Test
    fun `unavailable segment details preserve endpoint names coordinates and current location`() {
        val destination = PilgrimagePoint("destination", "测试终点", GeoPoint(3.0, 4.0))
        val state = PlannerUiState(
            selectedPoints = listOf(destination),
            useCurrentLocation = true,
        )

        val details = unavailableRouteSegmentDetails(
            throwable = TransitSegmentUnavailableException(
                segmentNumber = 1,
                segmentCount = 2,
                from = GeoPoint(1.0, 2.0),
                to = destination.coordinate,
            ),
            state = state,
        )

        requireNotNull(details)
        assertEquals("当前位置", details.origin.name)
        assertEquals(GeoPoint(1.0, 2.0), details.origin.coordinate)
        assertEquals("测试终点", details.destination.name)
        assertEquals(destination.coordinate, details.destination.coordinate)
        assertNull(unavailableRouteSegmentDetails(ApiException.NoRoute(), state))
    }

}
