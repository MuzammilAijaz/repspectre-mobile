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

@Composable
fun SessionManagerScreen(
    appViewModel: AppViewModel,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val categoryOptions = LiftCategory.entries.map { it.toString() }
        val noiseOptions = NoiseType.entries.map { it.toString() }
        val variationOptions = Variation.SpeedVariation.entries.map { it.toString() }

        var categorySelection: String by remember { mutableStateOf(categoryOptions.first()) }
        var noiseSelection: String by remember { mutableStateOf(noiseOptions.first()) }
        var variationSelection: String by remember { mutableStateOf(variationOptions.first()) }
        var rpeSelection: String by remember { mutableStateOf("7") }

        SessionCategorySelectionRow(categoryOptions) { selectedOption ->
            categorySelection = selectedOption
        }

        SessionNoiseSelectionRow(noiseOptions) { selectedOption ->
            noiseSelection = selectedOption
        }

        SessionVariationSelectionRow(variationOptions) { selectedRPE, selectedOption ->
            variationSelection = selectedOption
            rpeSelection = selectedRPE
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
            if(isNewSession) {appViewModel.createNewSession() ; isNewSession = false}
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
fun SessionCategorySelectionRow(category: List<String>, onSelectionChange: (String) -> Unit) {
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        var selectedOption: String by remember { mutableStateOf(category.first()) }

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
                Text(label)
            }
        }
    }
}

@Composable
fun SessionNoiseSelectionRow(noise: List<String>, onSelectionChange: (String) -> Unit) {
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        var selectedOption: String by remember { mutableStateOf(noise.first()) }

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
                Text(label)
            }
        }
    }
}

@Composable
fun SessionVariationSelectionRow(variation: List<String>, onSelectionChange: (String, String) -> Unit) {
    FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        var rpe by remember { mutableStateOf("") }
        var selectedOption: String by remember { mutableStateOf(variation.first()) }

        TextField(
            value = rpe,
            label = { Text("Enter RPE") },
            onValueChange = { newVal ->
                val rpeInt = newVal.toIntOrNull()
                if (rpeInt != null && rpeInt <= 10 && rpeInt >=0) {
                    rpe = newVal
                    onSelectionChange(rpe, selectedOption)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        // Display a button for every option
        variation.forEach { label ->
            Button(
                onClick = {
                    selectedOption = label
                    onSelectionChange(rpe, selectedOption)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == label) Color.Green else Color.Red,
                )
            ) {
                Text(label)
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

sealed class SessionType {
    data class Lift(
        val category: LiftCategory,
        val variation: Variation? = null,
    ) : SessionType()

    data class Noise(
        val type: NoiseType
    ) : SessionType()
}

enum class LiftCategory {
    FLOOR_PULL,
    SQUAT,
    HORIZONTAL_PRESS,
    VERTICAL_PRESS,
    WAIST_PULL,
    GENERAL,
}

class Variation (
    val rpe: Int,
    val speed: SpeedVariation,
){
    enum class SpeedVariation{
        EXPLOSIVE,
        CONTROLLED,
        SLOW,
    }
}

enum class NoiseType {
    LIFT,
    NOISE,
    ROLLS,
    MOVEMENT,
    UNRACKS
}