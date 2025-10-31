/* =====================================================================================
 * |                           DAO Item for Acceleration
 * | -----------------------------------------------------------------------------------
 * | DAO : Interface class which contains the methods (which will be implemented by room automatically)
 * |    to create sessions and insert sensor values.
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

    // =====================================================================================
    // |                                 Acceleration Session Entity
    // =====================================================================================

    /** Returns the sessionId so it can be used to create [AccelerationEntity] */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionItem(item: AccelerationSessionEntity): Long // Note: using long here to align with how sqlite stores data

    // =====================================================================================
    // |                                Acceleration Entity
    // =====================================================================================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: AccelerationEntity)

    @Update()
    suspend fun updateItem(item: AccelerationEntity)

    @Delete
    suspend fun deleteItem(item: AccelerationEntity)

    @Query("SELECT * FROM acceleration WHERE id = :id")
    fun getItem(id: Int): Flow<AccelerationEntity>

    @Query("SELECT * FROM acceleration")
    fun getAllItems(): Flow<List<AccelerationEntity>>
}