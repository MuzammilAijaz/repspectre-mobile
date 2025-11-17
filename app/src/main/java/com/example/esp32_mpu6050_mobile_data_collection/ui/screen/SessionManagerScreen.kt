package com.example.esp32_mpu6050_mobile_data_collection.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esp32_mpu6050_mobile_data_collection.ui.theme.Esp32mpu6050mobiledatacollectionTheme
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.AppViewModel
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.LiftCategory
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.NoiseCategory
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.SensorDataFormat
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.Variation

@Composable
fun SessionManagerScreen(
    appViewModel: AppViewModel,
) {
    val uiState by appViewModel.uiState.collectAsState()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        val categoryOptions = LiftCategory.entries
        val noiseOptions = NoiseCategory.entries
        val speedVariationOptions = Variation.SpeedVariation.entries
        val dataFormatOptions = SensorDataFormat.entries

        SessionCategorySelectionRow(categoryOptions) { selectedOption ->
            appViewModel.setLiftCategory(selectedOption)
        }

        SessionNoiseSelectionRow(noiseOptions) { selectedOption ->
            appViewModel.setNoiseCategory(selectedOption)
        }

        SessionVariationSelectionRow(speedVariationOptions) { selectedOption, selectedRPE ->
            appViewModel.setVariation(Variation(rpe = selectedRPE, selectedOption))
        }

        SessionDataFormSelectionScreen(dataFormatOptions) { selectedOption ->
            appViewModel.setSensorDataFormat(selectedOption)
        }

        // ----- Timer Functionality ------------------------------------
        // Start the Timer if button pressed
        Button(
            onClick = {
                if (!uiState.isDataSaveModeOn) appViewModel.startCollection() else appViewModel.stopCollection()
            }
        ) {
            if (!uiState.isDataSaveModeOn) Text("Create Session and start Collecting Data") else
                Text("Stop Storing")
        }

        // Display the Time
        if (uiState.isDataSaveModeOn) {
            Text(text = "Time: ${(uiState.duration) / 1000}")
        }
        // --------------------------------------------------------------
    }
}

@Composable
fun SessionCategorySelectionRow(category: List<LiftCategory>, onSelectionChange: (LiftCategory) -> Unit) {
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        var selectedOption: LiftCategory by remember { mutableStateOf(category.first()) }

        // Display a button for every option
        category.forEach { label ->
            Button(
                onClick = {
                    selectedOption = label
                    onSelectionChange(selectedOption)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == label) Color.Green else Color.Red,
                )
            ) {
                Text(label.name)
            }
        }
    }
}

@Composable
fun SessionNoiseSelectionRow(noise: List<NoiseCategory>, onSelectionChange: (NoiseCategory) -> Unit) {
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        var selectedOption: NoiseCategory by remember { mutableStateOf(noise.first()) }

        // Display a button for every option
        noise.forEach { label ->
            Button(
                onClick = {
                    selectedOption = label
                    onSelectionChange(selectedOption)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == label) Color.Green else Color.Red,
                )
            ) {
                Text(label.name)
            }
        }
    }
}

@Composable
fun SessionVariationSelectionRow(speedVariation: List<Variation.SpeedVariation>, onSelectionChange: (Variation.SpeedVariation, Int) -> Unit) {
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        var rpe by remember { mutableStateOf("") }
        var selectedOption: Variation.SpeedVariation by remember { mutableStateOf(speedVariation.first()) }

        TextField(
            value = rpe,
            label = { Text("Enter RPE") },
            onValueChange = { newVal ->
                val rpeInt = newVal.toIntOrNull()
                if (rpeInt != null && rpeInt <= 10 && rpeInt >=0) {
                    rpe = newVal
                    onSelectionChange(selectedOption, rpe.toIntOrNull()?: 7) // CONSTANT : 7 is the default rpe
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        // Display a button for every option
        speedVariation.forEach { label ->
            Button(
                onClick = {
                    selectedOption = label
                    onSelectionChange(selectedOption, rpe.toInt())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == label) Color.Green else Color.Red,
                )
            ) {
                Text(label.name)
            }
        }
    }
}

@Composable
fun SessionDataFormSelectionScreen(dataFormats: List<SensorDataFormat>, onSelectionChange: (SensorDataFormat) -> Unit) {
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        var selectedOption: SensorDataFormat by remember { mutableStateOf(dataFormats.first()) }

        // Display a button for every option
        dataFormats.forEach { label ->
            Button(
                onClick = {
                    selectedOption = label
                    onSelectionChange(selectedOption)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == label) Color.Green else Color.Red,
                )
            ) {
                Text(label.name)
            }
        }
    }
}

//@Preview
//@Composable
//fun SessionPurposeSelectionRowPreview() {
//    Esp32mpu6050mobiledatacollectionTheme{
//        val options = LiftCategory.entries.map { it.toString() }
//        SessionCategorySelectionRow(options, {})
//    }
//}
//
//@Preview
//@Composable
//fun SessionNoiseSelectionRowPreview() {
//    Esp32mpu6050mobiledatacollectionTheme{
//        val options = NoiseCategory.entries.map { it.toString() }
//        SessionNoiseSelectionRow(options)
//    }
//}
//
//@Preview
//@Composable
//fun SessionVariationSelectionRowPreview() {
//    Esp32mpu6050mobiledatacollectionTheme{
//        val options = Variation.SpeedVariation.entries.map { it.toString() }
//        SessionVariationSelectionRow(options)
//    }
//}
//
//
@Preview
@Composable
fun SessionManagerPreview() {
    Esp32mpu6050mobiledatacollectionTheme{
        val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
        SessionManagerScreen(viewModel)
    }
}