package com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO

import androidx.room.Dao
import androidx.room.Query
import com.example.esp32_mpu6050_mobile_data_collection.data.database.LiftCategoryTypeEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.MotionStateTypeEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.SensorDataFormatType
import com.example.esp32_mpu6050_mobile_data_collection.data.database.TempoTypeEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.FullIMURawEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FullIMURawDao : BaseSessionDao<FullIMURawEntity> {
    @Query("SELECT * from full_imu_raw ORDER BY id ASC")
    override fun getAllItems(): Flow<List<FullIMURawEntity>>

    @Query("SELECT * from full_imu_raw WHERE id = :id")
    override fun getItem(id: Int): Flow<FullIMURawEntity>

    @Query("DELETE FROM full_imu_raw")
    suspend fun deleteAllFullIMURaw()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'full_imu_raw'")
    suspend fun resetFullIMURawAutoIncrementCounter()

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertLiftCategoryType(item: LiftCategoryTypeEntity)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertMotionStateType(item: MotionStateTypeEntity)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertSensorDataFormatType(item: SensorDataFormatType)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertTempoType(item: TempoTypeEntity)

    override suspend fun cleanDatabase() {
        deleteAllFullIMURaw()
        deleteAllSessions()
        resetAutoIncrementCounter()
        resetFullIMURawAutoIncrementCounter()
    }
}
