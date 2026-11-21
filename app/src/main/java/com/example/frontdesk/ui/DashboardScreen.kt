package com.example.frontdesk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.frontdesk.model.RoomType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: FrontDeskUiState,
    snackbarHostState: SnackbarHostState,
    onFilterChange: (String) -> Unit,
    onGuestClick: (id: Long) -> Unit,
    onCheckInClick: () -> Unit,
    onHistoryClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Front Desk") },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Filled.History, contentDescription = "History")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCheckInClick,
                icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                text = { Text("Check in") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatsCard(
                    listOf(
                        "Free rooms" to "${state.freeRooms.values.sum()} / ${state.totalRooms}",
                        "Parking" to "${state.freeParking} / ${state.parkingCapacity}",
                        "Revenue" to "${state.revenue}",
                    )
                )
            }
            item {
                ChipSelector(
                    options = listOf("All") + RoomType.entries.map { it.label },
                    selected = state.filter,
                    onSelect = onFilterChange,
                )
            }
            if (state.guests.isEmpty()) {
                item { EmptyState("No guests yet. Tap Check in to add the first one") }
            } else if (state.visibleGuests.isEmpty()) {
                item { EmptyState("No guests in ${state.filter} rooms") }
            }
            items(state.visibleGuests, key = { it.id }) { stay ->
                GuestCard(stay = stay, onClick = { onGuestClick(stay.id) })
            }
        }
    }
}
