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
import com.example.esp32_mpu6050_mobile_data_collection.data.SensorData
import com.example.esp32_mpu6050_mobile_data_collection.data.database.AccelerationRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.QuaternionRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.RawDataRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.AccelerationEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.QuaternionEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.RawDataEntity
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
    private val bleRepository: AppBleRepository,
    private val rawDataRepository: RawDataRepository
) : ViewModel() {

    // ----- State Data Classes ------------------------------------
    /* This will be used as the query builder for the session type */
    data class SessionData(
        var liftCategory: LiftCategory = LiftCategory.FLOOR_PULL,
        var variation: Variation = Variation(rpe = 7, speed = Variation.SpeedVariation.CONTROLLED),
        var noiseCategory: NoiseCategory? = null,
        var dataFormat: SensorDataFormat = SensorDataFormat.FULL_IMU_RAW,
    )

    data class UiState(
        var isDataSaveModeOn: Boolean = false,
        var isNewSession: Boolean = false,
        var startTime: Long = 0,
        var duration: Long = 0,
        // NOTE:  END TIME IS CALCULATED DURING INSERTION FOR NOW......
    )

    // ----- States -------------------------------------------------
    // ---- Holds Session data for query building ----
    private val _sessionDataState = MutableStateFlow(SessionData())
    val sessionDataState: StateFlow<SessionData> = _sessionDataState.asStateFlow()

    // ---- Holds ui state for UI elements ----
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ----- Factory ------------------------------------------------
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SensorApplication)
                val accelerationRepository = application.container.accelerationRepository
                val quaternionRepository = application.container.quaternionRepository
                val bleRepository = application.container.bleRepository
                val rawDataRepository = application.container.rawDataRepository
                AppViewModel(accelerationRepository, quaternionRepository, bleRepository, rawDataRepository)
            }
        }
    }

    // ----- Private State ------------------------------------------
    private var messageList: MutableList<SensorData> = mutableListOf<SensorData>()
    private val lock = Any()

    // =====================================================================================
    // |                         Repository Private Functions
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

    private suspend fun updateRawDatabaseValuesInBatch(createEntityListFromSession: (sessionId: Long) -> List<RawDataEntity>) {
        rawDataRepository.insertItemBatch(createEntityListFromSession = createEntityListFromSession)
    }

    private fun insertQuaternionValueBatch(data: List<SensorData.Quaternion>) {
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

    private fun insertRawValueBatch(data: List<SensorData.Raw>) {
        // Coroutine scope launches and returns immediately, as its non-blocking
        viewModelScope.launch(Dispatchers.IO) {

            updateRawDatabaseValuesInBatch() { sessionId ->
                data.map { item ->
                    Log.d("QuatData", "x: $x")

                    RawDataEntity(sessionId = sessionId, ax = item.ax, ay = item.ay, az = item.ay, gx = item.gx, gy = item.gy, gz = item.gz)
                }
            }
        }
    }

    // =====================================================================================
    // |                                Private Functions
    // =====================================================================================

    private fun startTimer() {
        _uiState.update { it.copy(startTime = System.currentTimeMillis(), duration = 0) }
    }

    // =====================================================================================
    // |                                 Public Functions
    // =====================================================================================

    public fun stopOldSession() {
        viewModelScope.launch(Dispatchers.IO) {
            quaternionRepository.stopOldSession()
            accelerationRepository.stopOldSession()
            rawDataRepository.stopOldSession()
        }
    }

    public fun createNewSession() {
        // Simply sets the flag for the creation of a new session
        quaternionRepository.createNewSession(_sessionDataState.value)
        accelerationRepository.createNewSession(_sessionDataState.value)
        rawDataRepository.createNewSession(_sessionDataState.value)

        _uiState.update { it.copy(isNewSession = false) }
    }

    fun setLiftCategory(category: LiftCategory) {
        _sessionDataState.update { it.copy(liftCategory = category) }
    }

    fun setVariation(variation: Variation) {
        _sessionDataState.update { it.copy(variation = variation) }
    }

    fun setSensorDataFormat(format: SensorDataFormat) {
        _sessionDataState.update { it.copy(dataFormat = format) }
    }

    fun setNoiseCategory(noise: NoiseCategory?) {
        _sessionDataState.update { it.copy(noiseCategory = noise) }
    }

    fun startCollection() {
        _uiState.update { it.copy(isDataSaveModeOn = true, isNewSession = true) }
        createNewSession()
        startDataSave()
    }

    fun stopCollection() {
        _uiState.update { it.copy(isDataSaveModeOn = false, isNewSession = false) } // for when user stops manually

        stopOldSession()
        stopDataSave()
        saveMessages()
    }

    public fun cleanDatabase() {
        viewModelScope.launch(Dispatchers.IO){
            quaternionRepository.cleanDatabase()
            accelerationRepository.cleanDatabase()
            rawDataRepository.cleanDatabase()
        }
    }

    public fun insertQuaternionValue(data: SensorData.Quaternion) {

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

        _uiState.update { it.copy(isDataSaveModeOn = true) } // just in case
        timeHandler()

        dataSaveJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Collect BLE state updates continuously
                bleRepository.sensorFlow.collect { state ->
                    // Extract sensor data from the BLE state
                    if (_uiState.value.isDataSaveModeOn) {
                        synchronized(lock) {
                            if(state is SensorData.Quaternion) Log.d("AppViewModelValues", "Accel: x=${state.x} y=${state.y} z=${state.z}, z=${state.w}")
                            messageList.add(state)
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d("AppViewModel", "dataSaveJob cancelled")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Unexpected error collecting bleState", e)
            } finally {
                _uiState.update { it.copy(isDataSaveModeOn = false) }
                dataSaveJob = null
            }
        }
    }

    /** Sets the isDataSaveModeOn to false on timer expire */
    fun timeHandler() {
        startTimer()

        viewModelScope.launch(Dispatchers.IO) {
            while(_uiState.value.isDataSaveModeOn) {
                val currentTime = System.currentTimeMillis()
                _uiState.update { it.copy(duration = currentTime - _uiState.value.startTime) }

                if (_uiState.value.duration > 10000) { // if greater than 10 seconds, close database connection
                    break
                }
            }
            stopCollection()
        }
    }

    fun stopDataSave() {
        _uiState.value.isDataSaveModeOn = false
        dataSaveJob?.cancel() // stop collecting immediately
        dataSaveJob = null
    }

    /** This function is responsible for starting the action to save the values inside
     * the ROOM database. */
    fun saveMessages() {
        val batch: List<SensorData>
        synchronized(lock) {
            batch = messageList.toList()  // copy current items
            messageList.clear()           // clear safely
        }
        if (batch.isEmpty()) return;

        when(batch.first()) {
            is SensorData.Quaternion -> insertQuaternionValueBatch(batch.filterIsInstance<SensorData.Quaternion>())
            is SensorData.Raw -> insertRawValueBatch(batch.filterIsInstance<SensorData.Raw>())
            else -> {}
        }
    }
}

// =====================================================================================
// |                                 Session Data Classes
// -------------------------------------------------------------------------------------
/* Stores the values from [SessionManagedScreen.kt], which is used to build up the query
* for the ROOM database */

//sealed class SessionType {
//    data class Lift(
//        val category: LiftCategory,
//        val variation: Variation? = null,
//    ) : SessionType()
//
//    data class Noise(
//        val type: NoiseType
//    ) : SessionType()
//}

enum class LiftCategory {
    FLOOR_PULL,        // Deadlifts & floor-start pulls
    SQUAT,             // All squat variations
    PRESS_HORIZONTAL,  // Bench variations
    PRESS_VERTICAL,    // OHP variations
    UPRIGHT_PULL,      // Upright rows, curls, high pulls
    ROW,               // Bent-over rows, Pendlay rows
    OTHER              // Everything else
}

class Variation (
    val rpe: Int,
    val speed: SpeedVariation,
){
    enum class SpeedVariation{
        NORMAL,
        EXPLOSIVE,          // Aiming at Max acceleration (speed work)
        FAST,               // Faster-than-normal reps
        CONTROLLED,         // Standard tempo
        SLOW_TEMPO,         // Intentionally slow (3–5 sec phases)
        PAUSED,             // Pause reps (e.g., pause squat)
        ECCENTRIC_EMPHASIS, // Slow descent, normal ascent
        CONCENTRIC_EMPHASIS // Normal descent, slow ascent
    }
}

enum class NoiseCategory{
    // Sensor-level noise
    SENSOR_JITTER,            // Random IMU jitter
    SENSOR_DRIFT,             // Gradual orientation drift
    SENSOR_VIBRATION,         // High-frequency vibration on the bar

    // Barbell non-lift movement noise
    BARBELL_ROLLING,          // Rolling on floor or rack
    BARBELL_MICROMOTION,      // Slight bar shifts with no rep
    BARBELL_IMPACT,           // Bar hitting rack or safeties

    // Lift-related transitions (not actual reps)
    UNRACK_TRANSIENT,         // Unrack acceleration spike
    RERACK_TRANSIENT,         // Rerack acceleration spike
    SETUP_MOVEMENT,           // Athlete adjusting grip/feet before rep

    // Environment
    EXTERNAL_DISTURBANCE,     // Someone bumps into you/rack
    PLATFORM_VIBRATION,       // Deadlift platform shaking
    BACKGROUND_GYM_MOTION,    // People walking, movement nearby

    // Barbell state
    BARBELL_STATIONARY        // Completely still reference state
}

// TODO : not implemented yet
enum class SensorDataFormat {
    ACCELERATION_RAW,         // raw accel
    GYROSCOPE_RAW,            // raw gyro
    MAGNETOMETER_RAW,         // optional future expansion
    QUATERNION_ORIENTATION,   // fused orientation
    LINEAR_ACCELERATION,      // acceleration with gravity removed
    FULL_IMU_RAW,             // accel + gyro + (maybe mag)
    FULL_IMU_PROCESSED,       // fused + filtered signals
    ALL_FEATURES,             // everything including derived values (jerk, etc)
}