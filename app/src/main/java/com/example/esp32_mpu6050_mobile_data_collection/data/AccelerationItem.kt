/* =====================================================================================
 * |                                 ROOM DATA ENTITY
 * | -----------------------------------------------------------------------------------
 * | Sensor Item Entity acting as a table for the database
 * |
 * ===================================================================================== */

package com.example.esp32_mpu6050_mobile_data_collection.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "acceleration")
data class AccelerationItem(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val x: Float,
        val y: Float,
        val z: Float,
)