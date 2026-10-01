package com.example.gymcontrol.ui.cliente.asesorias

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun AsesoriasScreen() {
    val instructors = GymApp.repository.instructors()
    val pending = remember { mutableStateMapOf<Int, Boolean>() }

    ScreenContainer("Asesorías") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(instructors) { instructor ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(instructor.name, style = MaterialTheme.typography.titleMedium)
                            Text("$${"%.2f".format(instructor.cost)}")
                            Text("Cupo: ${instructor.activeClients}/${instructor.maxClients}")
                        }
                        Button(
                            enabled = pending[instructor.id] != true,
                            onClick = { pending[instructor.id] = true }
                        ) {
                            Text(if (pending[instructor.id] == true) "Pendiente" else "Solicitar")
                        }
                    }
                }
            }
        }
    }
}
