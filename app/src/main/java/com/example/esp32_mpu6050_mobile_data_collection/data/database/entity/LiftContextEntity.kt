package com.example.esp32_mpu6050_mobile_data_collection.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lift_context",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class LiftContextEntity(
    @PrimaryKey val sessionId: Long,
    val liftCategory: String,
    val tempo: String,
    val rpe: Int?
)
