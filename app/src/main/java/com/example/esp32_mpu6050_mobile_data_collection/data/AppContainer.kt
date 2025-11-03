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

class AppContainer(private val context: Context) {

    val accelerationRepository: AccelerationRepository by lazy {
        AccelerationRepository(SensorDatabase.getDatabase(context).accelerationDao())
    }

    val bleRepository: AppBleRepository by lazy {
        AppBleRepository(context)
    }
}