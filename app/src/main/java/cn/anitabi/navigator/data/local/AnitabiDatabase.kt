package cn.anitabi.navigator.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "pilgrimage_cache")
data class PilgrimageCacheEntity(
    @PrimaryKey val subjectId: Long,
    val payloadJson: String,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "tour_plans")
data class TourPlanEntity(
    @PrimaryKey val id: String,
    val storedTourJson: String?,
    val legacyPlanJson: String?,
    val legacyProgressJson: String?,
    val migrationError: String?,
    val routeNeedsRefresh: Boolean,
    val updatedAtEpochMillis: Long,
)

@Dao
interface PilgrimageCacheDao {
    @Query("SELECT * FROM pilgrimage_cache WHERE subjectId = :subjectId")
    suspend fun get(subjectId: Long): PilgrimageCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PilgrimageCacheEntity)
}

@Dao
interface TourPlanDao {
    @Query("SELECT * FROM tour_plans WHERE id = :id")
    suspend fun get(id: String): TourPlanEntity?

    @Query("SELECT * FROM tour_plans ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun getMostRecent(): TourPlanEntity?

    @Query("SELECT id FROM tour_plans ORDER BY updatedAtEpochMillis DESC, id DESC")
    suspend fun getIdsMostRecentFirst(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TourPlanEntity)

    @Query(
        """UPDATE tour_plans
        SET storedTourJson = :storedTourJson,
            legacyPlanJson = NULL,
            legacyProgressJson = NULL,
            migrationError = NULL,
            routeNeedsRefresh = 1,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :id""",
    )
    suspend fun finishLegacyMigration(id: String, storedTourJson: String, updatedAtEpochMillis: Long)

    @Query("UPDATE tour_plans SET migrationError = :message WHERE id = :id")
    suspend fun recordMigrationError(id: String, message: String)

    @Query(
        "SELECT migrationError FROM tour_plans " +
            "WHERE migrationError IS NOT NULL ORDER BY updatedAtEpochMillis DESC LIMIT 1",
    )
    suspend fun getMostRecentMigrationError(): String?
}

@Database(
    entities = [PilgrimageCacheEntity::class, TourPlanEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AnitabiDatabase : RoomDatabase() {
    abstract fun pilgrimageCacheDao(): PilgrimageCacheDao
    abstract fun tourPlanDao(): TourPlanDao

    companion object {
        fun create(context: Context): AnitabiDatabase = Room.databaseBuilder(
            context.applicationContext,
            AnitabiDatabase::class.java,
            "anitabi.db",
        ).addMigrations(MIGRATION_1_2).build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `tour_plans_v2` (
                        `id` TEXT NOT NULL,
                        `storedTourJson` TEXT,
                        `legacyPlanJson` TEXT,
                        `legacyProgressJson` TEXT,
                        `migrationError` TEXT,
                        `routeNeedsRefresh` INTEGER NOT NULL,
                        `updatedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )""".trimIndent(),
                )
                db.execSQL(
                    """INSERT INTO `tour_plans_v2` (
                        `id`, `storedTourJson`, `legacyPlanJson`, `legacyProgressJson`,
                        `migrationError`, `routeNeedsRefresh`, `updatedAtEpochMillis`
                    )
                    SELECT `id`, NULL, `planJson`, `progressJson`, NULL, 1, `updatedAtEpochMillis`
                    FROM `tour_plans`""".trimIndent(),
                )
                db.execSQL("DROP TABLE `tour_plans`")
                db.execSQL("ALTER TABLE `tour_plans_v2` RENAME TO `tour_plans`")
            }
        }
    }
}
