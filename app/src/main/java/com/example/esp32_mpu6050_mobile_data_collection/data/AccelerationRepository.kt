/* =====================================================================================
 * |                    Repository to Manage Acceleration Values
 * | -----------------------------------------------------------------------------------
 * | Communicates with the DAO to retrieve values and return to (ex. ViewModels)
 * |
 * ===================================================================================== */

package com.example.esp32_mpu6050_mobile_data_collection.data

import kotlinx.coroutines.flow.Flow

class AccelerationRepository(
    private val accelerationDao: AccelerationDao
) {
    fun getAllItemsStream(): Flow<List<AccelerationItem>> = accelerationDao.getAllItems()
    fun getItem(id: Int): Flow<AccelerationItem> = accelerationDao.getItem(id)

    suspend fun insertItem(item: AccelerationItem) = accelerationDao.insertItem(item)
}