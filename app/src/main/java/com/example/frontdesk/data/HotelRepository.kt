package com.example.frontdesk.data

import com.example.frontdesk.model.Hotel
import com.example.frontdesk.model.RoomType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class StayResult {
    data class Success(val stay: Stay) : StayResult()
    data class Error(val message: String) : StayResult()
}

class HotelRepository(private val dao: StayDao) {
    val stays: Flow<List<Stay>> = dao.observeAll()

    // One change at a time. Without it two quick saves could both see room 101 as free
    private val lock = Mutex()

    private suspend fun hotel() = Hotel(dao.getAll())

    suspend fun checkIn(name: String, nights: Int, type: RoomType, vehicleType: String?, plate: String?): StayResult = lock.withLock {
        val hotel = hotel()
        val room = hotel.findFreeRoom(type) ?: return@withLock StayResult.Error("No free ${type.label} rooms")

        var spot: Int? = null
        if (vehicleType != null && plate != null) {
            if (hotel.isParked(plate)) return@withLock StayResult.Error("$plate is already parked")
            spot = hotel.findFreeSpot() ?: return@withLock StayResult.Error("The parking lot is full")
        }

        val stay = Stay(
            guestName = name,
            nights = nights,
            roomNumber = room.number,
            roomType = type,
            vehicleType = vehicleType,
            plate = plate,
            parkingSpot = spot,
        )
        val id = dao.insert(stay)
        StayResult.Success(stay.copy(id = id))
    }

    // The room stays the same; name, nights and the car can change
    suspend fun update(id: Long, name: String, nights: Int, vehicleType: String?, plate: String?): StayResult = lock.withLock {
        val old = dao.getById(id)
        if (old == null || old.status != StayStatus.CHECKED_IN) return@withLock StayResult.Error("This guest is not checked in")
        val hotel = hotel()

        var spot: Int? = null
        if (vehicleType != null && plate != null) {
            if (hotel.isParked(plate, exceptId = id)) return@withLock StayResult.Error("$plate is already parked")
            spot = old.parkingSpot ?: hotel.findFreeSpot() ?: return@withLock StayResult.Error("The parking lot is full")
        }

        val stay = old.copy(guestName = name, nights = nights, vehicleType = vehicleType, plate = plate, parkingSpot = spot)
        dao.update(stay)
        StayResult.Success(stay)
    }

    suspend fun checkOut(id: Long): StayResult = lock.withLock {
        val stay = dao.getById(id)
        if (stay == null || stay.status != StayStatus.CHECKED_IN) return@withLock StayResult.Error("This guest is not checked in")
        val done = stay.copy(status = StayStatus.CHECKED_OUT, checkedOutAt = System.currentTimeMillis())
        dao.update(done)
        StayResult.Success(done)
    }

    // Undo a check-out. Works only if the room and a parking spot are still free
    suspend fun restore(id: Long): StayResult = lock.withLock {
        val stay = dao.getById(id)
        if (stay == null || stay.status != StayStatus.CHECKED_OUT) return@withLock StayResult.Error("This stay is not in the history")
        val hotel = hotel()
        if (!hotel.isRoomFree(stay.roomNumber)) {
            return@withLock StayResult.Error("Room ${stay.roomNumber} is taken by another guest")
        }

        var spot: Int? = null
        if (stay.plate != null) {
            if (hotel.isParked(stay.plate)) return@withLock StayResult.Error("${stay.plate} is already parked")
            spot = stay.parkingSpot?.takeIf { hotel.isSpotFree(it) }
                ?: hotel.findFreeSpot()
                ?: return@withLock StayResult.Error("The parking lot is full")
        }

        val restored = stay.copy(status = StayStatus.CHECKED_IN, checkedOutAt = null, parkingSpot = spot)
        dao.update(restored)
        StayResult.Success(restored)
    }

    suspend fun delete(stay: Stay) {
        lock.withLock { dao.delete(stay) }
    }
}
