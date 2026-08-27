package com.example.esp32_mpu6050_mobile_data_collection.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.FullIMURawEntity
import com.example.esp32_mpu6050_mobile_data_collection.ui.screen.ENTITY_TO_DISPLAY_PER_SECTION

val ENTITY_TO_DISPLAY_PER_SECTION = 5

@Composable
fun EntityDataScreen(
    fullIMURawValues: List<FullIMURawEntity>,
    onBackButtonPress: () -> Unit,
) {
    val sessions = fullIMURawValues.groupBy { it.sessionId }

    Column(
        modifier = Modifier.padding(14.dp)
    ) {
        Button(
            onClick = onBackButtonPress
        ) {
            Text("Back")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            sessions.forEach { (sessionId, entities) ->

                item {
                    Text(
                        text = "Session $sessionId",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray)
                            .padding(8.dp)
                    )
                }

                items(entities.take(ENTITY_TO_DISPLAY_PER_SECTION)) { entity ->
                    DataItem(entity)

                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        DividerDefaults.color
                    )
                }
            }
        }
    }
}

@Composable
fun DataItem(
    entity: FullIMURawEntity
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp) // give it some vertical space
    ) {
        Text(text = "%.2f".format(entity.ax.toFloat()))
        VerticalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        Text(text = "%.2f".format(entity.ay.toFloat()))
        VerticalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        Text(text = "%.2f".format(entity.az.toFloat()))
        VerticalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    }
}
