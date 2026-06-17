package com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO

import androidx.room.Dao
import androidx.room.Query
import com.example.esp32_mpu6050_mobile_data_collection.data.database.LiftCategoryTypeEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.MotionStateTypeEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.SensorDataFormatType
import com.example.esp32_mpu6050_mobile_data_collection.data.database.TempoTypeEntity
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.RawDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RawDataDao : BaseSessionDao<RawDataEntity> {
    @Query("SELECT * from raw_data ORDER BY id ASC")
    override fun getAllItems(): Flow<List<RawDataEntity>>

    @Query("SELECT * from raw_data WHERE id = :id")
    override fun getItem(id: Int): Flow<RawDataEntity>

    @Query("DELETE FROM raw_data")
    suspend fun deleteAllRawData()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'raw_data'")
    suspend fun resetRawDataAutoIncrementCounter()

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertLiftCategoryType(item: LiftCategoryTypeEntity)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertMotionStateType(item: MotionStateTypeEntity)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertSensorDataFormatType(item: SensorDataFormatType)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertTempoType(item: TempoTypeEntity)

    override suspend fun cleanDatabase() {
        deleteAllRawData()
        deleteAllSessions()
        resetAutoIncrementCounter()
        resetRawDataAutoIncrementCounter()
    }
}
