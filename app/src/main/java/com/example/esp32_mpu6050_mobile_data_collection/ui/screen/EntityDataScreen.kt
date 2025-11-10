package com.example.esp32_mpu6050_mobile_data_collection.ui.screen

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
import androidx.compose.ui.unit.dp
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.QuaternionEntity

@Composable
fun EntityDataScreen(
    quaternionEntities: List<QuaternionEntity>,
    onBackButtonPress: () -> Unit,
) {
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
            items(quaternionEntities) { entity ->
                DataItem(entity)
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }
        }
    }
}

@Composable
fun DataItem(
    entity: QuaternionEntity
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp) // give it some vertical space
    ) {
        Text(text = "%.2f".format(entity.x))
        VerticalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        Text(text = "%.2f".format(entity.y))
        VerticalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        Text(text = "%.2f".format(entity.z))
        VerticalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        Text(text = "%.2f".format(entity.w))
        VerticalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    }
}