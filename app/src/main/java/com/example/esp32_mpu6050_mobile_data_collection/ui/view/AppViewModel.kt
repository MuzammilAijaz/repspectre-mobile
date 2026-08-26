package com.example.esp32_mpu6050_mobile_data_collection.ui.view

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
import com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository.AccelerationRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository.FullIMURawRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository.QuaternionRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository.RawDataRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.AccelerationEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.FullIMURawEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.QuaternionEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.RawDataEntity
import com.example.esp32_mpu6050_mobile_data_collection.domain.model.LiftCategories
import com.example.esp32_mpu6050_mobile_data_collection.domain.model.MotionStates
import com.example.esp32_mpu6050_mobile_data_collection.domain.model.SensorDataFormats
import com.example.esp32_mpu6050_mobile_data_collection.domain.model.Tempos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class AppViewModel(
    private val accelerationRepository: AccelerationRepository,
    private val quaternionRepository: QuaternionRepository,
    private val bleRepository: AppBleRepository,
    private val rawDataRepository: RawDataRepository,
    private val fullIMURawRepository: FullIMURawRepository
) : ViewModel() {

    sealed class SessionConfig {
        abstract var motionState: MotionStates
        abstract var sensorDataFormat: SensorDataFormats

        data class LiftSession(
            override var motionState: MotionStates = MotionStates.REP_START,
            override var sensorDataFormat: SensorDataFormats = SensorDataFormats.FULL_IMU_RAW,
            var liftCategory: LiftCategories = LiftCategories.FLOOR_PULL,
            var rpe: Int = 7,
            var tempo: Tempos = Tempos.NORMAL
        ) : SessionConfig() {
            init {
                // REFACTOR: implement better type system (i.e LiftMotionState : MotionState) so i dont
                // have to do this check
                require(motionState.requiresLiftContext) {
                    "motionState should be set to a MotionState which has requiresLiftContext == true"
                }
            }
        }

        data class NonLiftSession(
            override var motionState: MotionStates = MotionStates.SENSOR_DRIFT,
            override var sensorDataFormat: SensorDataFormats = SensorDataFormats.FULL_IMU_RAW
        ) : SessionConfig() {
            init {
                // REFACTOR: implement better type system (i.e LiftMotionState : MotionState) so i don't
                // have to do this check
                require(!motionState.requiresLiftContext) {
                    "motionState should be set to a MotionState which has requiresLiftContext == false"
                }
            }
        }
    }

    data class UiState(
        var isDataSaveModeOn: Boolean = false,
        var isNewSession: Boolean = false,
        var startTime: Long = 0,
        var duration: Long = 0,
        // NOTE:  END TIME IS CALCULATED DURING INSERTION FOR NOW......
    )

    // ----- States -------------------------------------------------
    // ---- Holds Session data for query building ----
    private val _sessionConfigState = MutableStateFlow<SessionConfig>(SessionConfig.LiftSession())
    val sessionConfigState: StateFlow<SessionConfig> = _sessionConfigState.asStateFlow()

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
                val fullIMURawRepository = application.container.fullIMURawRepository
                AppViewModel(accelerationRepository, quaternionRepository, bleRepository, rawDataRepository, fullIMURawRepository)
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

    private suspend fun updateFullIMURawDatabaseValuesInBatch(createEntityListFromSession: (sessionId: Long) -> List<FullIMURawEntity>) {
        fullIMURawRepository.insertItemBatch(createEntityListFromSession = createEntityListFromSession)
    }

    private fun insertQuaternionValueBatch(data: List<SensorData.Quaternion>) {
        viewModelScope.launch(Dispatchers.IO) {
            updateQuatDatabaseValuesInBatch { sessionId ->
                data.map { item ->
                    QuaternionEntity(sessionId = sessionId, x = item.x, y = item.y, z = item.z, w = item.w ?: 0.00f)
                }
            }
        }
    }

    private fun insertRawValueBatch(data: List<SensorData.Raw>) {
        viewModelScope.launch(Dispatchers.IO) {
            updateRawDatabaseValuesInBatch { sessionId ->
                data.map { item ->
                    RawDataEntity(sessionId = sessionId, ax = item.ax, ay = item.ay, az = item.az, gx = item.gx, gy = item.gy, gz = item.gz)
                }
            }
        }
    }

    private fun insertFullIMURawValueBatch(data: List<SensorData.FullIMURaw>) {
        viewModelScope.launch(Dispatchers.IO) {
            updateFullIMURawDatabaseValuesInBatch { sessionId ->
                data.map { item ->
                    FullIMURawEntity(
                        sessionId = sessionId,
                        ax = item.ax,
                        ay = item.ay,
                        az = item.az,
                        gx = item.gx,
                        gy = item.gy,
                        gz = item.gz,
                        qx = item.qx,
                        qy = item.qy,
                        qz = item.qz,
                        qw = item.qw,
                        timestampUs = item.timestampUs
                    )
                }
            }
        }
    }

    private fun startTimer() {
        val startTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(startTime = startTime, duration = 0)
    }

    fun stopOldSession() {
        viewModelScope.launch(Dispatchers.IO) {
            quaternionRepository.stopOldSession()
            accelerationRepository.stopOldSession()
            rawDataRepository.stopOldSession()
            fullIMURawRepository.stopOldSession()
        }
    }

    fun createNewSession() {
        quaternionRepository.createNewSession(_sessionConfigState.value)
        accelerationRepository.createNewSession(_sessionConfigState.value)
        rawDataRepository.createNewSession(_sessionConfigState.value)
        fullIMURawRepository.createNewSession(_sessionConfigState.value)
        _uiState.value = _uiState.value.copy(isNewSession = false)
    }

    fun setLiftCategory(category: LiftCategories) {
        val config = _sessionConfigState.value
        check(config is SessionConfig.LiftSession) {
            "setLiftCategory() called when session is not a LiftSession"
        }
        _sessionConfigState.value = config.copy(liftCategory = category)
    }

    fun setLiftTempo(tempo: Tempos) {
        val config = _sessionConfigState.value
        check(config is SessionConfig.LiftSession) {
            "setLiftTempo() called when session is not a LiftSession"
        }
        _sessionConfigState.value = config.copy(tempo = tempo)
    }

    fun setRPE(rpe: Int) {
        val config = _sessionConfigState.value
        check(config is SessionConfig.LiftSession) {
            "setRPE() called when session is not a LiftSession"
        }
        _sessionConfigState.value = config.copy(rpe = rpe)
    }

    fun setSensorDataFormat(format: SensorDataFormats) {
        val current = _sessionConfigState.value
        _sessionConfigState.value = when (current) {
            is SessionConfig.LiftSession -> current.copy(sensorDataFormat = format)
            is SessionConfig.NonLiftSession -> current.copy(sensorDataFormat = format)
        }
    }

    fun setMotionState(motionState: MotionStates) {
        val current = _sessionConfigState.value
        _sessionConfigState.value = when (current) {
            is SessionConfig.LiftSession -> {
                if (motionState.requiresLiftContext) {
                    current.copy(motionState = motionState)
                } else {
                    SessionConfig.NonLiftSession(motionState, current.sensorDataFormat)
                }
            }
            is SessionConfig.NonLiftSession -> {
                if (motionState.requiresLiftContext) {
                    SessionConfig.LiftSession(motionState = motionState, sensorDataFormat = current.sensorDataFormat)
                } else {
                    current.copy(motionState = motionState)
                }
            }
        }
    }

    fun startCollection() {
        _uiState.value = _uiState.value.copy(isDataSaveModeOn = true, isNewSession = true)
        createNewSession()
        startDataSave()
    }

    fun stopCollection() {
        _uiState.value = _uiState.value.copy(isDataSaveModeOn = false, isNewSession = false)
        stopOldSession()
        stopDataSave()
        saveMessages()
    }

    fun cleanDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            quaternionRepository.cleanDatabase()
            accelerationRepository.cleanDatabase()
            rawDataRepository.cleanDatabase()
            fullIMURawRepository.cleanDatabase()
        }
    }

    fun insertQuaternionValue(data: SensorData.Quaternion) {
        viewModelScope.launch(Dispatchers.IO) {
            updateQuatDatabaseValues { sessionId ->
                QuaternionEntity(sessionId = sessionId, x = data.x, y = data.y, z = data.z, w = data.w ?: 0.00f)
            }
        }
    }

    fun getAllQuaternions(): Flow<List<QuaternionEntity>> {
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
        _uiState.value = _uiState.value.copy(isDataSaveModeOn = true)
        timeHandler()
        dataSaveJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Collect BLE state updates continuously
                bleRepository.sensorFlow.collect { state ->
                    // Extract sensor data from the BLE state
                    if (_uiState.value.isDataSaveModeOn) {
                        synchronized(lock) {
                            if (state is SensorData.Quaternion) {
                                Log.d("AppViewModelValues", "Accel: x=${state.x} y=${state.y} z=${state.z}, w=${state.w}")
                            }
                            if (state is SensorData.FullIMURaw) {
                                Log.d("AppViewModelValues", "FullIMURaw: ax=${state.ax} ay=${state.ay} az=${state.az} gx=${state.gx} gy=${state.gy} gz=${state.gz} qx=${state.qx} qy=${state.qy} qz=${state.qz} qw=${state.qw} ts=${state.timestampUs}")
                            }
                            messageList.add(state)
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d("AppViewModel", "dataSaveJob cancelled")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Unexpected error collecting bleState", e)
            } finally {
                _uiState.value = _uiState.value.copy(isDataSaveModeOn = false)
                dataSaveJob = null
            }
        }
    }

    /** Sets the isDataSaveModeOn to false on timer expire */
    fun timeHandler() {
        startTimer()

        viewModelScope.launch(Dispatchers.IO) {
            while (_uiState.value.isDataSaveModeOn) {
                val currentTime = System.currentTimeMillis()
                val duration = currentTime - _uiState.value.startTime
                _uiState.value = _uiState.value.copy(duration = duration)
                if (duration > 10000) {
                    break
                }
                kotlinx.coroutines.delay(100) // Avoid tight loop
            }
            stopCollection()
        }
    }

    fun stopDataSave() {
        _uiState.value = _uiState.value.copy(isDataSaveModeOn = false)
        dataSaveJob?.cancel()
        dataSaveJob = null
    }

    /** This function is responsible for starting the action to save the values inside
     * the ROOM database. */
    fun saveMessages() {
        val batch: List<SensorData>
        synchronized(lock) {
            batch = messageList.toList()
            messageList.clear()
        }
        if (batch.isEmpty()) return;

        when(batch.first()) {
            is SensorData.FullIMURaw -> insertFullIMURawValueBatch(batch.filterIsInstance<SensorData.FullIMURaw>())
            is SensorData.Quaternion -> insertQuaternionValueBatch(batch.filterIsInstance<SensorData.Quaternion>())
            is SensorData.Raw -> insertRawValueBatch(batch.filterIsInstance<SensorData.Raw>())
            else -> {}
        }
    }
}
