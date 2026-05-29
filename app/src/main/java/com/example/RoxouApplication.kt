package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.database.AppDatabase
import com.example.data.repository.RoxouRepository

class RoxouApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "roxou_database")
            .fallbackToDestructiveMigration()
            .build()
    }
    val repository by lazy {
        RoxouRepository(database.roxouDao(), this)
    }

    override fun onCreate() {
        super.onCreate()
        com.example.data.repository.RoxouNotificationManager.initNotificationChannel(this)
    }
}
