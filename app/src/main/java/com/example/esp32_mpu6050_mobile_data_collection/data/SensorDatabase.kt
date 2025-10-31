/* =====================================================================================
 * |                                 Database
 * | -----------------------------------------------------------------------------------
 * | Stores/initializes the instance of the actual database of the application
 * |
 * ===================================================================================== */

package com.example.esp32_mpu6050_mobile_data_collection.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AccelerationEntity::class, AccelerationSessionEntity::class], version = 3, exportSchema = false)
abstract class SensorDatabase : RoomDatabase() {

    abstract fun accelerationDao(): AccelerationDao

    companion object {
        @Volatile
        private var Instance: SensorDatabase? = null

        fun getDatabase(context: Context): SensorDatabase {

            return Instance ?: synchronized(this) {
                Room
                    .databaseBuilder(context, SensorDatabase::class.java, "sensor_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}