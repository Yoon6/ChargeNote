package com.daeyoon.chargenote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CarDao {
    @Query("SELECT * FROM car")
    suspend fun getAll(): List<Car>

    @Query("SELECT * FROM car WHERE uid = :carId LIMIT 1")
    fun getCarById(carId: Int): Car?

    @Insert
    fun insertAll(vararg car: Car)

    @Update
    fun updateCars(vararg car: Car)

    @Delete
    fun delete(car: Car)
}