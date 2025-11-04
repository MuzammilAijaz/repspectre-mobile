package com.example.esp32_mpu6050_mobile_data_collection.ui.view

import androidx.collection.FloatList
import androidx.collection.floatListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.esp32_mpu6050_mobile_data_collection.data.AccelerationEntity
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

    private suspend fun updateDatabaseValues(createEntityFromSession: (sessionId: Long) -> AccelerationEntity) {
        accelerationRepository.insertItem(createEntityFromSession = createEntityFromSession)
    }

// TODO: Move this somewhere else like the repository .......................................
    /** Returns List in form of : x, y, z */
    private fun parseString(string: String): List<Float>{
        val input = string

        val regex = Regex("""x=([-+]?\d*\.?\d+)\s+y=([-+]?\d*\.?\d+)\s+z=([-+]?\d*\.?\d+)""")
        val match = regex.find(input)

        if (match != null) {
            val (x, y, z) = match.destructured
            val xFloat = x.toFloat()
            val yFloat = y.toFloat()
            val zFloat = z.toFloat()
            return listOf(xFloat,yFloat,zFloat)
        }
        else {
            return listOf(0f, 0f, 0f)
        }
    }

    // =====================================================================================
    // |                                 Public Functions
    // =====================================================================================
    public fun stopOldSession() {viewModelScope.launch(Dispatchers.IO) {accelerationRepository.stopOldSession()}}
    public fun createNewSession() {accelerationRepository.createNewSession()}
    public fun cleanDatabase() {viewModelScope.launch(Dispatchers.IO){accelerationRepository.cleanDatabase()}}

    public fun insertValue(values: String) {

        // Coroutine scope launches and returns immediately, as its non-blocking
        viewModelScope.launch(Dispatchers.IO) {

            updateDatabaseValues() { sessionId ->
                val accelerationValues: List<Float> = parseString(values)
                AccelerationEntity(sessionId = sessionId, x = accelerationValues[0], y = accelerationValues[1], z = accelerationValues[2])
            }
        }
    }

    public fun returnCurrentValues(): FloatList {
        return floatListOf(_uiState.value.x, _uiState.value.y, _uiState.value.z)
    }

}
