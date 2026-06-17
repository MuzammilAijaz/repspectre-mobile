package com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO

import androidx.room.Dao
import androidx.room.Query
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.QuaternionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuaternionDao : BaseSessionDao<QuaternionEntity> {
    @Query("SELECT * from quaternion ORDER BY id ASC")
    override fun getAllItems(): Flow<List<QuaternionEntity>>

    @Query("SELECT * from quaternion WHERE id = :id")
    override fun getItem(id: Int): Flow<QuaternionEntity>

    @Query("DELETE FROM quaternion")
    suspend fun deleteAllQuaternions()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'quaternion'")
    suspend fun resetQuaternionAutoIncrementCounter()

    override suspend fun cleanDatabase() {
        deleteAllQuaternions()
        deleteAllSessions()
        resetAutoIncrementCounter()
        resetQuaternionAutoIncrementCounter()
    }
}
