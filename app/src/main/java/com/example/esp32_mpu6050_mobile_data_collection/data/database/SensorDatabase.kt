package com.example.esp32_mpu6050_mobile_data_collection.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO.AccelerationDao
import com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO.FullIMURawDao
import com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO.QuaternionDao
import com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO.RawDataDao
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.AccelerationEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.FullIMURawEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.LiftContextEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.QuaternionEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.RawDataEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.SessionEntity

@Database(entities = [AccelerationEntity::class, SessionEntity::class, QuaternionEntity::class, RawDataEntity::class, FullIMURawEntity::class, LiftContextEntity::class, LiftCategoryTypeEntity::class, MotionStateTypeEntity::class, SensorDataFormatType::class, TempoTypeEntity::class], version = 17, exportSchema = false)
abstract class SensorDatabase : RoomDatabase() {

    abstract fun accelerationDao(): AccelerationDao
    abstract fun quaternionDao(): QuaternionDao
    abstract fun rawDataDao(): RawDataDao
    abstract fun fullIMURawDao(): FullIMURawDao

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
