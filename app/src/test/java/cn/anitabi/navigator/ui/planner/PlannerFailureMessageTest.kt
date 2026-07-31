package cn.anitabi.navigator.ui.planner

import cn.anitabi.navigator.data.network.ApiException
import cn.anitabi.navigator.core.routing.TransitSegmentUnavailableException
import java.io.IOException
import org.junit.Assert.assertEquals
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
        val message = plannerFailureMessage(TransitSegmentUnavailableException(2, 5))

        assertEquals(
            "第 2/5 段在所选时间未找到公交或步行路线，请调整时间、顺序或出行方式",
            message,
        )
    }
}
