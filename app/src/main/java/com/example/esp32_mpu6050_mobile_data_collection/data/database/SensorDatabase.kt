package com.example.esp32_mpu6050_mobile_data_collection.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.AccelerationEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.SessionEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.QuaternionEntity

@Database(entities = [AccelerationEntity::class, SessionEntity::class, QuaternionEntity::class], version = 7, exportSchema = false)
abstract class SensorDatabase : RoomDatabase() {

    abstract fun accelerationDao(): AccelerationDao
    abstract fun quaternionDao(): QuaternionDao

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