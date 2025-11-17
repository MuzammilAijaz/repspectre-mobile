package com.example.esp32_mpu6050_mobile_data_collection.data.database

import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.SessionEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.QuaternionEntity
import kotlinx.coroutines.flow.Flow

class QuaternionRepository(
    private val quaternionDao: QuaternionDao
) {
    fun getAllItemsStream(): Flow<List<QuaternionEntity>> = quaternionDao.getAllItems()
    fun getItem(id: Int): Flow<QuaternionEntity> = quaternionDao.getItem(id)

    private var newSession: Boolean = false
    private var sessionId: Long = 0

    suspend fun insertItem(createEntityFromSession: (sessionId: Long) -> QuaternionEntity) {
        // Session Id remains the same
        if (newSession) {
            sessionId = quaternionDao.insertSessionItem(SessionEntity(startTime = System.currentTimeMillis()))
            newSession = false
        }

        quaternionDao.insertItem(item = createEntityFromSession(sessionId))
    }

    suspend fun insertItemBatch(createEntityListFromSession: (Long) -> List<QuaternionEntity>) {
        // Session Id remains the same
        if (newSession) {
            sessionId = quaternionDao.insertSessionItem(SessionEntity(startTime = System.currentTimeMillis()))
            newSession = false
        }

        quaternionDao.insertItemBatch(items = createEntityListFromSession(sessionId))
    }

    /** Allows the creation of new session*/
    fun createNewSession() {
        newSession = true
    }

    /** Updates the endTime of the latest session entity */
    suspend fun stopOldSession() {
        quaternionDao.updateSessionEndTime(quaternionDao.getLatestSessionId(), System.currentTimeMillis())
    }

    suspend fun cleanDatabase() = quaternionDao.cleanDatabase()
}
