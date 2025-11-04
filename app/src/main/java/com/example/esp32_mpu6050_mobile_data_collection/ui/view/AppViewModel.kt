package com.example.esp32_mpu6050_mobile_data_collection.ui.view

import androidx.collection.FloatList
import androidx.collection.floatListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.esp32_mpu6050_mobile_data_collection.data.AccelerationRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.QuaternionRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.SensorApplication
import com.example.esp32_mpu6050_mobile_data_collection.data.entity.AccelerationEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.entity.QuaternionEntity
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(
    private val accelerationRepository: AccelerationRepository,
    private val quaternionRepository: QuaternionRepository,
) : ViewModel() {

    data class AppUiState(
        val x: Float = 0f,
        val y: Float = 0f,
        val z: Float = 0f,
    )

    private val _uiState = MutableStateFlow(AppUiState())

    // uiState for public access
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SensorApplication)
                val accelerationRepository = application.container.accelerationRepository
                val quaternionRepository = application.container.quaternionRepository
                AppViewModel(accelerationRepository, quaternionRepository)
            }
        }
    }

    // =====================================================================================
    // |                                 Private Functions
    // =====================================================================================

    private suspend fun updateAccelDatabaseValues(createEntityFromSession: (sessionId: Long) -> AccelerationEntity) {
        accelerationRepository.insertItem(createEntityFromSession = createEntityFromSession)
    }

    private suspend fun updateQuatDatabaseValues(createEntityFromSession: (sessionId: Long) -> QuaternionEntity) {
        quaternionRepository.insertItem(createEntityFromSession = createEntityFromSession)
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

}
