package com.example.esp32_mpu6050_mobile_data_collection.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.RawDataEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RawDataDao {

    // =====================================================================================
    // |                             Database Cleanup
    // =====================================================================================

    /** Deletes all rows from both acceleration and session tables */
    @Query("DELETE FROM rawdata")
    suspend fun deleteAllAcceleration()

    @Query("DELETE FROM rawdata")
    suspend fun deleteAllSessions()

    /** Resets AUTOINCREMENT counters inside sqlite for both tables */
    @Query("DELETE FROM sqlite_sequence WHERE name IN ('rawdata', 'session')")
    suspend fun resetAutoIncrement()

    /** Convenience method to clear both tables */
    suspend fun cleanDatabase() {
        deleteAllAcceleration()
        deleteAllSessions()
        resetAutoIncrement()
    }

    // =====================================================================================
    // |                               Raw Data Session Entity
    // =====================================================================================

    /** Returns the sessionId so it can be used to create [RawDataEntity] */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionItem(item: SessionEntity): Long // Note: using long here to align with how sqlite stores data

    @Query("UPDATE session SET endTime = :endTime WHERE sessionId = :sessionId")
    suspend fun updateSessionEndTime(sessionId: Long, endTime: Long)

    @Query("SELECT sessionId FROM session ORDER BY sessionId DESC LIMIT 1")
    suspend fun getLatestSessionId(): Long

    // =====================================================================================
    // |                                 Raw Data Entity
    // =====================================================================================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: RawDataEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemBatch(items: List<RawDataEntity>)

    @Update()
    suspend fun updateItem(item: RawDataEntity)

    @Delete
    suspend fun deleteItem(item: RawDataEntity)

    @Query("SELECT * FROM rawdata WHERE id = :id")
    fun getItem(id: Int): Flow<RawDataEntity>

    @Query("SELECT * FROM rawdata")
    fun getAllItems(): Flow<List<RawDataEntity>>
}
