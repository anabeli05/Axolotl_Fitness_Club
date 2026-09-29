package com.example.gymcontrol.ui.recepcion.clientes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun ClientesScreen(onNewClient: () -> Unit) {
    var search by remember { mutableStateOf("") }
    val clients = GymApp.repository.users()
        .filter { it.role == UserRole.CLIENTE }
        .filter { it.name.contains(search, true) || (it.membershipNumber ?: "").contains(search, true) }

    ScreenContainer("Clientes") {
        OutlinedTextField(search, { search = it }, label = { Text("Buscar cliente") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onNewClient) { Text("Agregar nuevo cliente") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(clients) { client ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(client.name, style = MaterialTheme.typography.titleMedium)
                        Text("Membresía: ${client.membershipNumber}")
                        Text("Registro: ${client.registrationDate}")
                        Text("Estado: ${client.status}")
                        OutlinedButton(onClick = {}) { Text("Renovar") }
                    }
                }
            }
        }
    }
}
