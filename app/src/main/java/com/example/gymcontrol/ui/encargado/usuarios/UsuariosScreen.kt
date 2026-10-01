package com.example.gymcontrol.ui.encargado.usuarios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun UsuariosScreen(onNewUser: () -> Unit) {
    ScreenContainer("Usuarios") {
        Button(onClick = onNewUser) { Text("Agregar usuario") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GymApp.repository.users().filter { it.membershipNumber == null }) { user ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(user.name, style = MaterialTheme.typography.titleMedium)
                        Text("Rol: ${user.role}")
                        Text("Correo: ${user.email}")
                        Text("Estado: ${user.status}")
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
