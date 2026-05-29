package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.RoxouDao
import com.example.data.entities.*

@Database(
    entities = [
        Profile::class,
        DriverSettings::class,
        DriverStatus::class,
        RideRequest::class,
        RideMessage::class,
        DriverPartner::class,
        DriverLiveLocation::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roxouDao(): RoxouDao
}
