package com.example.gymcontrol.ui.cliente.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.OccupancyChart
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun ClienteHomeScreen() {
    ScreenContainer("Inicio") {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(140.dp))
                Text("QR de acceso")
                Text("M-1001", style = MaterialTheme.typography.titleMedium)
            }
        }
        Text("Horario del gimnasio: 06:00 - 22:00")
        OccupancyChart(GymApp.repository.occupancy())
    }
}
