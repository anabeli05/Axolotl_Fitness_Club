package com.example.gymcontrol.ui.instructor.clientes

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
fun MisClientesScreen() {
    var search by remember { mutableStateOf("") }
    val clients = GymApp.repository.instructorClients().filter {
        it.active && (it.clientName.contains(search, true) || it.goal.contains(search, true))
    }

    ScreenContainer("Mis clientes") {
        OutlinedTextField(search, { search = it }, label = { Text("Buscar") }, modifier = Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(clients) { client ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(client.clientName, style = MaterialTheme.typography.titleMedium)
                        Text("Días: ${client.days}")
                        Text("Objetivo: ${client.goal}")
                        OutlinedButton(onClick = {}) { Text("Editar") }
                    }
                }
            }
        }
    }
}
