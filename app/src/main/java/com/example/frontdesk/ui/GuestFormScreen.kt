package com.example.frontdesk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.frontdesk.data.Stay
import com.example.frontdesk.model.Bill
import com.example.frontdesk.model.RoomType
import com.example.frontdesk.model.createVehicle
import com.example.frontdesk.model.parkingCost

// Used for both check-in (editing == null) and editing a guest
@Composable
fun GuestFormScreen(
    state: FrontDeskUiState,
    editing: Stay?,
    onPlateChange: (String) -> Unit,
    onScanPlate: () -> Unit,
    onSave: (name: String, nights: Int, type: RoomType, vehicleType: String?) -> Unit,
    onBack: () -> Unit,
) {
    // Survive rotation; filled from the guest when editing
    var name by rememberSaveable { mutableStateOf(editing?.guestName ?: "") }
    var nights by rememberSaveable { mutableStateOf(editing?.nights ?: 1) }
    var roomType by rememberSaveable { mutableStateOf(editing?.roomType ?: RoomType.SINGLE) }
    var hasCar by rememberSaveable { mutableStateOf(editing?.vehicleType != null) }
    var vehicleType by rememberSaveable { mutableStateOf(editing?.vehicleType ?: vehicleTypes.first()) }

    val vehicle = if (hasCar) createVehicle(vehicleType, state.plate, name) else null
    val bill = Bill(nights * roomType.nightlyRate, parkingCost(vehicle, nights))

    Scaffold(topBar = { BackTopBar(title = if (editing == null) "Check in" else "Edit guest", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Guest name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Stepper(label = "Nights", value = nights, onValueChange = { nights = it })

            Text("Room", style = MaterialTheme.typography.titleMedium)
            if (editing == null) {
                ChipSelector(
                    options = RoomType.entries.map { "${it.label} (${state.freeRooms[it] ?: 0} free)" },
                    selected = "${roomType.label} (${state.freeRooms[roomType] ?: 0} free)",
                    onSelect = { option -> roomType = RoomType.entries.first { option.startsWith(it.label) } },
                )
            } else {
                Text("${editing.roomType.label} room ${editing.roomNumber}")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasCar, onCheckedChange = { hasCar = it })
                Text("Guest has a car (${state.freeParking} spots free)")
            }
            if (hasCar) {
                OutlinedTextField(
                    value = state.plate,
                    onValueChange = onPlateChange,
                    label = { Text("Plate number") },
                    singleLine = true,
                    trailingIcon = {
                        if (state.scanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            IconButton(onClick = onScanPlate) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = "Scan plate")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                ChipSelector(options = vehicleTypes, selected = vehicleType, onSelect = { vehicleType = it })
            }

            BillCard(bill = bill, nights = nights, nightlyRate = roomType.nightlyRate)

            state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { onSave(name, nights, roomType, if (hasCar) vehicleType else null) },
                enabled = !state.scanning && !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (editing == null) "Check in" else "Save changes")
            }
        }
    }
}
