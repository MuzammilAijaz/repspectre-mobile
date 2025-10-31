package com.example.esp32_mpu6050_mobile_data_collection.ui.view

import androidx.collection.FloatList
import androidx.collection.floatListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.esp32_mpu6050_mobile_data_collection.data.AccelerationItem
import com.example.esp32_mpu6050_mobile_data_collection.data.AccelerationRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.SensorApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SensorApplication)
                val accelerationRepository = application.container.accelerationRepository
                AppViewModel(accelerationRepository)
            }
        }
    }

    // =====================================================================================
    // |                                 Private Functions
    // =====================================================================================

    private suspend fun updateDatabaseValues(item: AccelerationItem) {
        accelerationRepository.insertItem(item)
    }

    private fun parseString(string: String): AccelerationItem{
        val input = string

        val regex = Regex("""x=([-+]?\d*\.?\d+)\s+y=([-+]?\d*\.?\d+)\s+z=([-+]?\d*\.?\d+)""")
        val match = regex.find(input)

        if (match != null) {
            val (x, y, z) = match.destructured
            val xFloat = x.toFloat()
            val yFloat = y.toFloat()
            val zFloat = z.toFloat()
            return AccelerationItem(x = xFloat, y = yFloat, z = zFloat)
        }
        else {
            return AccelerationItem(x = 0f, y = 0f, z = 0f)
        }
    }

    // =====================================================================================
    // |                                 Public Functions
    // =====================================================================================
    public fun insertValue(values: String) {

        // Coroutine scope launches and returns immediately, as its non-blocking
        viewModelScope.launch(Dispatchers.IO) {
            updateDatabaseValues(parseString(values))
        }
    }

    public fun returnCurrentValues(): FloatList {
        return floatListOf(_uiState.value.x, _uiState.value.y, _uiState.value.z)
    }
}