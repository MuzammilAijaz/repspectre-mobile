package com.example.esp32_mpu6050_mobile_data_collection.ui.view

import android.R.attr.x
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.esp32_mpu6050_mobile_data_collection.data.AppBleRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.SensorApplication
import com.example.esp32_mpu6050_mobile_data_collection.data.database.AccelerationRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.QuaternionRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.AccelerationEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.QuaternionEntity
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class AppViewModel(
    private val accelerationRepository: AccelerationRepository,
    private val quaternionRepository: QuaternionRepository,
    private val bleRepository: AppBleRepository
) : ViewModel() {


    /* This will be used as the query builder for the session type */
    data class SessionData(
        var category: LiftCategory = LiftCategory.FLOOR_PULL,
        var variation: Variation = Variation(rpe = 7, speed = Variation.SpeedVariation.CONTROLLED),
        var noise: NoiseType? = null,
        var dataFormat: SensorDataFormat = SensorDataFormat.ALL_RAW_VALUES,
    )

    private val _sessionDataState = MutableStateFlow(SessionData())

    // uiState for public access
    val sessionDataState: StateFlow<SessionData> = _sessionDataState.asStateFlow()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SensorApplication)
                val accelerationRepository = application.container.accelerationRepository
                val quaternionRepository = application.container.quaternionRepository
                val bleRepository = application.container.bleRepository
                AppViewModel(accelerationRepository, quaternionRepository, bleRepository)
            }
        }
    }

    private var messageList: MutableList<AppService.SensorData> = mutableListOf<AppService.SensorData>()

    private val lock = Any()
    private var isDataSaveModeOn = false

    // =====================================================================================
    // |                                 Private Functions
    // =====================================================================================

    private suspend fun updateAccelDatabaseValues(createEntityFromSession: (sessionId: Long) -> AccelerationEntity) {
        accelerationRepository.insertItem(createEntityFromSession = createEntityFromSession)
    }

    private suspend fun updateQuatDatabaseValues(createEntityFromSession: (sessionId: Long) -> QuaternionEntity) {
        quaternionRepository.insertItem(createEntityFromSession = createEntityFromSession)
    }

    private suspend fun updateQuatDatabaseValuesInBatch(createEntityListFromSession: (sessionId: Long) -> List<QuaternionEntity>) {
        quaternionRepository.insertItemBatch(createEntityListFromSession = createEntityListFromSession)
    }

    private fun insertQuaternionValueBatch(data: List<AppService.SensorData>) {
        // Coroutine scope launches and returns immediately, as its non-blocking
        viewModelScope.launch(Dispatchers.IO) {

            updateQuatDatabaseValuesInBatch() { sessionId ->
                data.map { item ->
                    Log.d("QuatData", "x: $x")
                    QuaternionEntity(sessionId = sessionId, x = item.x, y = item.y, z = item.z, w = item.w?:0.00f)
                }
            }
        }
    }

    // =====================================================================================
    // |                                 Public Functions
    // =====================================================================================

    public fun stopOldSession() {
        viewModelScope.launch(Dispatchers.IO) {
            quaternionRepository.stopOldSession()
            accelerationRepository.stopOldSession()
        }
    }

    public fun createNewSession() {
        // Simply sets the flag for the creation of a new session
        quaternionRepository.createNewSession(_sessionDataState.value)
        accelerationRepository.createNewSession(_sessionDataState.value)
    }

    fun setCategory(category: LiftCategory) {
        _sessionDataState.update { it.copy(category = category) }
    }

    fun setVariation(variation: Variation) {
        _sessionDataState.update { it.copy(variation = variation) }
    }

    fun setSensorDataFormat(format: SensorDataFormat) {
        _sessionDataState.update { it.copy(dataFormat = format) }
    }

    fun setNoise(noise: NoiseType?) {
        _sessionDataState.update { it.copy(noise = noise) }
    }

    public fun cleanDatabase() {
        viewModelScope.launch(Dispatchers.IO){
            quaternionRepository.cleanDatabase()
            accelerationRepository.cleanDatabase()
        }
    }

    public fun insertQuaternionValue(data: AppService.SensorData) {

        // Coroutine scope launches and returns immediately, as its non-blocking
        viewModelScope.launch(Dispatchers.IO) {

            updateQuatDatabaseValues() { sessionId ->
                QuaternionEntity(sessionId = sessionId, x = data.x, y = data.y, z = data.z, w = data.w?:0.00f)
            }
        }
    }

//    public fun insertAccelValue(data: AppService.SensorData) {
//
//        // Coroutine scope launches and returns immediately, as its non-blocking
//        viewModelScope.launch(Dispatchers.IO) {
//
//            updateAccelDatabaseValues() { sessionId ->
//                AccelerationEntity(sessionId = sessionId, x = data.x, y = data.y, z = data.z)
//            }
//        }
//    }

    public fun getAllQuaternions(): Flow<List<QuaternionEntity>> {
        return quaternionRepository.getAllItemsStream()
    }

    private var dataSaveJob: Job? = null

    /** This function is responsible for fetching the latest values from the repository
    * and appending the values to the messageList (the list will be stored in the database
    * AFTER data collection has been stopped */
    fun startDataSave() {
        if (dataSaveJob?.isActive == true) {
            Log.d("AppViewModel", "startDataSave: collector already running — ignoring duplicate start")
            return
        }

        isDataSaveModeOn = true

        dataSaveJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Collect BLE state updates continuously
                bleRepository.sensorFlow.collect { state ->
                    // Extract sensor data from the BLE state
                    if (isDataSaveModeOn) {
                        synchronized(lock) {
                            Log.d("AppViewModelValues", "Accel: x=${state.x} y=${state.y} z=${state.z}, z=${state.w}")
                            messageList.add(state)
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d("AppViewModel", "dataSaveJob cancelled")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Unexpected error collecting bleState", e)
            } finally {
                isDataSaveModeOn = false
                dataSaveJob = null
            }
        }
    }

    fun stopDataSave() {
        isDataSaveModeOn = false
        dataSaveJob?.cancel() // stop collecting immediately
        dataSaveJob = null
    }


    /** This function is responsible for starting the action to save the values inside
     * the ROOM database. */
    fun saveMessages() {
        val batch: List<AppService.SensorData>
        synchronized(lock) {
            batch = messageList.toList()  // copy current items
            messageList.clear()           // clear safely
        }
        insertQuaternionValueBatch(batch)
    }
}

// =====================================================================================
// |                                 Session Data Classes
// -------------------------------------------------------------------------------------
/* Stores the values from [SessionManagedScreen.kt], which is used to build up the query
* for the ROOM database */

sealed class SessionType {
    data class Lift(
        val category: LiftCategory,
        val variation: Variation? = null,
    ) : SessionType()

    data class Noise(
        val type: NoiseType
    ) : SessionType()
}

enum class LiftCategory {
    FLOOR_PULL,
    SQUAT,
    HORIZONTAL_PRESS,
    VERTICAL_PRESS,
    WAIST_PULL,
    GENERAL,
}

class Variation (
    val rpe: Int,
    val speed: SpeedVariation,
){
    enum class SpeedVariation{
        EXPLOSIVE,
        CONTROLLED,
        SLOW,
    }
}

enum class NoiseType {
    NOISE,
    ROLLS,
    MOVEMENT,
    UNRACKS
}

enum class SensorDataFormat {
    ACCELERATION,
    QUATERNIONS,
    GYROSCOPE,
    ALL_RAW_VALUES, // contains both raw acceleration and gyroscope values
    ALL_LINEAR_VALUES, // contains linear acceleration and gyro values
}