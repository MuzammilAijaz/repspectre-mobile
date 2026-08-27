package com.example.esp32_mpu6050_mobile_data_collection.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository.Session
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

        if (typeOfSessionSelection == SessionType.LIFT) {
            Text(text = "Lift Categories", modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp))
            val currentCategory = appViewModel.getCurrentLiftCategory()
            SessionCategorySelectionRow(
                categories = LiftCategories.entries,
                selectedCategory = currentCategory
            ) { selectedOption ->
                appViewModel.setLiftCategory(selectedOption)
            }
            Spacer(modifier = Modifier.padding(vertical = 5.dp))

            Text(text = "Lift Context", modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp))
            val currentTempo = appViewModel.getCurrentLiftTempo()
            val currentRpe = appViewModel.getCurrentRpe()
            SessionLiftContextSelectionRow(
                tempos = Tempos.entries,
                selectedTempo = currentTempo,
                rpe = currentRpe,
                onTempoChange = { appViewModel.setLiftTempo(it) },
                onRpeChange = { appViewModel.setRPE(it) }
            )
            Spacer(modifier = Modifier.padding(vertical = 5.dp))
        }

        if (typeOfSessionSelection == SessionType.LIFT_SPECIFIC_NOISE) {
            Text(text = "Lift Categories", modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp))
            val currentCategory = appViewModel.getCurrentLiftCategory()
            SessionCategorySelectionRow(
                categories = LiftCategories.entries,
                selectedCategory = currentCategory
            ) { selectedOption ->
                appViewModel.setLiftCategory(selectedOption)
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
                motionStates = MotionStates.entries.filter { !it.requiresLiftContext },
                selectedState = sessionConfig.motionState
            ) { selectedOption ->
                appViewModel.setMotionState(selectedOption)
            }
            Spacer(modifier = Modifier.padding(vertical = 5.dp))
        }
        else if (typeOfSessionSelection == SessionType.LIFT_SPECIFIC_NOISE){
            // only display states which require lift context if its lift session
            SessionMotionStateSelectionRow(
                motionStates = MotionStates.entries.filter { it.requiresLiftContext },
                selectedState = sessionConfig.motionState
            ) { selectedOption ->
                appViewModel.setMotionState(selectedOption)
            }
            Spacer(modifier = Modifier.padding(vertical = 5.dp))
        }

        Text(text = "Data Formats", modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp))
        SessionSensorDataFormatSelectionRow(
            dataFormats = SensorDataFormats.entries,
            selectedFormat = sessionConfig.sensorDataFormat
        ) { selectedOption ->
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
fun SessionCategorySelectionRow(
    categories: List<LiftCategories>,
    selectedCategory: LiftCategories?,
    onSelectionChange: (LiftCategories) -> Unit
) {
    SelectionDropdown(
        items = categories,
        selectedItem = selectedCategory,
        onSelectionChange = onSelectionChange,
        label = "Lift Category",
        itemLabel = { it.name },
    )
}

@Composable
fun SessionMotionStateSelectionRow(
    motionStates: List<MotionStates>,
    selectedState: MotionStates?,
    onSelectionChange: (MotionStates) -> Unit
) {
    SelectionDropdown(
        items = motionStates,
        selectedItem = selectedState,
        onSelectionChange = onSelectionChange,
        label = "Motion State",
        itemLabel = { it.name },
        itemDescription = { it.description }
    )
}

@Composable
fun SessionLiftContextSelectionRow(
    tempos: List<Tempos>,
    selectedTempo: Tempos?,
    rpe: Int,
    onTempoChange: (Tempos) -> Unit,
    onRpeChange: (Int) -> Unit
) {
    var rpeText by remember(rpe) { mutableStateOf(rpe.toString()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelectionDropdown(
            items = tempos,
            selectedItem = selectedTempo,
            onSelectionChange = onTempoChange,
            label = "Tempo",
            itemLabel = { it.name }
        )

        OutlinedTextField(
            value = rpeText,
            onValueChange = { newVal ->
                if (newVal.isEmpty()) {
                    rpeText = ""
                } else {
                    val rpeInt = newVal.toIntOrNull()
                    if (rpeInt != null && rpeInt in 0..10) {
                        rpeText = newVal
                        onRpeChange(rpeInt)
                    }
                }
            },
            label = { Text("RPE (0-10)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun SessionSensorDataFormatSelectionRow(
    dataFormats: List<SensorDataFormats>,
    selectedFormat: SensorDataFormats?,
    onSelectionChange: (SensorDataFormats) -> Unit
) {
    SelectionDropdown(
        items = dataFormats,
        selectedItem = selectedFormat,
        onSelectionChange = onSelectionChange,
        label = "Data Format",
        itemLabel = { it.name }
    )
}

enum class SessionType {
    LIFT,
    NOISE,
    LIFT_SPECIFIC_NOISE,
}

//==============================================================================
// | Generic Composable
//==============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectionDropdown(
    items: List<T>,
    selectedItem: T?,
    onSelectionChange: (T) -> Unit,
    label: String,
    itemLabel: (T) -> String,
    itemDescription: ((T) -> String)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var descriptionItem by remember { mutableStateOf<T?>(null) }

    Box(
        modifier = modifier
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedItem?.let(itemLabel) ?: "Select $label",
                onValueChange = {},
                readOnly = true,
                label = {
                    Text(label)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                ),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEach { item ->

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = itemLabel(item),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            if (selectedItem == item) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        },
                        trailingIcon = {
                            if (itemDescription != null) {
                                IconButton(
                                    onClick = {
                                        descriptionItem = item
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Description"
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelectionChange(item)
                            expanded = false
                        }
                    )
                }
            }
        }

        descriptionItem?.let { item ->
            AlertDialog(
                onDismissRequest = {
                    descriptionItem = null
                },
                title = {
                    Text(itemLabel(item))
                },
                text = {
                    Text(itemDescription!!(item))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            descriptionItem = null
                        }
                    ) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun SessionManagerPreview() {
    Esp32mpu6050mobiledatacollectionTheme {
        val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
        SessionManagerScreen(viewModel)
    }
}
