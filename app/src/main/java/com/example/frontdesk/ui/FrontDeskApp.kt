package com.example.frontdesk.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun FrontDeskApp(viewModel: FrontDeskViewModel = viewModel(factory = FrontDeskViewModel.Factory)) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Shows each message once. "Undo" after a check-out puts the guest back
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message.text,
            actionLabel = if (message.undoId != null) "Undo" else null,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed && message.undoId != null) {
            viewModel.restore(message.undoId)
        }
        viewModel.messageShown()
    }

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                state = state,
                snackbarHostState = snackbarHostState,
                onFilterChange = viewModel::setFilter,
                onGuestClick = { id -> navController.navigate("guest/$id") },
                onCheckInClick = {
                    viewModel.startForm()
                    navController.navigate("checkin")
                },
                onHistoryClick = { navController.navigate("history") },
            )
        }
        composable("checkin") {
            GuestFormScreen(
                state = state,
                editing = null,
                onPlateChange = viewModel::setPlate,
                onScanPlate = viewModel::scanPlate,
                onSave = { name, nights, type, vehicleType ->
                    viewModel.saveGuest(null, name, nights, type, vehicleType) { navController.navigateUp() }
                },
                onBack = { navController.navigateUp() },
            )
        }
        composable(
            route = "guest/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0
            GuestScreen(
                stay = state.guests.find { it.id == id },
                snackbarHostState = snackbarHostState,
                onEdit = { stay ->
                    viewModel.startForm(stay.plate.orEmpty())
                    navController.navigate("edit/$id")
                },
                onCheckOut = {
                    viewModel.checkOut(id)
                    navController.navigateUp()
                },
                onDelete = { stay ->
                    viewModel.delete(stay)
                    navController.navigateUp()
                },
                onBack = { navController.navigateUp() },
            )
        }
        composable(
            route = "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0
            state.guests.find { it.id == id }?.let { stay ->
                GuestFormScreen(
                    state = state,
                    editing = stay,
                    onPlateChange = viewModel::setPlate,
                    onScanPlate = viewModel::scanPlate,
                    onSave = { name, nights, type, vehicleType ->
                        viewModel.saveGuest(id, name, nights, type, vehicleType) { navController.navigateUp() }
                    },
                    onBack = { navController.navigateUp() },
                )
            }
        }
        composable("history") {
            HistoryScreen(
                state = state,
                snackbarHostState = snackbarHostState,
                onRestore = viewModel::restore,
                onDelete = viewModel::delete,
                onBack = { navController.navigateUp() },
            )
        }
    }
}
