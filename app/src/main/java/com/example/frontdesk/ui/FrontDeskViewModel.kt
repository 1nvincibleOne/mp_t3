package com.example.frontdesk.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.frontdesk.FrontDeskApplication
import com.example.frontdesk.data.HotelRepository
import com.example.frontdesk.data.Stay
import com.example.frontdesk.data.StayResult
import com.example.frontdesk.data.StayStatus
import com.example.frontdesk.model.Hotel
import com.example.frontdesk.model.PARKING_CAPACITY
import com.example.frontdesk.model.RoomType
import com.example.frontdesk.model.hotelRooms
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val vehicleTypes = listOf("Car", "Motorcycle", "Truck")

// undoId is set when the snackbar should offer "Undo" for a check-out
data class UserMessage(val text: String, val undoId: Long? = null, val time: Long = System.currentTimeMillis())

data class FrontDeskUiState(
    val stays: List<Stay> = emptyList(),
    val filter: String = "All",
    val plate: String = "",
    val scanning: Boolean = false,
    val saving: Boolean = false,
    val formError: String? = null,
    val message: UserMessage? = null,
) {
    val guests: List<Stay>
        get() = stays.filter { it.status == StayStatus.CHECKED_IN }.sortedBy { it.roomNumber }

    val history: List<Stay>
        get() = stays.filter { it.status == StayStatus.CHECKED_OUT }.sortedByDescending { it.checkedOutAt }

    val visibleGuests: List<Stay>
        get() = if (filter == "All") guests else guests.filter { it.roomType.label == filter }

    val freeRooms: Map<RoomType, Int>
        get() = RoomType.entries.associateWith { Hotel(stays).freeRooms(it) }

    val freeParking: Int
        get() = Hotel(stays).freeParking

    val totalRooms: Int
        get() = hotelRooms.size

    val parkingCapacity: Int
        get() = PARKING_CAPACITY

    val revenue: Int
        get() = history.sumOf { it.bill.total }
}

class FrontDeskViewModel(
    private val repository: HotelRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    // The plate is also kept in SavedStateHandle, so the form gets it back
    // if Android kills the app while it is in the background
    private val _uiState = MutableStateFlow(FrontDeskUiState(plate = savedState["plate"] ?: ""))
    val uiState: StateFlow<FrontDeskUiState> = _uiState.asStateFlow()

    init {
        // Room sends a new list after every insert, update or delete
        viewModelScope.launch {
            repository.stays.collect { stays ->
                _uiState.update { it.copy(stays = stays) }
            }
        }
    }

    private var scanJob: Job? = null

    // Clears the plate and error left from a previous form
    fun startForm(plate: String = "") {
        cancelScan()
        updatePlate(plate)
        _uiState.update { it.copy(formError = null) }
    }

    fun setPlate(plate: String) {
        cancelScan() // a manual edit wins over a pending scan
        updatePlate(plate)
    }

    private fun updatePlate(plate: String) {
        savedState["plate"] = plate
        _uiState.update { it.copy(plate = plate) }
    }

    // Simulates a camera reading the plate of the car at the entrance
    fun scanPlate() {
        if (_uiState.value.scanning) return
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(scanning = true) }
            delay(1500)
            val plate = "${(100..999).random()}${listOf("ABC", "KZT", "AST", "ALA").random()}0${(1..9).random()}"
            _uiState.update { it.copy(scanning = false) }
            updatePlate(plate)
        }
    }

    private fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.update { it.copy(scanning = false) }
    }

    private fun validate(name: String, vehicleType: String?, plate: String): String? = when {
        name.isEmpty() -> "Enter the guest's name"
        name.none { it.isLetter() } -> "The name must have letters"
        !name.all { it.isLetter() || it == ' ' || it == '-' } -> "The name can only have letters, spaces and -"
        vehicleType != null && plate.isEmpty() -> "Enter or scan the car's plate"
        vehicleType != null && !plate.matches(Regex("[A-Z0-9]{4,10}")) -> "The plate must be 4-10 letters or digits"
        else -> null
    }

    // id == null means a new check-in, otherwise the guest with this id is edited.
    // onSaved runs only when the data was saved, so the screen can close
    fun saveGuest(id: Long?, name: String, nights: Int, type: RoomType, vehicleType: String?, onSaved: () -> Unit) {
        if (_uiState.value.saving) return // ignore a second tap while saving
        val cleanName = name.trim()
        val plate = _uiState.value.plate.trim().uppercase()
        val error = validate(cleanName, vehicleType, plate)
        if (error != null) {
            _uiState.update { it.copy(formError = error) }
            return
        }

        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val carPlate = if (vehicleType != null) plate else null
            val result = if (id == null) {
                repository.checkIn(cleanName, nights, type, vehicleType, carPlate)
            } else {
                repository.update(id, cleanName, nights, vehicleType, carPlate)
            }
            when (result) {
                is StayResult.Success -> {
                    val stay = result.stay
                    val spot = stay.parkingSpot?.let { ", parking spot $it" } ?: ""
                    val text = if (id == null) "${stay.guestName} checked into room ${stay.roomNumber}$spot" else "Changes saved"
                    updatePlate("")
                    _uiState.update { it.copy(saving = false, formError = null) }
                    showMessage(UserMessage(text))
                    onSaved()
                }
                is StayResult.Error -> _uiState.update { it.copy(saving = false, formError = result.message) }
            }
        }
    }

    fun checkOut(id: Long) {
        viewModelScope.launch {
            when (val result = repository.checkOut(id)) {
                is StayResult.Success -> {
                    val stay = result.stay
                    showMessage(UserMessage("${stay.guestName} checked out and paid ${stay.bill.total} KZT", undoId = id))
                }
                is StayResult.Error -> showMessage(UserMessage(result.message))
            }
        }
    }

    fun restore(id: Long) {
        viewModelScope.launch {
            when (val result = repository.restore(id)) {
                is StayResult.Success -> {
                    val stay = result.stay
                    showMessage(UserMessage("${stay.guestName} is back in room ${stay.roomNumber}"))
                }
                is StayResult.Error -> showMessage(UserMessage("Can't restore: ${result.message}"))
            }
        }
    }

    fun delete(stay: Stay) {
        viewModelScope.launch {
            repository.delete(stay)
            showMessage(UserMessage("Stay of ${stay.guestName} deleted"))
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(filter = filter) }
    }

    private fun showMessage(message: UserMessage) {
        _uiState.update { it.copy(message = message) }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        // Tells viewModel() how to create this ViewModel with the repository
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FrontDeskApplication
                FrontDeskViewModel(app.repository, createSavedStateHandle())
            }
        }
    }
}
