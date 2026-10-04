package com.example.gymcontrol.ui.encargado.usuarios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.theme.GymColors

private val Purple = Color(0xFFA54FD9)
private val ScreenBackground = Color(0xFF0B0B0E)
private val CardBackground = Color(0xFF1E1F20)
private val CardBorder = Color(0xFF4A4B4D)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB5B5B5)

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = CardBackground,
    unfocusedContainerColor = CardBackground,
    focusedBorderColor = Purple,
    unfocusedBorderColor = CardBorder,
    focusedLabelColor = Purple,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Purple
)

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = formFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun NuevoUsuarioScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.RECEPCION) }
    var cost by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }

    val isInstructor = role == UserRole.INSTRUCTOR
    val canSave = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() &&
            (!isInstructor || (cost.isNotBlank() && capacity.isNotBlank()))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = TextPrimary)
            }
            Text(
                text = "Nuevo personal",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(color = Purple, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Completa los datos del nuevo integrante del personal",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Datos personales", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    FormField(name, { name = it }, "Nombre")
                    FormField(phone, { phone = it }, "Teléfono", KeyboardType.Phone)
                }
            }

            //Acceso
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Acceso a la app", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    FormField(email, { email = it }, "Correo asignado", KeyboardType.Email)
                    FormField(password, { password = it }, "Contraseña asignada", KeyboardType.Password, isPassword = true)
                }
            }

            //Rol
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Rol", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            UserRole.RECEPCION to "Recepción",
                            UserRole.INSTRUCTOR to "Instructor"
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = role == value,
                                onClick = { role = value },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.Black,
                                    labelColor = TextPrimary,
                                    selectedContainerColor = Purple,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    if (isInstructor) {
                        FormField(cost, { cost = it }, "Costo asesoría", KeyboardType.Number)
                        FormField(capacity, { capacity = it }, "Capacidad máxima", KeyboardType.Number)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Estado inicial:", color = TextSecondary, fontSize = 13.sp)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, GymColors.Green)
                        ) {
                            Text(
                                text = "ACTIVO",
                                color = GymColors.Green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    onBack()
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple,
                    contentColor = Color.Black,
                    disabledContainerColor = Purple.copy(alpha = 0.35f),
                    disabledContentColor = Color.Black.copy(alpha = 0.6f)
                )
            ) {
                Text("Guardar personal", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CardBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text("Cancelar")
            }
        }
    }
}