package com.example.esp32_mpu6050_mobile_data_collection.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rawdata",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = CASCADE, // Deleting parent deletes dependents
            onUpdate = CASCADE, // Updating parent deletes dependents
        )
    ],
    indices = [Index(value = ["sessionId"])]
    // TODO() : create index for sessionID
)
data class RawDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Long, // Foreign Key
    val ax: Float,
    val ay: Float,
    val az: Float,

    val gx: Float,
    val gy: Float,
    val gz: Float,
)
