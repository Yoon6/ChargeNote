package com.daeyoon.chargenote

import android.app.Application
import androidx.room.Room
import com.daeyoon.chargenote.data.AppDatabase

class MyApp : Application() {

    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "chargenote-db"
        ).build()
    }

}