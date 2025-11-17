package com.example.esp32_mpu6050_mobile_data_collection.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.AppViewModel
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.LiftCategory
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.NoiseType
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.SensorDataFormat
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.Variation

@Composable
fun SessionManagerScreen(
    appViewModel: AppViewModel,
) {
    val sessionState by appViewModel.sessionDataState.collectAsState()
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val categoryOptions = LiftCategory.entries
        val noiseOptions = NoiseType.entries
        val speedVariationOptions = Variation.SpeedVariation.entries
        val dataFormatOptions = SensorDataFormat.entries

        // ------------- TODO : move this to the viewmodel. --------------------
        // ---- Type of Life ----
        var categorySelection: LiftCategory by remember { mutableStateOf(categoryOptions.first()) }
        var noiseSelection: NoiseType by remember { mutableStateOf(noiseOptions.first()) }
        var speedVariationSelection: Variation.SpeedVariation by remember { mutableStateOf(speedVariationOptions.first()) }
        var rpeSelection: Int by remember { mutableStateOf(7) }
        // ---- Type of Data required to be colllected ----
        var dataFormatSelection: SensorDataFormat by remember { mutableStateOf(SensorDataFormat.ALL_RAW_VALUES) }
        // ------------- TODO : move this to the viewmodel. --------------------

        SessionCategorySelectionRow(categoryOptions) { selectedOption ->
            categorySelection = selectedOption
            appViewModel.setCategory(categorySelection)
        }

        SessionNoiseSelectionRow(noiseOptions) { selectedOption ->
            noiseSelection = selectedOption
            appViewModel.setNoise(noiseSelection)
        }

        SessionVariationSelectionRow(speedVariationOptions) { selectedOption, selectedRPE ->
            speedVariationSelection = selectedOption
            rpeSelection = selectedRPE
            appViewModel.setVariation(Variation(rpe = selectedRPE, selectedOption))
        }

        SessionDataFormSelectionScreen(dataFormatOptions) { selectedOption ->
            dataFormatSelection = selectedOption
            appViewModel.setSensorDataFormat(selectedOption)
        }

        // ----- Timer Functionality ------------------------------------
        var isStoreDataOn by remember {mutableStateOf(false)}
        var isNewSession by remember {mutableStateOf(false)}
        var startTime by remember { mutableLongStateOf(0) }
        var duration by remember { mutableLongStateOf(0)}
        Button(
            onClick = {
                isStoreDataOn = !isStoreDataOn
                startTime = System.currentTimeMillis()
                duration = 0
                isNewSession = !isNewSession
            }
        ) {
            if (!isStoreDataOn) Text("Create Session and start Collecting Data") else
                Text("Stop Storing")
        }

        // Start the Timer if button pressed
        if (isStoreDataOn) {
            if(isNewSession) {
                appViewModel.createNewSession()
                isNewSession = false
            }
            Log.d("Database", "Collection Started")
            appViewModel.startDataSave()
        }

        // Display the Time
        if (isStoreDataOn) {
            val currentTime = System.currentTimeMillis()
            duration = currentTime - startTime
            Text(text = "Time: ${(duration) / 1000}")
        }

        // Stop the Timer if duration > 10seconds or user stops
        if (duration > 10000 || !isStoreDataOn) { // if greater than 10 seconds, close database connection
            isStoreDataOn = false
            isNewSession = false
            appViewModel.stopOldSession()
            appViewModel.stopDataSave()
            appViewModel.saveMessages()
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
fun SessionNoiseSelectionRow(noise: List<NoiseType>, onSelectionChange: (NoiseType) -> Unit) {
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        var selectedOption: NoiseType by remember { mutableStateOf(noise.first()) }

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
                    onSelectionChange(selectedOption, rpe.toInt())
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
//        val options = NoiseType.entries.map { it.toString() }
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

//@Preview
//@Composable
//fun SessionManagerPreview() {
//    Esp32mpu6050mobiledatacollectionTheme{
//        SessionManagerScreen()
//    }
//}