package com.example.esp32_mpu6050_mobile_data_collection.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session")
data class AccelerationSessionEntity (
    @PrimaryKey(autoGenerate = true) val sessionId: Int = 0,
    val startTime: Long,
    val endTime: Long? = null,
)