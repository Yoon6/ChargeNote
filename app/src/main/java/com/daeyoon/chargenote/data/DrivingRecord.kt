package com.daeyoon.chargenote.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity(
    tableName = "driving_record",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["uid"],
            childColumns = ["car_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["car_id"])]
)
@Parcelize
data class DrivingRecord(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    @ColumnInfo(name = "car_id") val carId: Int?,
    @ColumnInfo(name = "total_amount") val totalAmount: Int?,
    @ColumnInfo(name = "charge_amount") val chargeAmount: Float?,
    @ColumnInfo(name = "current_mileage") val currentMileage: Int?,
    @ColumnInfo(name = "current_battery") val currentBattery: Int?,
    @ColumnInfo(name = "date") val date: Date?,
) : Parcelable
