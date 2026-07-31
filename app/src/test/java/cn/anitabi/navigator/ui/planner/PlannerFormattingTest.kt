package cn.anitabi.navigator.ui.planner

import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.core.model.TransitRoutingPreference
import cn.anitabi.navigator.core.model.TransitTimeMode
import cn.anitabi.navigator.core.model.TransitTravelMode
import cn.anitabi.navigator.core.routing.TransitRideUnavailableException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerFormattingTest {
    @Test
    fun `transit UTC timestamps use the stop time zone`() {
        assertEquals("09:01", formatTransitTime("2026-07-30T00:01:00Z", "Asia/Tokyo"))
        assertEquals("00:01", formatTransitTime("2026-07-30T00:01:00Z", null))
        assertEquals("时间未知", formatTransitTime(null, "Asia/Tokyo"))
    }

    @Test
    fun `navigation requires both location and notification permissions`() {
        assertNull(navigationPermissionError(hasLocation = true, hasNotifications = true))
        assertEquals(
            "需要定位权限才能开始导航",
            navigationPermissionError(hasLocation = false, hasNotifications = true),
        )
        assertEquals(
            "需要通知权限才能在锁屏和后台持续导航",
            navigationPermissionError(hasLocation = true, hasNotifications = false),
        )
        assertEquals(
            "需要定位和通知权限才能开始导航",
            navigationPermissionError(hasLocation = false, hasNotifications = false),
        )
    }

    @Test
    fun `Google walking and cycling routes show beta notice`() {
        val notice = "Google 地图的步行和骑行路线仍为测试版，请以现场道路和交通规则为准。"

        assertEquals(notice, googleRouteBetaNotice(TravelMode.WALK))
        assertEquals(notice, googleRouteBetaNotice(TravelMode.BIKE))
        assertNull(googleRouteBetaNotice(TravelMode.DRIVE))
        assertNull(googleRouteBetaNotice(TravelMode.TRANSIT))
    }

    @Test
    fun `transit schedule labels follow Google Maps time semantics`() {
        val today = LocalDate.of(2026, 7, 31)

        assertEquals(
            "现在出发",
            transitScheduleLabel(TransitTimeMode.NOW, today, LocalTime.of(12, 36), today),
        )
        assertEquals(
            "今天 12:36 出发",
            transitScheduleLabel(TransitTimeMode.DEPART_AT, today, LocalTime.of(12, 36), today),
        )
        assertEquals(
            "8月1日 09:00 前到达",
            transitScheduleLabel(TransitTimeMode.ARRIVE_BY, today.plusDays(1), LocalTime.of(9, 0), today),
        )
    }

    @Test
    fun `transit preference labels are concise`() {
        assertEquals("最佳路线", transitPreferenceLabel(TransitRoutingPreference.RECOMMENDED))
        assertEquals("少步行", transitPreferenceLabel(TransitRoutingPreference.LESS_WALKING))
        assertEquals("少换乘", transitPreferenceLabel(TransitRoutingPreference.FEWER_TRANSFERS))
    }

    @Test
    fun `empty transit mode set displays all four Google travel modes`() {
        assertEquals(allTransitTravelModes.toSet(), selectedTransitTravelModes(emptySet()))
        assertEquals("全部方式", transitTravelModesLabel(emptySet()))
        assertEquals(
            "最佳路线 · 全部方式",
            transitOptionsSummaryLabel(TransitRoutingPreference.RECOMMENDED, emptySet()),
        )
    }

    @Test
    fun `transit mode toggle normalizes all selected modes back to empty`() {
        val withoutBus = toggledTransitTravelModes(emptySet(), TransitTravelMode.BUS)

        assertEquals(
            setOf(TransitTravelMode.SUBWAY, TransitTravelMode.TRAIN, TransitTravelMode.LIGHT_RAIL),
            withoutBus,
        )
        assertEquals(emptySet<TransitTravelMode>(), toggledTransitTravelModes(withoutBus, TransitTravelMode.BUS))
    }

    @Test
    fun `transit mode toggle keeps at least one selected`() {
        val busOnly = setOf(TransitTravelMode.BUS)

        assertEquals(busOnly, toggledTransitTravelModes(busOnly, TransitTravelMode.BUS))
        assertEquals(
            "公交、火车",
            transitTravelModesLabel(setOf(TransitTravelMode.TRAIN, TransitTravelMode.BUS)),
        )
    }

    @Test
    fun `now transit anchor is captured when planning starts`() {
        val now = ZonedDateTime.of(2026, 7, 31, 12, 36, 47, 123_000_000, ZoneId.of("Asia/Shanghai"))

        assertEquals(
            now,
            resolveTransitAnchor(
                mode = TransitTimeMode.NOW,
                date = LocalDate.of(1970, 1, 1),
                time = LocalTime.MIDNIGHT,
                now = now,
            ),
        )
    }

    @Test
    fun `current transit planning time preserves seconds`() {
        val instant = java.time.Instant.parse("2026-07-31T04:36:47.123Z")
        val clock = java.time.Clock.fixed(instant, ZoneId.of("Asia/Shanghai"))

        assertEquals(
            ZonedDateTime.of(2026, 7, 31, 12, 36, 47, 123_000_000, ZoneId.of("Asia/Shanghai")),
            currentTransitPlanningTime(clock),
        )
    }

    @Test
    fun `walking-only result explains the Routes API Japan limitation`() {
        val message = plannerFailureMessage(TransitRideUnavailableException())

        assertTrue(message.contains("未将全步行路线作为公交方案"))
        assertTrue(message.contains("日本"))
    }

    @Test
    fun `scheduled transit anchor accepts recent history and rejects outside the Google window`() {
        val now = ZonedDateTime.of(2026, 7, 31, 12, 36, 0, 0, ZoneId.of("Asia/Shanghai"))

        assertEquals(
            now.minusMinutes(36),
            resolveTransitAnchor(TransitTimeMode.DEPART_AT, now.toLocalDate(), LocalTime.NOON, now),
        )
        assertThrows(InvalidTransitScheduleException::class.java) {
            val tooOld = now.minusDays(7).minusMinutes(1)
            resolveTransitAnchor(TransitTimeMode.DEPART_AT, tooOld.toLocalDate(), tooOld.toLocalTime(), now)
        }
        assertThrows(InvalidTransitScheduleException::class.java) {
            resolveTransitAnchor(
                TransitTimeMode.ARRIVE_BY,
                now.toLocalDate().plusDays(101),
                LocalTime.NOON,
                now,
            )
        }
    }

    @Test
    fun `one hundred day transit boundary is compared as an instant across daylight saving`() {
        val zone = ZoneId.of("America/Los_Angeles")
        val now = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, zone)
        val boundary = now.toInstant().plusSeconds(100L * 24 * 60 * 60).atZone(zone)

        assertEquals(
            boundary,
            resolveTransitAnchor(
                TransitTimeMode.DEPART_AT,
                boundary.toLocalDate(),
                boundary.toLocalTime(),
                now,
            ),
        )
        assertThrows(InvalidTransitScheduleException::class.java) {
            val tooLate = boundary.plusMinutes(1)
            resolveTransitAnchor(
                TransitTimeMode.ARRIVE_BY,
                tooLate.toLocalDate(),
                tooLate.toLocalTime(),
                now,
            )
        }
    }
}
