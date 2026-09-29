package com.example.gymcontrol.ui.recepcion.clientes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun NuevoClienteScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var membership by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    ScreenContainer("Nuevo cliente") {
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(membership, { membership = it }, label = { Text("Número de membresía") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Guardar cliente") }
    }
}
