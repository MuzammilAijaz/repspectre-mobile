package com.example.esp32_mpu6050_mobile_data_collection.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.AccelerationEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.QuaternionEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.RawDataEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.SessionEntity

@Database(entities = [AccelerationEntity::class, SessionEntity::class, QuaternionEntity::class, RawDataEntity::class], version = 9, exportSchema = false)
abstract class SensorDatabase : RoomDatabase() {

    abstract fun accelerationDao(): AccelerationDao
    abstract fun quaternionDao(): QuaternionDao
    abstract fun rawDataDao(): RawDataDao

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