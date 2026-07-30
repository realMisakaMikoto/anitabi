package cn.anitabi.navigator.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import cn.anitabi.navigator.data.network.ApiHttpClient
import cn.anitabi.navigator.data.repository.TourRepository
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnitabiDatabaseMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val json = ApiHttpClient.defaultJson

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, AnitabiDatabase::class.java)

    @Test
    @Throws(IOException::class)
    fun publicV020RecordMigratesWithoutRouteContentAndIsIdempotent() = runBlocking {
        val plan = v020Plan()
        val progress = NavigationProgress(
            tourId = plan.id,
            legIndex = 0,
            completedPointIds = setOf("101::a"),
            state = NavigationState.NAVIGATING,
        )
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                "INSERT INTO tour_plans (id, planJson, progressJson, updatedAtEpochMillis) VALUES (?, ?, ?, ?)",
                arrayOf<Any?>(
                    plan.id,
                    json.encodeToString(TourPlan.serializer(), plan),
                    json.encodeToString(NavigationProgress.serializer(), progress),
                    1000L,
                ),
            )
            close()
        }
        helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, AnitabiDatabase.MIGRATION_1_2).close()

        val database = openLatestDatabase()
        val repository = TourRepository(database.tourPlanDao(), json, now = { 2000L })
        val first = repository.get(plan.id)
        val second = repository.get(plan.id)

        assertNotNull(first)
        assertEquals(first?.storedTour, second?.storedTour)
        assertTrue(first?.routeNeedsRefresh == true)
        assertTrue(first?.plan?.legs?.isEmpty() == true)
        assertEquals(setOf("101::a"), first?.progress?.completedPointIds)
        database.openHelper.readableDatabase.query(
            "SELECT storedTourJson, legacyPlanJson, legacyProgressJson, migrationError FROM tour_plans WHERE id = ?",
            arrayOf(plan.id),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            val storedJson = cursor.getString(0)
            assertNotNull(storedJson)
            assertFalse(storedJson.contains("geometry"))
            assertFalse(storedJson.contains("steps"))
            assertNull(cursor.getString(1))
            assertNull(cursor.getString(2))
            assertNull(cursor.getString(3))
        }
        database.close()
    }

    @Test
    fun failedPayloadKeepsOriginalRecordAndStoresRecoveryMessage() = runBlocking {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                "INSERT INTO tour_plans (id, planJson, progressJson, updatedAtEpochMillis) VALUES (?, ?, NULL, ?)",
                arrayOf<Any?>("broken-tour", "{not-valid-json", 1000L),
            )
            close()
        }
        helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, AnitabiDatabase.MIGRATION_1_2).close()

        val database = openLatestDatabase()
        val repository = TourRepository(database.tourPlanDao(), json)
        assertNull(repository.get("broken-tour"))
        assertEquals(TourRepository.RECOVERY_ERROR_MESSAGE, repository.getMostRecentRecoveryError())
        database.openHelper.readableDatabase.query(
            "SELECT storedTourJson, legacyPlanJson, migrationError FROM tour_plans WHERE id = 'broken-tour'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertNull(cursor.getString(0))
            assertEquals("{not-valid-json", cursor.getString(1))
            assertEquals(TourRepository.RECOVERY_ERROR_MESSAGE, cursor.getString(2))
        }
        database.close()
    }

    private fun openLatestDatabase(): AnitabiDatabase = Room.databaseBuilder(
        instrumentation.targetContext,
        AnitabiDatabase::class.java,
        TEST_DATABASE,
    ).addMigrations(AnitabiDatabase.MIGRATION_1_2).build()

    private fun v020Plan(): TourPlan {
        val first = PilgrimagePoint("101::a", "《作品甲》· A", GeoPoint(35.0, 139.0))
        val second = PilgrimagePoint("202::b", "《作品乙》· B", GeoPoint(35.1, 139.1))
        return TourPlan(
            id = "public-v0.2.0-tour",
            anime = Anime(0, "作品甲 + 作品乙", "2 部作品联合巡礼"),
            selectedPoints = listOf(first, second),
            orderedPoints = listOf(first, second),
            legs = listOf(
                TourLeg(
                    from = first.coordinate,
                    to = second.coordinate,
                    mode = TravelMode.WALK,
                    geometry = listOf(first.coordinate, second.coordinate),
                    steps = listOf(RouteStep("旧路线步骤", 100.0, 60.0)),
                    distanceMeters = 100.0,
                    durationSeconds = 60.0,
                    source = "legacy provider",
                    destinationPointId = second.id,
                ),
            ),
            mode = TravelMode.WALK,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            estimatedDurationSeconds = 60.0,
            attribution = listOf("legacy attribution"),
            initialStart = first.coordinate,
        )
    }

    companion object {
        private const val TEST_DATABASE = "anitabi-v020-migration-test"
    }
}
