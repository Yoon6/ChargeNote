package com.daeyoon.chargenote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.util.Date

@Dao
interface DrivingRecordDao {
    @Query("SELECT * FROM driving_record WHERE car_id = :id ORDER BY date ASC, uid ASC\n")
    fun getDrivingRecordsByIdOrderByDateAndId(id: Int): List<DrivingRecord>

    @Insert
    fun insertAll(vararg drivingRecord: DrivingRecord)

    @Update
    fun updateDrivingRecords(vararg drivingRecord: DrivingRecord)

    @Delete
    fun delete(drivingRecord: DrivingRecord)

    // 바로 전 날짜의 기록 (같은 날짜면 uid가 작은 것 중에서)
    @Query("""
        SELECT * FROM driving_record
        WHERE car_id = :carId AND date <= :date
        ORDER BY date DESC, uid DESC
        LIMIT 1
    """)
    suspend fun getPreviousRecord(carId: Int, date: Date): DrivingRecord?

    // 바로 다음 날짜의 기록 (같은 날짜면 uid가 큰 것 중에서)
    @Query("""
        SELECT * FROM driving_record
        WHERE car_id = :carId AND date > :date
        ORDER BY date ASC, uid ASC
        LIMIT 1
    """)
    suspend fun getNextRecord(carId: Int, date: Date): DrivingRecord?

}