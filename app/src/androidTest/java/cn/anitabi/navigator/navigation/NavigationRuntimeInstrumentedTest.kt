package cn.anitabi.navigator.navigation

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.anitabi.navigator.AnitabiApplication
import cn.anitabi.navigator.MainActivity
import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.RouteStep
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TravelMode
import java.io.FileInputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationRuntimeInstrumentedTest {
    private val application: AnitabiApplication
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun seedProcessRecoveryFixture() = runBlocking {
        prepareDevice()
        application.stopService(Intent(application, NavigationService::class.java))
        NavigationRuntime.set(NavigationRuntimeState())
        val plan = fixturePlan(RECOVERY_TOUR_ID)
        application.container.tourRepository.save(
            plan,
            NavigationProgress(tourId = plan.id, state = NavigationState.NAVIGATING),
        )
        val saved = application.container.tourRepository.get(plan.id)
        assertEquals(NavigationState.NAVIGATING, saved?.progress?.state)
        assertEquals(2, saved?.plan?.legs?.size)
    }

    @Test
    fun foregroundServiceCompletesOfflineRouteAndPersistsProgress() = runBlocking {
        prepareDevice()
        application.stopService(Intent(application, NavigationService::class.java))
        NavigationRuntime.set(NavigationRuntimeState())
        val plan = fixturePlan(SERVICE_TOUR_ID)
        application.container.tourRepository.save(
            plan,
            NavigationProgress(tourId = plan.id, state = NavigationState.NAVIGATING),
        )
        setAirplaneMode(true)
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            awaitCondition("foreground service did not resume the saved route") {
                NavigationRuntime.state.value.let { state ->
                    state.plan?.id == plan.id && state.isRunning &&
                        state.progress?.state == NavigationState.NAVIGATING
                }
            }
            assertEquals("1", shell("settings get global airplane_mode_on").trim())
            assertNotNull(activeNavigationNotification())

            sendServiceAction(NavigationService.ACTION_MANUAL_ARRIVAL)
            awaitCondition("navigation did not advance to the second leg") {
                NavigationRuntime.state.value.progress?.let { progress ->
                    progress.legIndex == 1 && progress.state == NavigationState.NAVIGATING &&
                        FIRST_STOP_ID in progress.completedPointIds
                } == true
            }

            sendServiceAction(NavigationService.ACTION_MANUAL_ARRIVAL)
            awaitCondition("navigation did not complete the second leg") {
                NavigationRuntime.state.value.progress?.state == NavigationState.COMPLETED
            }
            awaitCondition("completed progress was not persisted") {
                application.container.tourRepository.get(plan.id)?.progress?.state == NavigationState.COMPLETED
            }
            val completed = application.container.tourRepository.get(plan.id)?.progress
            assertEquals(setOf(START_ID, FIRST_STOP_ID, SECOND_STOP_ID), completed?.completedPointIds)
            assertTrue(NavigationRuntime.state.value.errorMessage == null)
        } finally {
            setAirplaneMode(false)
            application.stopService(Intent(application, NavigationService::class.java))
            scenario.close()
        }
    }

    private fun prepareDevice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = application.packageName
        shell("settings put secure location_mode 3")
        instrumentation.uiAutomation.grantRuntimePermission(
            packageName,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        instrumentation.uiAutomation.grantRuntimePermission(
            packageName,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(
                packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }

    private fun sendServiceAction(action: String) {
        application.startService(Intent(application, NavigationService::class.java).setAction(action))
    }

    private fun activeNavigationNotification(): Notification? {
        val manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return manager.activeNotifications
            .map { it.notification }
            .firstOrNull { notification ->
                notification.extras.getString(Notification.EXTRA_TITLE) == "巡礼手帖 · 连续导航" &&
                    notification.flags and Notification.FLAG_ONGOING_EVENT != 0
            }
    }

    private suspend fun awaitCondition(message: String, block: suspend () -> Boolean) {
        repeat(150) {
            if (block()) return
            delay(100L)
        }
        throw AssertionError(message)
    }

    private fun setAirplaneMode(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        shell("settings put global airplane_mode_on $value")
        shell("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled")
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return descriptor.use {
            FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText() }
        }
    }

    private fun fixturePlan(id: String): TourPlan {
        val start = PilgrimagePoint(
            id = START_ID,
            name = "Runtime Start",
            coordinate = GeoPoint(35.681236, 139.767125),
        )
        val first = PilgrimagePoint(
            id = FIRST_STOP_ID,
            name = "Runtime Stop A",
            coordinate = GeoPoint(35.681900, 139.768000),
        )
        val second = PilgrimagePoint(
            id = SECOND_STOP_ID,
            name = "Runtime Stop B",
            coordinate = GeoPoint(35.682500, 139.769000),
        )
        val points = listOf(start, first, second)
        val legs = points.zipWithNext().mapIndexed { index, (from, to) ->
            TourLeg(
                from = from.coordinate,
                to = to.coordinate,
                mode = TravelMode.WALK,
                geometry = listOf(from.coordinate, to.coordinate),
                steps = listOf(
                    RouteStep(
                        instruction = "Continue to ${to.name}",
                        distanceMeters = 120.0,
                        durationSeconds = 90.0,
                        coordinate = to.coordinate,
                    ),
                ),
                distanceMeters = 120.0,
                durationSeconds = 90.0,
                source = "Runtime fixture",
                destinationPointId = if (index == 0) FIRST_STOP_ID else SECOND_STOP_ID,
            )
        }
        return TourPlan(
            id = id,
            anime = Anime(subjectId = 1L, name = "Runtime Smoke Tour"),
            selectedPoints = points,
            orderedPoints = points,
            legs = legs,
            mode = TravelMode.WALK,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            estimatedDurationSeconds = 180.0,
            attribution = listOf("Runtime fixture"),
            dwellMinutes = 0,
            initialStart = start.coordinate,
        )
    }

    companion object {
        private const val RECOVERY_TOUR_ID = "runtime-recovery-fixture"
        private const val SERVICE_TOUR_ID = "runtime-service-fixture"
        private const val START_ID = "runtime-start"
        private const val FIRST_STOP_ID = "runtime-stop-a"
        private const val SECOND_STOP_ID = "runtime-stop-b"
    }
}
