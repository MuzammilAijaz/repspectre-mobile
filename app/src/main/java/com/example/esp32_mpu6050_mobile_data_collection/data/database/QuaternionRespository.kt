package com.example.esp32_mpu6050_mobile_data_collection.data.database

import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.QuaternionEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.SessionEntity
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.AppViewModel
import kotlinx.coroutines.flow.Flow

class QuaternionRepository(
    private val quaternionDao: QuaternionDao
) {
    fun getAllItemsStream(): Flow<List<QuaternionEntity>> = quaternionDao.getAllItems()
    fun getItem(id: Int): Flow<QuaternionEntity> = quaternionDao.getItem(id)

    private var newSession: Boolean = false
    private var sessionId: Long = 0

    private var session: Session? = null // holds information for the whole session

    suspend fun insertItem(createEntityFromSession: (sessionId: Long) -> QuaternionEntity) {
        val currentSession = session ?: return

        // Session Id remains the same
        if (newSession) {
            sessionId = quaternionDao.insertSessionItem(SessionEntity(
                startTime = System.currentTimeMillis(),
                speedVariation = currentSession.sessionData.variation.speed.name,
                rpe = currentSession.sessionData.variation.rpe,
                noise = currentSession.sessionData.noiseCategory?.name,
                liftCategory = currentSession.sessionData.liftCategory.name))

            newSession = false
        }

        quaternionDao.insertItem(item = createEntityFromSession(sessionId))
    }

    suspend fun insertItemBatch(createEntityListFromSession: (Long) -> List<QuaternionEntity>) {
        val currentSession = session ?: return

        // Session Id remains the same
        if (newSession) {
            sessionId = quaternionDao.insertSessionItem(SessionEntity(
                startTime = System.currentTimeMillis(),
                speedVariation = currentSession.sessionData.variation.speed.name,
                rpe = currentSession.sessionData.variation.rpe,
                noise = currentSession.sessionData.noiseCategory?.name,
                liftCategory = currentSession.sessionData.liftCategory.name))

            newSession = false
        }

        quaternionDao.insertItemBatch(items = createEntityListFromSession(sessionId))
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
        quaternionDao.updateSessionEndTime(quaternionDao.getLatestSessionId(), System.currentTimeMillis())
    }

    suspend fun cleanDatabase() = quaternionDao.cleanDatabase()
}


// =====================================================================================
// |                                   Classes
// -------------------------------------------------------------------------------------

data class Session(
    val sessionData: AppViewModel.SessionData // contains the session information like what variation, type of lift it is.
)

