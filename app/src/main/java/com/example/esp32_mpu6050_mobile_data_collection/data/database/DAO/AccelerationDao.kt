package com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO

import androidx.room.Dao
import androidx.room.Query
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.AccelerationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccelerationDao : BaseSessionDao<AccelerationEntity> {
    @Query("SELECT * from acceleration ORDER BY id ASC")
    override fun getAllItems(): Flow<List<AccelerationEntity>>

    @Query("SELECT * from acceleration WHERE id = :id")
    override fun getItem(id: Int): Flow<AccelerationEntity>

    @Query("DELETE FROM acceleration")
    suspend fun deleteAllAcceleration()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'acceleration'")
    suspend fun resetAccelerationAutoIncrementCounter()

    override suspend fun cleanDatabase() {
        deleteAllAcceleration()
        deleteAllSessions()
        resetAutoIncrementCounter()
        resetAccelerationAutoIncrementCounter()
    }
}
