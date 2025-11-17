package com.example.esp32_mpu6050_mobile_data_collection.data.database

import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.AccelerationEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.SessionEntity
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.AppViewModel
import kotlinx.coroutines.flow.Flow

class AccelerationRepository(
    private val accelerationDao: AccelerationDao
) {
    fun getAllItemsStream(): Flow<List<AccelerationEntity>> = accelerationDao.getAllItems()
    fun getItem(id: Int): Flow<AccelerationEntity> = accelerationDao.getItem(id)

    private var newSession: Boolean = false
    private var sessionId: Long = 0

    private var session: Session? = null // holds information for the whole session

    suspend fun insertItem(createEntityFromSession: (sessionId: Long) -> AccelerationEntity) {
       val currentSession = session ?: throw IllegalStateException("Session must be set before inserting items")

        // Session Id remains the same
        if (newSession) {
            sessionId = accelerationDao.insertSessionItem(SessionEntity(
                startTime = System.currentTimeMillis(),
                speedVariation = currentSession.sessionData.variation.speed.name,
                rpe = currentSession.sessionData.variation.rpe,
                noise = currentSession.sessionData.noise?.name,
                liftCategory = currentSession.sessionData.category.name))

            newSession = false
        }

        accelerationDao.insertItem(item = createEntityFromSession(sessionId))
    }

    /** Allows the creation of new session by simply setting the flag for the creation of a new
     * Session
     * @note: It does not actually start recording of values */
    fun createNewSession(sessionData: AppViewModel.SessionData) {
        session = Session(sessionData)
        newSession = true
    }

    /** Updates the endTime of the latest session entity */
    suspend fun stopOldSession() {
        accelerationDao.updateSessionEndTime(accelerationDao.getLatestSessionId(), System.currentTimeMillis())
    }

    suspend fun cleanDatabase() = accelerationDao.cleanDatabase()
}