/* =====================================================================================
 * |                           DAO Item for Acceleration
 * | -----------------------------------------------------------------------------------
 * | DAO : Interface class which contains the methods (which will be implemented by room automatically
 * |
 * ===================================================================================== */

package com.example.esp32_mpu6050_mobile_data_collection.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccelerationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: AccelerationItem)

    @Update()
    suspend fun updateItem(item: AccelerationItem)

    @Delete()
    suspend fun deleteItem(id: Int)

    @Query("SELECT * FROM acceleration WHERE id = :id")
    fun getItem(id: Int): Flow<AccelerationItem>

    @Query("SELECT * FROM acceleration")
    fun getAllItems(): Flow<List<AccelerationItem>>
}