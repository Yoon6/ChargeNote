package com.daeyoon.chargenote.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "car")
data class Car(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name = "nickname") val nickname: String?,
    @ColumnInfo(name = "number_plate") val numberPlate: String?,
    @ColumnInfo(name = "mileage") val mileage: Int?,
    @ColumnInfo(name = "capacity") val capacity: Float?,
    @ColumnInfo(name = "initial_battery") val initialBattery: Int?
)
