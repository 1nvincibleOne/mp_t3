package com.example.frontdesk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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

@Composable
fun HistoryScreen(
    state: FrontDeskUiState,
    snackbarHostState: SnackbarHostState,
    onRestore: (id: Long) -> Unit,
    onDelete: (Stay) -> Unit,
    onBack: () -> Unit,
) {
    val roomRevenue = state.history.sumOf { it.bill.roomCost }
    val parkingRevenue = state.history.sumOf { it.bill.parkingCost }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = { BackTopBar(title = "History", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatsCard(
                    listOf(
                        "Stays" to "${state.history.size}",
                        "Rooms" to "$roomRevenue",
                        "Parking" to "$parkingRevenue",
                    )
                )
            }
            if (state.history.isEmpty()) {
                item { EmptyState("No checked-out guests yet") }
            }
            items(state.history, key = { it.id }) { stay ->
                HistoryCard(stay = stay, onRestore = { onRestore(stay.id) }, onDelete = { deleteId = stay.id })
            }
        }

        state.history.find { it.id == deleteId }?.let { stay ->
            ConfirmDialog(
                title = "Delete this record?",
                text = "The stay of ${stay.guestName} and its ${stay.bill.total} KZT are removed from the history.",
                confirmText = "Delete",
                onConfirm = {
                    deleteId = null
                    onDelete(stay)
                },
                onDismiss = { deleteId = null },
            )
        }
    }
}

@Composable
fun HistoryCard(stay: Stay, onRestore: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stay.guestName, style = MaterialTheme.typography.titleMedium)
            Text("Room ${stay.roomNumber} (${stay.roomType.label}) · ${stay.nights} nights")
            stay.checkedOutAt?.let {
                Text("Checked out ${formatTime(it)}", style = MaterialTheme.typography.bodySmall)
            }
            BillLine("Paid", stay.bill.total, bold = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onRestore) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete record")
                }
            }
        }
    }
}
