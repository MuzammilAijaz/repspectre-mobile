package com.example.esp32_mpu6050_mobile_data_collection.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lift_category_type")
data class LiftCategoryTypeEntity(
    @PrimaryKey val liftCategory: String
)

@Entity(tableName = "motion_state_type")
data class MotionStateTypeEntity(
    @PrimaryKey val motionState: String
)

@Entity(tableName = "sensor_data_format_type")
data class SensorDataFormatType(
    @PrimaryKey val sensorDataFormat: String
)

@Entity(tableName = "tempo_type")
data class TempoTypeEntity(
    @PrimaryKey val tempo: String
)
