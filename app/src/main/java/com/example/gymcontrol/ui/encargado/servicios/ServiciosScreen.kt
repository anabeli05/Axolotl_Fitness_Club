package com.example.gymcontrol.ui.encargado.servicios

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
fun ServiciosScreen() {
    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    ScreenContainer("Servicios") {
        Button(onClick = { showForm = !showForm }) { Text("Agregar servicio") }

        if (showForm) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(duration, { duration = it }, label = { Text("Duración") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(price, { price = it }, label = { Text("Precio") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { showForm = false }) { Text("Guardar") }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GymApp.repository.services()) { service ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(service.name, style = MaterialTheme.typography.titleMedium)
                        Text("Duración: ${service.duration}")
                        Text("Precio: $${service.price}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {}) { Text("Editar") }
                            OutlinedButton(onClick = {}) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }
}
