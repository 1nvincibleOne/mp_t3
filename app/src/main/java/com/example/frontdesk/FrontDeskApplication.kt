package com.example.frontdesk

import android.app.Application
import androidx.room.Room
import com.example.frontdesk.data.HotelDatabase
import com.example.frontdesk.data.HotelRepository

// Created once when the app starts, so there is only one database for the whole app
class FrontDeskApplication : Application() {
    val repository: HotelRepository by lazy {
        val database = Room.databaseBuilder(this, HotelDatabase::class.java, "hotel.db").build()
        HotelRepository(database.stayDao())
    }
}
