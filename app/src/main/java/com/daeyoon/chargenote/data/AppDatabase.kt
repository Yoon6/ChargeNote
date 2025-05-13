package com.daeyoon.chargenote.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Car::class, DrivingRecord::class], version = 1)
@TypeConverters(DateTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun drivingRecordDao(): DrivingRecordDao
}