package com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO

import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.LiftContextEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

interface BaseSessionDao<T> {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: T)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertItemBatch(items: List<T>)

    @androidx.room.Update
    suspend fun updateItem(item: T)

    @androidx.room.Delete
    suspend fun deleteItem(item: T)

    fun getItem(id: Int): Flow<T>

    fun getAllItems(): Flow<List<T>>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertSessionItem(item: SessionEntity): Long

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertLiftContextItem(item: LiftContextEntity)

    @androidx.room.Query("SELECT MAX(sessionId) FROM session")
    suspend fun getLatestSessionId(): Long

    @androidx.room.Query("UPDATE session SET endTime = :endTime WHERE sessionId = :sessionId")
    suspend fun updateSessionEndTime(sessionId: Long, endTime: Long)

    @androidx.room.Query("DELETE FROM session")
    suspend fun deleteAllSessions()

    @androidx.room.Query("DELETE FROM session WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @androidx.room.Query("DELETE FROM sqlite_sequence WHERE name = 'session'")
    suspend fun resetAutoIncrementCounter()

    suspend fun cleanDatabase()
}
