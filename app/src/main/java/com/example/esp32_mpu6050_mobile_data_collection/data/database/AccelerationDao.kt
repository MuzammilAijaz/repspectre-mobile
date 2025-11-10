package com.example.esp32_mpu6050_mobile_data_collection.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.AccelerationEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.AccelerationSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccelerationDao {

    // =====================================================================================
    // |                             Database Cleanup
    // =====================================================================================

    /** Deletes all rows from both acceleration and session tables */
    @Query("DELETE FROM acceleration")
    suspend fun deleteAllAcceleration()

    @Query("DELETE FROM session")
    suspend fun deleteAllSessions()

    /** Resets AUTOINCREMENT counters inside sqlite for both tables */
    @Query("DELETE FROM sqlite_sequence WHERE name IN ('acceleration', 'session')")
    suspend fun resetAutoIncrement()

    /** Convenience method to clear both tables */
    suspend fun cleanDatabase() {
        deleteAllAcceleration()
        deleteAllSessions()
        resetAutoIncrement()
    }

    // =====================================================================================
    // |                                 Acceleration Session Entity
    // =====================================================================================

    /** Returns the sessionId so it can be used to create [AccelerationEntity] */
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertSessionItem(item: AccelerationSessionEntity): Long // Note: using long here to align with how sqlite stores data

    @Query("UPDATE session SET endTime = :endTime WHERE sessionId = :sessionId")
    suspend fun updateSessionEndTime(sessionId: Long, endTime: Long)

    @Query("SELECT sessionId FROM session ORDER BY sessionId DESC LIMIT 1")
    suspend fun getLatestSessionId(): Long

    // =====================================================================================
    // |                                Acceleration Entity
    // =====================================================================================

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertItem(item: AccelerationEntity)

    @Update()
    suspend fun updateItem(item: AccelerationEntity)

    @Delete
    suspend fun deleteItem(item: AccelerationEntity)

    @Query("SELECT * FROM acceleration WHERE id = :id")
    fun getItem(id: Int): Flow<AccelerationEntity>

    @Query("SELECT * FROM acceleration")
    fun getAllItems(): Flow<List<AccelerationEntity>>
}