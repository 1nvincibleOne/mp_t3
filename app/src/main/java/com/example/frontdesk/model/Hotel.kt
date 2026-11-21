package com.example.frontdesk.model

import com.example.frontdesk.data.Stay
import com.example.frontdesk.data.StayStatus

enum class RoomType(val label: String, val nightlyRate: Int) {
    SINGLE("Single", 15000),
    DOUBLE("Double", 22000),
    SUITE("Suite", 40000),
}

data class Room(val number: Int, val type: RoomType)

data class Bill(val roomCost: Int, val parkingCost: Int) {
    val total: Int
        get() = roomCost + parkingCost
}

val hotelRooms = listOf(
    Room(101, RoomType.SINGLE), Room(102, RoomType.SINGLE), Room(103, RoomType.SINGLE),
    Room(201, RoomType.DOUBLE), Room(202, RoomType.DOUBLE),
    Room(301, RoomType.SUITE),
)

const val PARKING_CAPACITY = 3

fun createVehicle(type: String, plate: String, owner: String): Vehicle = when (type) {
    "Motorcycle" -> Motorcycle(plate, owner)
    "Truck" -> Truck(plate, owner)
    else -> Car(plate, owner)
}

// Guests' cars are billed for the full stay, 24 hours per night
fun parkingCost(vehicle: Vehicle?, nights: Int): Int = vehicle?.fee(nights * 24) ?: 0

// What is taken and what is free, worked out from the saved stays
class Hotel(stays: List<Stay>) {
    private val current = stays.filter { it.status == StayStatus.CHECKED_IN }

    val freeParking: Int
        get() = PARKING_CAPACITY - current.count { it.parkingSpot != null }

    fun isRoomFree(number: Int): Boolean = current.none { it.roomNumber == number }

    fun freeRooms(type: RoomType): Int = hotelRooms.count { it.type == type && isRoomFree(it.number) }

    fun findFreeRoom(type: RoomType): Room? = hotelRooms.firstOrNull { it.type == type && isRoomFree(it.number) }

    fun isSpotFree(spot: Int): Boolean = current.none { it.parkingSpot == spot }

    fun findFreeSpot(): Int? = (1..PARKING_CAPACITY).firstOrNull { isSpotFree(it) }

    // exceptId skips the guest being edited, so their own car doesn't count
    fun isParked(plate: String, exceptId: Long = 0): Boolean = current.any { it.plate == plate && it.id != exceptId }
}
