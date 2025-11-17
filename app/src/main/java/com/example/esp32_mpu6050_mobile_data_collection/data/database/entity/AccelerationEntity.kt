package com.example.esp32_mpu6050_mobile_data_collection.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = "acceleration",
        foreignKeys = [
            ForeignKey(
                entity = SessionEntity::class,
                parentColumns = ["sessionId"],
                childColumns = ["sessionId"],
                onDelete = ForeignKey.Companion.CASCADE, // Deleting parent deletes dependents
                onUpdate = ForeignKey.Companion.CASCADE, // Updating parent deletes dependents
            )
        ],
        indices = [Index(value = ["sessionId"])]
        // TODO() : create index for sessionID
)
data class AccelerationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Long, // Foreign Key
    val x: Float,
    val y: Float,
    val z: Float,
)