package com.example.gymcontrol.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.data.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLogin: (UserRole) -> Unit) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.CLIENTE) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Gym Control", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            UserRole.entries.forEachIndexed { index, role ->
                SegmentedButton(
                    selected = selectedRole == role,
                    onClick = { selectedRole = role },
                    shape = SegmentedButtonDefaults.itemShape(index, UserRole.entries.size)
                ) {
                    Text(
                        when (role) {
                            UserRole.CLIENTE -> "Cliente"
                            UserRole.RECEPCION -> "Recepción"
                            UserRole.INSTRUCTOR -> "Instructor"
                            UserRole.ENCARGADO -> "Encargado"
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = {
                Text(if (selectedRole == UserRole.CLIENTE) "Número de membresía" else "Correo")
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onLogin(selectedRole) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Demo: el login todavía no valida credenciales. Aquí conectarás tu API.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
