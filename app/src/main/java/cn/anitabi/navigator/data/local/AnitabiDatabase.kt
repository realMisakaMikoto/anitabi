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

@Entity(tableName = "pilgrimage_cache")
data class PilgrimageCacheEntity(
    @PrimaryKey val subjectId: Long,
    val payloadJson: String,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "tour_plans")
data class TourPlanEntity(
    @PrimaryKey val id: String,
    val planJson: String,
    val progressJson: String?,
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TourPlanEntity)
}

@Database(
    entities = [PilgrimageCacheEntity::class, TourPlanEntity::class],
    version = 1,
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
        ).build()
    }
}
