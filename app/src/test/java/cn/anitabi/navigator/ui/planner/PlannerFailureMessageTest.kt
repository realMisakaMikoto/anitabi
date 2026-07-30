package cn.anitabi.navigator.ui.planner

import cn.anitabi.navigator.data.network.ApiException
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
}
