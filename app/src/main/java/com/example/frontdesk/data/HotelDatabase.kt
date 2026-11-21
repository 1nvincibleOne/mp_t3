package com.example.frontdesk.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Stay::class], version = 1, exportSchema = false)
abstract class HotelDatabase : RoomDatabase() {
    abstract fun stayDao(): StayDao
}
