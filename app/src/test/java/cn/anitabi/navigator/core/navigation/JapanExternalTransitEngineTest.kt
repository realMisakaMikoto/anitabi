package cn.anitabi.navigator.core.navigation

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TransitExecutionStrategy
import cn.anitabi.navigator.core.model.TravelMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JapanExternalTransitEngineTest {
    @Test
    fun `qualified proximity accumulates for fifteen seconds and freezes in hysteresis band`() {
        val plan = plan(pointCount = 1, dwellMinutes = 0)
        val engine = JapanExternalTransitEngine(plan)
        val target = plan.legs.single().to
        engine.start()

        engine.onLocation(sample(target, distanceMeters = 79.0, elapsedRealtimeMillis = 0L))
        engine.onLocation(sample(target, distanceMeters = 79.0, elapsedRealtimeMillis = 5_000L))
        val frozen = engine.onLocation(
            sample(target, distanceMeters = 100.0, elapsedRealtimeMillis = 8_000L),
        )

        assertEquals(NavigationState.NAVIGATING, frozen.progress.state)
        assertEquals(5_000L, frozen.runtimeState.arrivalCandidateAccumulatedMillis)

        engine.onLocation(sample(target, distanceMeters = 79.0, elapsedRealtimeMillis = 13_000L))
        engine.onLocation(sample(target, distanceMeters = 79.0, elapsedRealtimeMillis = 18_000L))
        val arriving = engine.onLocation(
            sample(target, distanceMeters = 79.0, elapsedRealtimeMillis = 23_000L),
        )

        assertEquals(NavigationState.ARRIVING, arriving.progress.state)
        assertEquals(15_000L, arriving.runtimeState.arrivalCandidateAccumulatedMillis)
        assertEquals(0L, arriving.runtimeState.arrivalCandidateSinceElapsedRealtimeMillis)
        assertEquals(
            NavigationState.ARRIVING,
            engine.onTick(nowEpochMillis = 500_000L, nowElapsedRealtimeMillis = 500_000L).progress.state,
        )
    }

    @Test
    fun `inaccurate samples never count and a qualified gap over five seconds resets candidate`() {
        val plan = plan(pointCount = 1, dwellMinutes = 0)
        val engine = JapanExternalTransitEngine(plan)
        val target = plan.legs.single().to
        engine.start()

        engine.onLocation(sample(target, 10.0, 0L, accuracyMeters = 50.0))
        engine.onLocation(sample(target, 10.0, 5_000L, accuracyMeters = 50.0))
        engine.onLocation(sample(target, 10.0, 7_000L, accuracyMeters = 50.1))

        assertEquals(5_000L, engine.runtimeState.arrivalCandidateAccumulatedMillis)
        engine.onTick(nowEpochMillis = 10_000L, nowElapsedRealtimeMillis = 10_000L)
        assertEquals(5_000L, engine.runtimeState.arrivalCandidateAccumulatedMillis)

        val reset = engine.onTick(nowEpochMillis = 10_001L, nowElapsedRealtimeMillis = 10_001L)
        assertNull(reset.runtimeState.arrivalCandidateSinceElapsedRealtimeMillis)
        assertEquals(0L, reset.runtimeState.arrivalCandidateAccumulatedMillis)
        assertEquals(NavigationState.NAVIGATING, reset.progress.state)
    }

    @Test
    fun `distance over reset radius discards candidate while hysteresis distance preserves it`() {
        val plan = plan(pointCount = 1, dwellMinutes = 0)
        val engine = JapanExternalTransitEngine(plan)
        val target = plan.legs.single().to
        engine.start()

        engine.onLocation(sample(target, 20.0, 0L))
        engine.onLocation(sample(target, 20.0, 5_000L))
        val frozen = engine.onLocation(sample(target, 119.0, 8_000L))
        assertEquals(5_000L, frozen.runtimeState.arrivalCandidateAccumulatedMillis)

        val reset = engine.onLocation(sample(target, 121.0, 10_000L))
        assertNull(reset.runtimeState.arrivalCandidateSinceElapsedRealtimeMillis)
        assertEquals(0L, reset.runtimeState.arrivalCandidateAccumulatedMillis)
        assertEquals(121.0, requireNotNull(reset.runtimeState.targetDistanceMeters), 0.01)
    }

    @Test
    fun `early manual arrival requires confirmation and forced confirmation starts dwell`() {
        val plan = plan(pointCount = 1, dwellMinutes = 2)
        val engine = JapanExternalTransitEngine(plan)
        engine.start()

        val warning = engine.confirmArrival(nowEpochMillis = 1_000L)
        assertTrue(warning.requiresEarlyArrivalConfirmation)
        assertEquals(NavigationState.NAVIGATING, warning.progress.state)
        assertTrue(warning.progress.completedPointIds.isEmpty())

        val confirmed = engine.confirmArrival(nowEpochMillis = 1_000L, confirmEarly = true)
        assertFalse(confirmed.requiresEarlyArrivalConfirmation)
        assertEquals(NavigationState.DWELLING, confirmed.progress.state)
        assertEquals(setOf("point-0"), confirmed.progress.completedPointIds)
        assertEquals(121_000L, confirmed.progress.dwellingUntilEpochMillis)
    }

    @Test
    fun `arriving waits for explicit confirmation`() {
        val plan = plan(pointCount = 1, dwellMinutes = 0)
        val engine = JapanExternalTransitEngine(plan)
        val target = plan.legs.single().to
        engine.start()
        approachForFifteenSeconds(engine, target)

        assertEquals(NavigationState.ARRIVING, engine.progress.state)
        assertEquals(
            NavigationState.ARRIVING,
            engine.onTick(nowEpochMillis = 60_000L, nowElapsedRealtimeMillis = 60_000L).progress.state,
        )

        val confirmed = engine.confirmArrival(nowEpochMillis = 60_000L)
        assertFalse(confirmed.requiresEarlyArrivalConfirmation)
        assertEquals(NavigationState.DWELLING, confirmed.progress.state)
        assertEquals(setOf("point-0"), confirmed.progress.completedPointIds)
        assertEquals(
            NavigationState.COMPLETED,
            engine.onTick(nowEpochMillis = 60_000L, nowElapsedRealtimeMillis = 60_000L).progress.state,
        )
    }

    @Test
    fun `pause freezes dwell and resume shifts its deadline`() {
        val plan = plan(pointCount = 1, dwellMinutes = 1)
        val engine = JapanExternalTransitEngine(plan)
        engine.start()
        engine.confirmArrival(nowEpochMillis = 1_000L, confirmEarly = true)

        val paused = engine.pause(nowEpochMillis = 11_000L)
        assertTrue(paused.progress.isPaused)
        assertEquals(11_000L, paused.progress.pausedAtEpochMillis)
        assertEquals(
            NavigationState.DWELLING,
            engine.onTick(nowEpochMillis = 200_000L, nowElapsedRealtimeMillis = 200_000L).progress.state,
        )

        val resumed = engine.resume(nowEpochMillis = 31_000L)
        assertFalse(resumed.progress.isPaused)
        assertNull(resumed.progress.pausedAtEpochMillis)
        assertEquals(81_000L, resumed.progress.dwellingUntilEpochMillis)
        assertEquals(
            NavigationState.DWELLING,
            engine.onTick(nowEpochMillis = 80_999L, nowElapsedRealtimeMillis = 80_999L).progress.state,
        )
        assertEquals(
            NavigationState.COMPLETED,
            engine.onTick(nowEpochMillis = 81_000L, nowElapsedRealtimeMillis = 81_000L).progress.state,
        )
    }

    @Test
    fun `pause interrupts proximity continuity`() {
        val plan = plan(pointCount = 1, dwellMinutes = 0)
        val engine = JapanExternalTransitEngine(plan)
        val target = plan.legs.single().to
        engine.start()
        engine.onLocation(sample(target, 10.0, 0L))
        engine.onLocation(sample(target, 10.0, 5_000L))

        engine.pause(nowEpochMillis = 5_000L)
        engine.onLocation(sample(target, 10.0, 30_000L))
        engine.resume(nowEpochMillis = 30_000L)
        val resumedSample = engine.onLocation(sample(target, 10.0, 30_000L))

        assertEquals(NavigationState.NAVIGATING, resumedSample.progress.state)
        assertEquals(0L, resumedSample.runtimeState.arrivalCandidateAccumulatedMillis)
        assertEquals(30_000L, resumedSample.runtimeState.arrivalCandidateSinceElapsedRealtimeMillis)
    }

    @Test
    fun `next leg advances only after explicit action`() {
        val plan = plan(pointCount = 2, dwellMinutes = 0)
        val engine = JapanExternalTransitEngine(plan)
        engine.start()
        engine.confirmArrival(nowEpochMillis = 0L, confirmEarly = true)
        engine.onTick(nowEpochMillis = 0L, nowElapsedRealtimeMillis = 0L)

        assertEquals(NavigationState.NEXT_STOP, engine.progress.state)
        assertEquals(
            NavigationState.NEXT_STOP,
            engine.onTick(nowEpochMillis = 1_000_000L, nowElapsedRealtimeMillis = 1_000_000L).progress.state,
        )

        val next = engine.startNextLeg()
        assertEquals(NavigationState.NAVIGATING, next.progress.state)
        assertEquals(1, next.progress.legIndex)
        assertNull(next.runtimeState.targetDistanceMeters)
    }

    @Test
    fun `leaving dwell early advances and starts the next leg in one user action`() {
        val plan = plan(pointCount = 2, dwellMinutes = 15)
        val engine = JapanExternalTransitEngine(plan)
        engine.start()
        val dwelling = engine.confirmArrival(nowEpochMillis = 1_000L, confirmEarly = true)

        assertEquals(NavigationState.DWELLING, dwelling.progress.state)
        val next = engine.leaveDwellEarlyAndStartNextLeg()

        assertEquals(NavigationState.NAVIGATING, next.progress.state)
        assertEquals(1, next.progress.legIndex)
        assertNull(next.progress.dwellingUntilEpochMillis)
        assertEquals(setOf("point-0"), next.progress.completedPointIds)
        assertEquals(next, engine.leaveDwellEarlyAndStartNextLeg())
    }

    @Test
    fun `leaving the final dwell early completes without inventing another leg`() {
        val plan = plan(pointCount = 1, dwellMinutes = 15)
        val engine = JapanExternalTransitEngine(plan)
        engine.start()
        engine.confirmArrival(nowEpochMillis = 1_000L, confirmEarly = true)

        val completed = engine.leaveDwellEarlyAndStartNextLeg()

        assertEquals(NavigationState.COMPLETED, completed.progress.state)
        assertEquals(0, completed.progress.legIndex)
        assertNull(completed.progress.dwellingUntilEpochMillis)
        assertEquals(setOf("point-0"), completed.progress.completedPointIds)
    }

    @Test
    fun `leaving dwell early respects pause and can start the return leg`() {
        val plan = plan(pointCount = 1, dwellMinutes = 15, returnToStart = true)
        val engine = JapanExternalTransitEngine(plan)
        engine.start()
        engine.confirmArrival(nowEpochMillis = 1_000L, confirmEarly = true)
        val paused = engine.pause(nowEpochMillis = 2_000L)

        assertEquals(paused, engine.leaveDwellEarlyAndStartNextLeg())

        engine.resume(nowEpochMillis = 3_000L)
        val returning = engine.leaveDwellEarlyAndStartNextLeg()
        assertEquals(NavigationState.NAVIGATING, returning.progress.state)
        assertEquals(1, returning.progress.legIndex)
        assertEquals(setOf("point-0"), returning.progress.completedPointIds)
    }

    @Test
    fun `return-to-start leg completes without adding a pilgrimage point`() {
        val plan = plan(pointCount = 1, dwellMinutes = 0, returnToStart = true)
        val engine = JapanExternalTransitEngine(plan)
        engine.start()
        engine.confirmArrival(nowEpochMillis = 0L, confirmEarly = true)
        engine.onTick(nowEpochMillis = 0L, nowElapsedRealtimeMillis = 0L)
        engine.startNextLeg()

        assertEquals(1, engine.progress.legIndex)
        assertEquals(NavigationState.NAVIGATING, engine.progress.state)
        val completed = engine.confirmArrival(nowEpochMillis = 1_000L, confirmEarly = true)

        assertEquals(NavigationState.COMPLETED, completed.progress.state)
        assertEquals(setOf("point-0"), completed.progress.completedPointIds)
    }

    @Test
    fun `ending is terminal and preserves completed progress`() {
        val plan = plan(pointCount = 2, dwellMinutes = 0)
        val engine = JapanExternalTransitEngine(plan)
        engine.start()
        engine.confirmArrival(nowEpochMillis = 0L, confirmEarly = true)
        engine.onTick(nowEpochMillis = 0L, nowElapsedRealtimeMillis = 0L)

        val ended = engine.end()
        assertEquals(NavigationState.ENDED, ended.progress.state)
        assertEquals(setOf("point-0"), ended.progress.completedPointIds)
        assertFalse(ended.progress.isPaused)
        assertEquals(
            NavigationState.ENDED,
            engine.startNextLeg().progress.state,
        )
        assertEquals(
            NavigationState.ENDED,
            engine.onLocation(sample(plan.legs[1].to, 0.0, 20_000L)).progress.state,
        )
    }

    @Test
    fun `five legs complete through explicit confirmations and advances`() {
        val plan = plan(pointCount = 5, dwellMinutes = 0)
        val engine = JapanExternalTransitEngine(plan)
        engine.start()

        repeat(5) { index ->
            val arrivalTime = index.toLong()
            val confirmed = engine.confirmArrival(nowEpochMillis = arrivalTime, confirmEarly = true)
            assertEquals(NavigationState.DWELLING, confirmed.progress.state)
            val afterDwell = engine.onTick(
                nowEpochMillis = arrivalTime,
                nowElapsedRealtimeMillis = arrivalTime,
            )
            if (index < 4) {
                assertEquals(NavigationState.NEXT_STOP, afterDwell.progress.state)
                assertEquals(NavigationState.NAVIGATING, engine.startNextLeg().progress.state)
            } else {
                assertEquals(NavigationState.COMPLETED, afterDwell.progress.state)
            }
        }

        assertEquals(5, engine.progress.completedPointIds.size)
        assertEquals(4, engine.progress.legIndex)
    }

    @Test
    fun `arrival candidate runtime is not restored from navigation progress`() {
        val plan = plan(pointCount = 1, dwellMinutes = 0)
        val firstEngine = JapanExternalTransitEngine(plan)
        val target = plan.legs.single().to
        firstEngine.start()
        firstEngine.onLocation(sample(target, 10.0, 0L))
        firstEngine.onLocation(sample(target, 10.0, 5_000L))
        assertEquals(5_000L, firstEngine.runtimeState.arrivalCandidateAccumulatedMillis)

        val restored = JapanExternalTransitEngine(plan, firstEngine.progress)
        assertNull(restored.runtimeState.targetDistanceMeters)
        assertNull(restored.runtimeState.arrivalCandidateSinceElapsedRealtimeMillis)
        assertEquals(0L, restored.runtimeState.arrivalCandidateAccumulatedMillis)
    }

    private fun approachForFifteenSeconds(engine: JapanExternalTransitEngine, target: GeoPoint) {
        listOf(0L, 5_000L, 10_000L, 15_000L).forEach { elapsed ->
            engine.onLocation(sample(target, distanceMeters = 10.0, elapsedRealtimeMillis = elapsed))
        }
    }

    private fun sample(
        target: GeoPoint,
        distanceMeters: Double,
        elapsedRealtimeMillis: Long,
        accuracyMeters: Double = 10.0,
    ): JapanTransitLocationSample = JapanTransitLocationSample(
        coordinate = GeoPoint(
            latitude = target.latitude + Math.toDegrees(distanceMeters / EARTH_RADIUS_METERS),
            longitude = target.longitude,
        ),
        accuracyMeters = accuracyMeters,
        elapsedRealtimeMillis = elapsedRealtimeMillis,
    )

    private fun plan(
        pointCount: Int,
        dwellMinutes: Int,
        returnToStart: Boolean = false,
    ): TourPlan {
        val start = GeoPoint(latitude = 0.0, longitude = 0.0)
        val points = List(pointCount) { index ->
            PilgrimagePoint(
                id = "point-$index",
                name = "Point $index",
                coordinate = GeoPoint(latitude = 0.01 * (index + 1), longitude = 0.0),
            )
        }
        val legs = buildList {
            var from = start
            points.forEach { point ->
                add(leg(from, point.coordinate, point.id))
                from = point.coordinate
            }
            if (returnToStart) add(leg(from, start, destinationPointId = null))
        }
        return TourPlan(
            id = "japan-tour-$pointCount-$returnToStart",
            anime = Anime(subjectId = 1L, name = "Fixture"),
            selectedPoints = points,
            orderedPoints = points,
            legs = legs,
            mode = TravelMode.TRANSIT,
            objective = RouteObjective.FASTEST,
            endPolicy = if (returnToStart) EndPolicy.RETURN_TO_START else EndPolicy.OPEN,
            estimatedDurationSeconds = 0.0,
            attribution = emptyList(),
            dwellMinutes = dwellMinutes,
            initialStart = start,
            executionStrategy = TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN,
        )
    }

    private fun leg(from: GeoPoint, to: GeoPoint, destinationPointId: String?): TourLeg = TourLeg(
        from = from,
        to = to,
        mode = TravelMode.TRANSIT,
        geometry = emptyList(),
        steps = emptyList(),
        distanceMeters = 0.0,
        durationSeconds = 0.0,
        source = "LOCAL_JAPAN_EXTERNAL_TRANSIT",
        destinationPointId = destinationPointId,
    )

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
