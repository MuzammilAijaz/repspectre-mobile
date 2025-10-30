package com.example.esp32_mpu6050_mobile_data_collection.ui.view

import androidx.collection.FloatList
import androidx.collection.floatListOf
import androidx.lifecycle.ViewModel
import com.example.esp32_mpu6050_mobile_data_collection.data.AccelerationItem
import com.example.esp32_mpu6050_mobile_data_collection.data.AccelerationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(
    private val accelerationRepository: AccelerationRepository,
) : ViewModel() {

    data class AppUiState(
        val x: Float = 0f,
        val y: Float = 0f,
        val z: Float = 0f,
    )

    private val _uiState = MutableStateFlow(AppUiState())

    // uiState for public access
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    // =====================================================================================
    // |                                 Private Functions
    // =====================================================================================

    private suspend fun updateDatabaseValues(item: AccelerationItem) {
        accelerationRepository.insertItem(item)
    }

    public fun returnCurrentValues(): FloatList {
        return floatListOf(_uiState.value.x, _uiState.value.y, _uiState.value.z)
    }
}