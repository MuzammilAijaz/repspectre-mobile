/* =====================================================================================
 * |                    Repository to Manage Acceleration Values
 * | -----------------------------------------------------------------------------------
 * | Communicates with the DAO to retrieve values and return to (ex. ViewModels)
 * |
 * ===================================================================================== */

package com.example.esp32_mpu6050_mobile_data_collection.data

import kotlinx.coroutines.flow.Flow

class AccelerationRepository(
    private val accelerationDao: AccelerationDao
) {
    fun getAllItemsStream(): Flow<List<AccelerationEntity>> = accelerationDao.getAllItems()
    fun getItem(id: Int): Flow<AccelerationEntity> = accelerationDao.getItem(id)

    private var newSession: Boolean = false
    private var sessionId: Long = 0

    suspend fun insertItem(createEntityFromSession: (sessionId: Long) -> AccelerationEntity) {
        // Session Id remains the same
        if (newSession) {
            sessionId = accelerationDao.insertSessionItem(AccelerationSessionEntity(startTime = System.currentTimeMillis()))
            newSession = false
        }

        accelerationDao.insertItem(item = createEntityFromSession(sessionId))
    }

    /** Allows the creation of new session*/
    fun createNewSession() {
        newSession = true
    }

    /** Updates the endTime of the latest session entity */
    suspend fun stopOldSession() {
        accelerationDao.updateSessionEndTime(accelerationDao.getLatestSessionId(), System.currentTimeMillis())
    }

    suspend fun cleanDatabase() = accelerationDao.cleanDatabase()
}