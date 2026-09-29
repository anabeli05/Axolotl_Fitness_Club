package com.example.gymcontrol.ui.recepcion.solicitudes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun SolicitudesScreen() {
    val requests = GymApp.repository.requests()
    val resolved = remember { mutableStateMapOf<Int, String>() }

    ScreenContainer("Solicitudes") {
        Text("Pendientes: ${requests.count { resolved[it.id] == null }}")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(requests) { req ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(req.clientName, style = MaterialTheme.typography.titleMedium)
                        Text("Membresía: ${req.membershipNumber}")
                        Text("Instructor: ${req.instructorName}")
                        Text("Costo: $${req.cost}")
                        Text("Fecha: ${req.requestDate}")
                        Text("Estado: ${resolved[req.id] ?: "PENDIENTE"}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { resolved[req.id] = "APROBADA" }) { Text("Aprobar") }
                            OutlinedButton(onClick = { resolved[req.id] = "RECHAZADA" }) { Text("Rechazar") }
                        }
                    }
                }
            }
        }
    }
}
