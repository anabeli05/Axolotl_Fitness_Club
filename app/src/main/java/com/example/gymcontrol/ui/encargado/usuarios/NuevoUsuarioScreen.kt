package com.example.gymcontrol.ui.encargado.usuarios

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun NuevoUsuarioScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.RECEPCION) }
    var cost by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }

    ScreenContainer("Nuevo usuario") {
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Correo asignado") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Contraseña asignada") }, modifier = Modifier.fillMaxWidth())

        Text("Rol")
        Row {
            FilterChip(
                selected = role == UserRole.RECEPCION,
                onClick = { role = UserRole.RECEPCION },
                label = { Text("Recepción") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = role == UserRole.INSTRUCTOR,
                onClick = { role = UserRole.INSTRUCTOR },
                label = { Text("Instructor") }
            )
        }

        if (role == UserRole.INSTRUCTOR) {
            OutlinedTextField(cost, { cost = it }, label = { Text("Costo asesoría") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(capacity, { capacity = it }, label = { Text("Capacidad máxima") }, modifier = Modifier.fillMaxWidth())
        }

        Text("Estado inicial: ACTIVO")
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Guardar usuario") }
    }
}