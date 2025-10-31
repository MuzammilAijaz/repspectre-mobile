/* =====================================================================================
 * |                                 ROOM DATA ENTITY
 * | -----------------------------------------------------------------------------------
 * | Sensor Item Entity acting as a table for the database
 * |
 * ===================================================================================== */

package com.example.esp32_mpu6050_mobile_data_collection.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = "acceleration",
        foreignKeys = [
                ForeignKey(
                        entity = AccelerationSessionEntity::class,
                        parentColumns = ["sessionId"],
                        childColumns = ["sessionId"],
                        onDelete = CASCADE, // Deleting parent deletes dependents
                        onUpdate = CASCADE, // Updating parent deletes dependents
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