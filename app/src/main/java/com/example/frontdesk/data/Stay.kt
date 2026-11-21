package com.example.frontdesk.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.frontdesk.model.Bill
import com.example.frontdesk.model.RoomType
import com.example.frontdesk.model.Vehicle
import com.example.frontdesk.model.createVehicle
import com.example.frontdesk.model.parkingCost

enum class StayStatus { CHECKED_IN, CHECKED_OUT }

// One guest stay. Current guests and history are the same table with a different status
@Entity(tableName = "stays")
data class Stay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val guestName: String,
    val nights: Int,
    val roomNumber: Int,
    val roomType: RoomType,
    val vehicleType: String? = null,
    val plate: String? = null,
    val parkingSpot: Int? = null,
    val status: StayStatus = StayStatus.CHECKED_IN,
    val checkedInAt: Long = System.currentTimeMillis(),
    val checkedOutAt: Long? = null,
) {
    val vehicle: Vehicle?
        get() = vehicleType?.let { createVehicle(it, plate.orEmpty(), guestName) }

    val bill: Bill
        get() = Bill(nights * roomType.nightlyRate, parkingCost(vehicle, nights))
}
