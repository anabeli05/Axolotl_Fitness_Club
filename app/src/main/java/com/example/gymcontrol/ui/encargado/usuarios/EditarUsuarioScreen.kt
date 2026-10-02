package com.example.gymcontrol.ui.encargado.usuarios

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.data.model.Status
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.theme.GymColors

private val Purple = Color(0xFFA54FD9)
private val ScreenBackground = Color(0xFF0B0B0E)
private val CardBackground = Color(0xFF1E1F20)
private val CardBorder = Color(0xFF4A4B4D)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB5B5B5)

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
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
private fun EditField(
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
        colors = editFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
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
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
@Composable
fun EditarUsuarioScreen(
    name: String,
    email: String,
    role: UserRole,
    status: Status,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var nameValue by remember { mutableStateOf(name) }
    var phone by remember { mutableStateOf("") }
    var emailValue by remember { mutableStateOf(email) }
    var password by remember { mutableStateOf("") }
    var roleValue by remember { mutableStateOf(role) }
    var statusValue by remember { mutableStateOf(status) }
    var cost by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }

    val canSave = nameValue.isNotBlank() && emailValue.isNotBlank()

    BackHandler(onBack = onBack)

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
                text = "Editar personal",
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
                text = "Modifica los datos del integrante del personal",
                color = TextSecondary,
                fontSize = 14.sp
            )

            SectionCard("Datos personales") {
                EditField(nameValue, { nameValue = it }, "Nombre")
                EditField(phone, { phone = it }, "Teléfono", KeyboardType.Phone)
            }

            SectionCard("Acceso a la app") {
                EditField(emailValue, { emailValue = it }, "Correo asignado", KeyboardType.Email)
                EditField(
                    password, { password = it }, "Nueva contraseña (opcional)",
                    KeyboardType.Password, isPassword = true
                )
            }

            SectionCard("Rol y estado") {
                Text("Rol", color = TextSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        UserRole.RECEPCION to "Recepción",
                        UserRole.INSTRUCTOR to "Instructor",
                        UserRole.ENCARGADO to "Encargado"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = roleValue == value,
                            onClick = { roleValue = value },
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

                if (roleValue == UserRole.INSTRUCTOR) {
                    EditField(cost, { cost = it }, "Costo asesoría", KeyboardType.Number)
                    EditField(capacity, { capacity = it }, "Capacidad máxima", KeyboardType.Number)
                }

                Text("Estado", color = TextSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Status.ACTIVO to ("Activo" to GymColors.Green),
                        Status.INACTIVO to ("Inactivo" to GymColors.Red)
                    ).forEach { (value, info) ->
                        FilterChip(
                            selected = statusValue == value,
                            onClick = { statusValue = value },
                            label = { Text(info.first) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Black,
                                labelColor = info.second,
                                selectedContainerColor = info.second,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    onSave()
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
                Text("Guardar cambios", fontWeight = FontWeight.Bold)
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
