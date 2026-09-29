package com.example.gymcontrol.ui.cliente.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun ClientePerfilScreen() {
    var name by remember { mutableStateOf("Ana López") }
    var phone by remember { mutableStateOf("3121111111") }
    var email by remember { mutableStateOf("ana@email.com") }
    var newPassword by remember { mutableStateOf("") }

    ScreenContainer("Mi perfil") {
        Text("Membresía: M-1001")
        AssistChip(onClick = {}, label = { Text("ACTIVO") })

        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {}) { Text("Guardar cambios") }

        HorizontalDivider()
        Text("Cambiar contraseña", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(newPassword, { newPassword = it }, label = { Text("Nueva contraseña") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {}) { Text("Actualizar contraseña") }
    }
}
