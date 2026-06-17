/* =====================================================================================
 * |                                 Container
 * | -----------------------------------------------------------------------------------
 * | Container class used to hold all global dependencies/variables of the application
 * |    : ex. Repository
 * | -> Holds the instance of repository instantiated with the DAO object
 * |
 * ====================================================================================== */

package com.example.esp32_mpu6050_mobile_data_collection.data

import android.content.Context
import com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository.AccelerationRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository.QuaternionRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository.RawDataRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.database.SensorDatabase

class AppContainer(private val context: Context) {

    val accelerationRepository: AccelerationRepository by lazy {
        AccelerationRepository(SensorDatabase.getDatabase(context).accelerationDao())
    }

    val quaternionRepository: QuaternionRepository by lazy {
        QuaternionRepository(SensorDatabase.getDatabase(context).quaternionDao())
    }

    val bleRepository: AppBleRepository by lazy {
        AppBleRepository(context)
    }

    val rawDataRepository: RawDataRepository by lazy {
        RawDataRepository(SensorDatabase.getDatabase(context).rawDataDao())
    }
}
