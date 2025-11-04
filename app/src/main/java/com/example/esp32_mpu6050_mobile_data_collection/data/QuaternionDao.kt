package com.example.esp32_mpu6050_mobile_data_collection.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.esp32_mpu6050_mobile_data_collection.data.entity.AccelerationSessionEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.entity.QuaternionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuaternionDao {

    // =====================================================================================
    // |                             Database Cleanup
    // =====================================================================================

    /** Deletes all rows from both acceleration and session tables */
    @Query("DELETE FROM quaternion")
    suspend fun deleteAllAcceleration()

    @Query("DELETE FROM quaternion")
    suspend fun deleteAllSessions()

    /** Resets AUTOINCREMENT counters inside sqlite for both tables */
    @Query("DELETE FROM sqlite_sequence WHERE name IN ('quaternion', 'session')")
    suspend fun resetAutoIncrement()

    /** Convenience method to clear both tables */
    suspend fun cleanDatabase() {
        deleteAllAcceleration()
        deleteAllSessions()
        resetAutoIncrement()
    }

    // =====================================================================================
    // |                                 Quaternion Session Entity
    // =====================================================================================

    /** Returns the sessionId so it can be used to create [com.example.esp32_mpu6050_mobile_data_collection.data.entity.QuaternionEntity] */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionItem(item: AccelerationSessionEntity): Long // Note: using long here to align with how sqlite stores data

    @Query("UPDATE session SET endTime = :endTime WHERE sessionId = :sessionId")
    suspend fun updateSessionEndTime(sessionId: Long, endTime: Long)

    @Query("SELECT sessionId FROM session ORDER BY sessionId DESC LIMIT 1")
    suspend fun getLatestSessionId(): Long

    // =====================================================================================
    // |                                Quaternion Entity
    // =====================================================================================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: QuaternionEntity)

    @Update()
    suspend fun updateItem(item: QuaternionEntity)

    @Delete
    suspend fun deleteItem(item: QuaternionEntity)

    @Query("SELECT * FROM quaternion WHERE id = :id")
    fun getItem(id: Int): Flow<QuaternionEntity>

    @Query("SELECT * FROM quaternion")
    fun getAllItems(): Flow<List<QuaternionEntity>>
}
