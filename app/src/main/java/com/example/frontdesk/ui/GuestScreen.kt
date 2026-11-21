package com.example.frontdesk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.frontdesk.data.Stay

@Composable
fun GuestScreen(
    stay: Stay?,
    snackbarHostState: SnackbarHostState,
    onEdit: (Stay) -> Unit,
    onCheckOut: () -> Unit,
    onDelete: (Stay) -> Unit,
    onBack: () -> Unit,
) {
    var confirmCheckOut by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { BackTopBar(title = stay?.let { "Room ${it.roomNumber}" } ?: "Guest", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (stay == null) {
            EmptyState("This guest is not checked in", modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val bill = stay.bill
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoomIcon(type = stay.roomType, roomNumber = stay.roomNumber)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stay.guestName, style = MaterialTheme.typography.titleLarge)
                        Text("${stay.roomType.label} room · ${stay.nights} nights")
                        val car = stay.vehicleType?.let { "$it ${stay.plate} · spot ${stay.parkingSpot}" }
                        Text(car ?: "No car")
                        Text("Checked in ${formatTime(stay.checkedInAt)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            BillCard(bill = bill, nights = stay.nights, nightlyRate = stay.roomType.nightlyRate)
            Button(onClick = { confirmCheckOut = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Check out · ${bill.total} KZT")
            }
            OutlinedButton(onClick = { onEdit(stay) }, modifier = Modifier.fillMaxWidth()) {
                Text("Edit")
            }
            TextButton(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete stay")
            }
        }

        if (confirmCheckOut) {
            ConfirmDialog(
                title = "Check out ${stay.guestName}?",
                text = "The guest pays ${bill.total} KZT and the room is freed.",
                confirmText = "Check out",
                onConfirm = {
                    confirmCheckOut = false
                    onCheckOut()
                },
                onDismiss = { confirmCheckOut = false },
            )
        }
        if (confirmDelete) {
            ConfirmDialog(
                title = "Delete this stay?",
                text = "${stay.guestName} is removed without payment. This can't be undone.",
                confirmText = "Delete",
                onConfirm = {
                    confirmDelete = false
                    onDelete(stay)
                },
                onDismiss = { confirmDelete = false },
            )
        }
    }
}
