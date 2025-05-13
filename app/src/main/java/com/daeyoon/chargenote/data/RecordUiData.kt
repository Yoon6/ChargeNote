package com.daeyoon.chargenote.data

data class RecordUiData(
    val uid: Long,
    val efficiency: String,
    val totalAmount: String,
    val tripMileage: String,
    val date: String,
    val originData: DrivingRecord
)
