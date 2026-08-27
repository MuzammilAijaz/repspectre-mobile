package com.example.esp32_mpu6050_mobile_data_collection.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esp32_mpu6050_mobile_data_collection.domain.model.LiftCategories
import com.example.esp32_mpu6050_mobile_data_collection.domain.model.MotionStates
import com.example.esp32_mpu6050_mobile_data_collection.domain.model.SensorDataFormats
import com.example.esp32_mpu6050_mobile_data_collection.domain.model.Tempos
import com.example.esp32_mpu6050_mobile_data_collection.ui.theme.Esp32mpu6050mobiledatacollectionTheme
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.AppViewModel

@Composable
fun SessionManagerScreen(
    appViewModel: AppViewModel,
) {
    val uiState by appViewModel.uiState.collectAsState()
    val sessionConfig by appViewModel.sessionConfigState.collectAsState()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 20.dp)
    ) {

        val typeOfSessionSelection = when (sessionConfig) {
            is AppViewModel.SessionConfig.LiftSession -> SessionType.LIFT
            is AppViewModel.SessionConfig.NonLiftSession -> SessionType.NOISE
            is AppViewModel.SessionConfig.LiftSpecificNoiseSession -> SessionType.LIFT_SPECIFIC_NOISE
        }

        Row {
            Button(
                onClick = {
                    appViewModel.setSessionType(SessionType.LIFT)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (typeOfSessionSelection == SessionType.LIFT) Color.Green else Color.Gray
                )
            ) {
                Text("Lift")
            }
            Button(
                onClick = {
                    appViewModel.setSessionType(SessionType.NOISE)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (typeOfSessionSelection == SessionType.NOISE) Color.Green else Color.Gray
                )
            ) {
                Text("Noise")
            }
            Button(
                onClick = {
                    appViewModel.setSessionType(SessionType.LIFT_SPECIFIC_NOISE)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (typeOfSessionSelection == SessionType.LIFT_SPECIFIC_NOISE) Color.Green else Color.Gray
                )
            ) {
                Text("Lift Specific Noise")
            }
        }

        Spacer(modifier = Modifier.padding(vertical = 5.dp))

        if (typeOfSessionSelection == SessionType.LIFT || typeOfSessionSelection == SessionType.LIFT_SPECIFIC_NOISE) {
            Text(text = "Lift Categories", modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp))
            SessionCategorySelectionRow(LiftCategories.entries) { selectedOption ->
                appViewModel.setLiftCategory(selectedOption)
            }
            Spacer(modifier = Modifier.padding(vertical = 5.dp))

            Text(text = "Lift Context", modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp))
            SessionLiftContextSelectionRow(Tempos.entries) { selectedTempo, selectedRPE ->
                appViewModel.setLiftTempo(selectedTempo)
                appViewModel.setRPE(selectedRPE)
            }
            Spacer(modifier = Modifier.padding(vertical = 5.dp))
        }

        if (typeOfSessionSelection == SessionType.NOISE) {
            Text(
                text = "Motion States",
                modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp)
            )
            // Noise sessions DO NOT require lift context
            SessionMotionStateSelectionRow(
                MotionStates.entries.filter { !it.requiresLiftContext }
            ) { selectedOption ->
                appViewModel.setMotionState(selectedOption)
            }
            Spacer(modifier = Modifier.padding(vertical = 5.dp))
        }
        else if (typeOfSessionSelection == SessionType.LIFT_SPECIFIC_NOISE){
            // only display states which require lift context if its lift session
            SessionMotionStateSelectionRow(
                MotionStates.entries.filter { it.requiresLiftContext }
            ) { selectedOption ->
                appViewModel.setMotionState(selectedOption)
            }
            Spacer(modifier = Modifier.padding(vertical = 5.dp))
        }

        Text(text = "Data Formats", modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp))
        SessionSensorDataFormatSelectionScreen(SensorDataFormats.entries) { selectedOption ->
            appViewModel.setSensorDataFormat(selectedOption)
        }
        Spacer(modifier = Modifier.padding(vertical = 5.dp))

        Button(
            onClick = {
                if (uiState.isDataSaveModeOn) appViewModel.stopCollection() else appViewModel.startCollection()
            }
        ) {
            if (uiState.isDataSaveModeOn) Text("Stop Storing") else Text("Create Session and start Collecting Data")
        }

        if (uiState.isDataSaveModeOn) {
            Text(text = "Time: ${(uiState.duration) / 1000}")
        }
    }
}

@Composable
fun SessionCategorySelectionRow(category: List<LiftCategories>, onSelectionChange: (LiftCategories) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center
    ) {
        var selectedOption by remember { mutableStateOf(category.first()) }
        category.forEach { label ->
            Button(
                onClick = {
                    selectedOption = label
                    onSelectionChange(label)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == label) Color.Green else Color.Gray,
                )
            ) {
                Text(label.name)
            }
        }
    }
}

@Composable
fun SessionMotionStateSelectionRow(motionState: List<MotionStates>, onSelectionChange: (MotionStates) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center
    ) {
        var selectedOption by remember { mutableStateOf(motionState.first()) }
        motionState.forEach { label ->
            Button(
                onClick = {
                    selectedOption = label
                    onSelectionChange(label)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == label) Color.Green else Color.Gray,
                )
            ) {
                Text(label.name)
            }
        }
    }
}

@Composable
fun SessionLiftContextSelectionRow(tempo: List<Tempos>, onSelectionChange: (Tempos, Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center
    ) {
        var rpe by remember { mutableStateOf("") }
        var selectedOption by remember { mutableStateOf(tempo.first()) }

        TextField(
            value = rpe,
            label = { Text("Enter RPE") },
            onValueChange = { newVal ->
                val rpeInt = newVal.toIntOrNull()
                if (rpeInt != null && rpeInt <= 10 && rpeInt >= 0) {
                    rpe = newVal
                    onSelectionChange(selectedOption, rpe.toIntOrNull() ?: 7)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        tempo.forEach { label ->
            Button(
                onClick = {
                    selectedOption = label
                    onSelectionChange(label, rpe.toIntOrNull() ?: 7)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == label) Color.Green else Color.Gray,
                )
            ) {
                Text(label.name)
            }
        }
    }
}

@Composable
fun SessionSensorDataFormatSelectionScreen(dataFormats: List<SensorDataFormats>, onSelectionChange: (SensorDataFormats) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center
    ) {
        var selectedOption by remember { mutableStateOf(dataFormats.first()) }
        dataFormats.forEach { label ->
            Button(
                onClick = {
                    selectedOption = label
                    onSelectionChange(label)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == label) Color.Green else Color.Gray,
                )
            ) {
                Text(label.name)
            }
        }
    }
}

enum class SessionType {
    LIFT,
    NOISE,
    LIFT_SPECIFIC_NOISE,
}

@Preview
@Composable
fun SessionManagerPreview() {
    Esp32mpu6050mobiledatacollectionTheme {
        val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
        SessionManagerScreen(viewModel)
    }
}
