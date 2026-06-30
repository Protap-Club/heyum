package com.pgstorm148.heyum.db

import android.content.Context
import androidx.room.*
import com.pgstorm148.heyum.engine.MealLogRepository
import com.pgstorm148.heyum.model.MealLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "meal_logs")
data class MealLogEntity(
    @PrimaryKey val id: String,
    val dishId: String,
    val dishName: String,
    val cuisine: String,
    val mealType: String,
    val timestamp: Long,
    val accepted: Boolean,
    val rating: String?
)

@Dao
interface MealLogDao {
    @Query("SELECT * FROM meal_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_logs WHERE date(timestamp/1000, 'unixepoch', 'localtime') = date('now', 'localtime') ORDER BY timestamp DESC")
    fun getTodayLogs(): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_logs WHERE date(timestamp/1000, 'unixepoch', 'localtime') = date(:dateString) ORDER BY timestamp DESC")
    fun getLogsByDate(dateString: String): Flow<List<MealLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MealLogEntity)

    @Delete
    suspend fun delete(entry: MealLogEntity)

    @Query("UPDATE meal_logs SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: String, rating: String)
}

@Database(entities = [MealLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealLogDao(): MealLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "heyum_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class AndroidMealLogRepository(private val context: Context) : MealLogRepository {
    private val dao = AppDatabase.getDatabase(context).mealLogDao()

    override fun getAllLogs(): Flow<List<MealLogEntry>> = dao.getAllLogs().map { list -> list.map { it.toEntry() } }

    override fun getTodayLogs(): Flow<List<MealLogEntry>> = dao.getTodayLogs().map { list -> list.map { it.toEntry() } }

    override fun getLogsByDate(dateString: String): Flow<List<MealLogEntry>> = dao.getLogsByDate(dateString).map { list -> list.map { it.toEntry() } }

    override suspend fun insert(entry: MealLogEntry) {
        dao.insert(entry.toEntity())
    }

    override suspend fun delete(entry: MealLogEntry) {
        dao.delete(entry.toEntity())
    }

    override suspend fun updateRating(id: String, rating: String) {
        dao.updateRating(id, rating)
    }

    private fun MealLogEntity.toEntry() = MealLogEntry(
        id = id,
        dishId = dishId,
        dishName = dishName,
        cuisine = cuisine,
        mealType = mealType,
        timestamp = timestamp,
        accepted = accepted,
        rating = rating
    )

    private fun MealLogEntry.toEntity() = MealLogEntity(
        id = id.ifBlank { java.util.UUID.randomUUID().toString() },
        dishId = dishId,
        dishName = dishName,
        cuisine = cuisine,
        mealType = mealType,
        timestamp = timestamp,
        accepted = accepted,
        rating = rating
    )
}
