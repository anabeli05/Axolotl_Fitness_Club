package com.example.gymcontrol.ui.recepcion.clientes

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.theme.GymColors
import kotlinx.coroutines.delay

// El nombre debe tener al menos `min` letras (espacios y números no cuentan)
private fun nameHasMinLetters(s: String, min: Int = 3) = s.count { it.isLetter() } >= min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormDialog(
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    // Datos con los que arranca el formulario (vacíos al crear, con los del cliente al editar)
    initialName: String = "",
    initialLastName: String = "",
    initialPhone: String = "",
    initialEmail: String = "",
    initialPlan: String? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var email by remember { mutableStateOf(initialEmail) }
    var selectedPlan by remember { mutableStateOf(initialPlan) }
    var planMenuExpanded by remember { mutableStateOf(false) }

    val plans = GymApp.repository.services().map { it.name }
    val borderBrush = Brush.linearGradient(colors = listOf(GymColors.Purple, GymColors.Gold))

    // Reglas: nombre y apellido solo letras (mínimo 3), teléfono 10 dígitos, correo con formato válido, plan elegido
    val nameValid = nameHasMinLetters(name)
    val lastNameValid = nameHasMinLetters(lastName)
    val emailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val canSave = nameValid &&
            lastNameValid &&
            phone.length == 10 &&
            emailValid &&
            selectedPlan != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(GymColors.Surface, RoundedCornerShape(16.dp))
                .border(1.5.dp, borderBrush, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Editar cliente" else "Nuevo cliente",
                    color = GymColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = GymColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            RestrictedField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre",
                accept = { s -> s.all { it.isLetter() || it == ' ' } },
                maxLength = 30,
                warningText = "Solo se permiten letras",
                errorText = if (name.isNotEmpty() && !nameValid) "Mínimo 3 letras" else null
            )
            Spacer(Modifier.height(10.dp))
            RestrictedField(
                value = lastName,
                onValueChange = { lastName = it },
                label = "Apellido",
                accept = { s -> s.all { it.isLetter() || it == ' ' } },
                maxLength = 30,
                warningText = "Solo se permiten letras",
                errorText = if (lastName.isNotEmpty() && !lastNameValid) "Mínimo 3 letras" else null
            )
            Spacer(Modifier.height(10.dp))
            RestrictedField(
                value = phone,
                onValueChange = { phone = it },
                label = "Teléfono",
                accept = { s -> s.all { it in '0'..'9' } },
                maxLength = 10,
                warningText = "Solo se permiten números",
                keyboardType = KeyboardType.Number,
                errorText = if (phone.isNotEmpty() && phone.length < 10) {
                    "Deben ser 10 dígitos (${phone.length}/10)"
                } else null
            )
            Spacer(Modifier.height(10.dp))
            RestrictedField(
                value = email,
                onValueChange = { email = it },
                label = "Correo",
                accept = { s -> s.none { it.isWhitespace() } },
                maxLength = 60,
                warningText = "El correo no lleva espacios",
                keyboardType = KeyboardType.Email,
                errorText = if (email.isNotEmpty() && !emailValid) "Correo no válido" else null
            )
            Spacer(Modifier.height(10.dp))

            ExposedDropdownMenuBox(
                expanded = planMenuExpanded,
                onExpandedChange = { planMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedPlan ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Selecciona un plan", color = GymColors.TextSecondary) },
                    label = { Text("Plan", color = GymColors.TextSecondary) },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = GymColors.TextSecondary)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                ExposedDropdownMenu(
                    expanded = planMenuExpanded,
                    onDismissRequest = { planMenuExpanded = false },
                    modifier = Modifier.background(GymColors.Surface)
                ) {
                    plans.forEach { plan ->
                        DropdownMenuItem(
                            text = { Text(plan, color = GymColors.TextPrimary) },
                            onClick = {
                                selectedPlan = plan
                                planMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onSave,
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GymColors.Purple,
                    contentColor = GymColors.TextPrimary,
                    disabledContainerColor = GymColors.Purple.copy(alpha = 0.45f),
                    disabledContentColor = GymColors.TextPrimary.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Guardar" else "Crear Cliente", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GymColors.Purple,
    unfocusedBorderColor = GymColors.Border,
    focusedTextColor = GymColors.TextPrimary,
    unfocusedTextColor = GymColors.TextPrimary,
    focusedLabelColor = GymColors.Purple,
    cursorColor = GymColors.Gold,
    errorBorderColor = GymColors.Red,
    errorTextColor = GymColors.TextPrimary,
    errorLabelColor = GymColors.Red,
    errorCursorColor = GymColors.Red,
    errorSupportingTextColor = GymColors.Red
)

// Campo que rechaza lo que no cumple `accept` (muestra un aviso unos segundos)
// y limita la longitud. `errorText` es un error fijo (ej. "faltan dígitos").
@Composable
private fun RestrictedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accept: (String) -> Boolean,
    maxLength: Int,
    warningText: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: String? = null
) {
    var warning by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(tick) {
        if (tick > 0) {
            warning = true
            delay(2500)
            warning = false
        }
    }

    val shownError = if (warning) warningText else errorText

    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            when {
                !accept(new) -> tick++
                new.length <= maxLength -> onValueChange(new)
            }
        },
        label = { Text(label, color = if (shownError != null) GymColors.Red else GymColors.TextSecondary) },
        singleLine = true,
        isError = shownError != null,
        supportingText = if (shownError != null) {
            { Text(shownError) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = fieldColors()
    )
}