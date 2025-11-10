package com.example.esp32_mpu6050_mobile_data_collection.ui.view

import android.R.attr.x
import android.util.Log
import androidx.collection.FloatList
import androidx.collection.floatListOf
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
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class AppViewModel(
    private val accelerationRepository: AccelerationRepository,
    private val quaternionRepository: QuaternionRepository,
    private val bleRepository: AppBleRepository
) : ViewModel() {

    data class AppUiState(
        val x: Float = 0f,
        val y: Float = 0f,
        val z: Float = 0f,
    )

    val bleState: StateFlow<AppService.BleStateProvider.BleState> = bleRepository.bleState // only used when user presses store to database button
    private val _uiState = MutableStateFlow(AppUiState())

    // uiState for public access
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

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
        quaternionRepository.createNewSession()
        accelerationRepository.createNewSession()
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

    public fun returnCurrentValues(): FloatList {
        return floatListOf(_uiState.value.x, _uiState.value.y, _uiState.value.z)
    }

    public fun getAllQuaternions(): Flow<List<QuaternionEntity>> {
        return quaternionRepository.getAllItemsStream()
    }

    private var dataSaveJob: Job? = null

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

    fun saveMessages() {
        val batch: List<AppService.SensorData>
        synchronized(lock) {
            batch = messageList.toList()  // copy current items
            messageList.clear()           // clear safely
        }
        insertQuaternionValueBatch(batch)
    }
}
