package com.example.gymcontrol.ui.instructor.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun InstructorPerfilScreen() {
    var capacity by remember { mutableStateOf("12") }
    ScreenContainer("Perfil instructor") {
        Text("Marco Díaz", style = MaterialTheme.typography.headlineSmall)
        Text("Correo: marco@gym.com")
        Text("Teléfono: 3124444444")
        Text("Costo asesoría: $350.00")
        OutlinedTextField(
            capacity,
            { capacity = it.filter(Char::isDigit) },
            label = { Text("Capacidad máxima de com.example.gymcontrol.ui.encargado.clientes") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {}) { Text("Guardar") }
    }
}
