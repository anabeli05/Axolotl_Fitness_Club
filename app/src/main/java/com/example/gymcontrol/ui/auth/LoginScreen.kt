package com.example.gymcontrol.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.data.remote.AuthRepository
import kotlinx.coroutines.launch

// Paleta tomada del diseño
private val FondoNegro = Color(0xFF000000)
private val TarjetaOscura = Color(0xFF1B1B1B)
private val MoradoCampo = Color(0xFF3B2459)
private val MoradoBorde = Color(0xFF4A2D73)
private val DoradoBorde = Color(0xFF6B5326)
private val Dorado = Color(0xFFC9A24B)
private val BlancoTexto = Color(0xFFFFFFFF)
private val RojoError = Color(0xFFFF6B6B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLogin: (UserRole) -> Unit) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val campoShape = RoundedCornerShape(50)
    val campoColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MoradoCampo,
        unfocusedContainerColor = MoradoCampo,
        focusedBorderColor = Dorado,
        unfocusedBorderColor = DoradoBorde,
        focusedTextColor = BlancoTexto,
        unfocusedTextColor = BlancoTexto,
        cursorColor = Dorado,
        focusedLabelColor = Dorado,
        unfocusedLabelColor = BlancoTexto,
        focusedTrailingIconColor = Dorado,
        unfocusedTrailingIconColor = BlancoTexto
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoNegro)
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(TarjetaOscura)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(MoradoBorde, DoradoBorde)),
                    shape = RoundedCornerShape(28.dp)
                )
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_axolotl),
                contentDescription = "Axolotl Fitness Club",
                modifier = Modifier.size(150.dp)
            )
            Spacer(Modifier.height(24.dp))

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = { Text("Correo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = campoShape,
                colors = campoColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar contraseña"
                            else "Mostrar contraseña"
                        )
                    }
                },
                shape = campoShape,
                colors = campoColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            val id = identifier.trim()
                            // Sin selector: si parece correo se prueba primero el personal
                            val roles = if (id.contains("@")) UserRole.entries.reversed()
                            else UserRole.entries.toList()
                            var rolEncontrado: UserRole? = null
                            for (role in roles) {
                                if (AuthRepository.login(id, password, role) != null) {
                                    rolEncontrado = role
                                    break
                                }
                            }
                            if (rolEncontrado != null) onLogin(rolEncontrado)
                            else error = "Usuario o contraseña incorrectos"
                        } catch (e: Exception) {
                            error = "Error de conexión: ${e.message}"
                        }
                        loading = false
                    }
                },
                enabled = !loading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoradoCampo,
                    contentColor = BlancoTexto,
                    disabledContainerColor = MoradoCampo.copy(alpha = 0.5f),
                    disabledContentColor = BlancoTexto.copy(alpha = 0.7f)
                ),
                border = BorderStroke(1.dp, DoradoBorde),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text(if (loading) "Entrando..." else "Ingresar")
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = RojoError, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}