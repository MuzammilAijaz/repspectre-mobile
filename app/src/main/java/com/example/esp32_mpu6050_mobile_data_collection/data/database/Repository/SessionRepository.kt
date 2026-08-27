package com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository

import com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO.BaseSessionDao
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.LiftContextEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.SessionEntity
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.AppViewModel
import kotlinx.coroutines.flow.Flow

open class SessionRepository<T>(
    private val dao: BaseSessionDao<T>
) {
    fun getAllItemsStream(): Flow<List<T>> = dao.getAllItems()
    fun getItem(id: Int): Flow<T> = dao.getItem(id)

    private var newSession: Boolean = false
    private var sessionId: Long = 0
    private var session: Session? = null

    suspend fun insertItem(createEntityFromSession: (sessionId: Long) -> T) {
        val currentSession = session ?: throw IllegalStateException("Session must be set before inserting items")

        if (newSession) {
            val config = currentSession.sessionConfig
            sessionId = dao.insertSessionItem(
                SessionEntity(
                    startTime = System.currentTimeMillis(),
                    motionState = config.motionState.name,
                    sensorDataFormat = config.sensorDataFormat.name
                )
            )

            if (config is AppViewModel.SessionConfig.LiftSession) {
                dao.insertLiftContextItem(
                    LiftContextEntity(
                        sessionId = sessionId.toLong(),
                        liftCategory = config.liftCategory.name,
                        tempo = config.tempo.name,
                        rpe = config.rpe
                    )
                )
            }
            newSession = false
        }
        dao.insertItem(createEntityFromSession(sessionId))
    }

    suspend fun insertItemBatch(createEntityListFromSession: (sessionId: Long) -> List<T>) {
        val currentSession = session ?: throw IllegalStateException("Session must be set before inserting items")

        if (newSession) {
            val config = currentSession.sessionConfig
            sessionId = dao.insertSessionItem(
                SessionEntity(
                    startTime = System.currentTimeMillis(),
                    motionState = config.motionState.name,
                    sensorDataFormat = config.sensorDataFormat.name
                )
            )

            if (config is AppViewModel.SessionConfig.LiftSession) {
                dao.insertLiftContextItem(
                    LiftContextEntity(
                        sessionId = sessionId.toLong(),
                        liftCategory = config.liftCategory.name,
                        tempo = config.tempo.name,
                        rpe = config.rpe
                    )
                )
            }
            newSession = false
        }
        dao.insertItemBatch(createEntityListFromSession(sessionId))
    }

    fun createNewSession(sessionConfig: AppViewModel.SessionConfig) {
        session = Session(sessionConfig)
        newSession = true
    }

    suspend fun deleteLatestSession() {
        val latestId = dao.getLatestSessionId()
        dao.deleteSession(latestId)
    }

    suspend fun stopOldSession() {
        val latestId = dao.getLatestSessionId()
        dao.updateSessionEndTime(latestId, System.currentTimeMillis())
    }

    suspend fun cleanDatabase() = dao.cleanDatabase()
}
