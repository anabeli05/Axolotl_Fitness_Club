package com.example.gymcontrol.ui.encargado.clientes

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymcontrol.R
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.DatosSaludCampos
import com.example.gymcontrol.ui.components.fechaNacimientoValida
import com.example.gymcontrol.ui.components.metodoPagoLocalizado
import com.example.gymcontrol.data.remote.DatosSalud
import kotlinx.coroutines.delay

// El nombre debe tener al menos `min` letras (espacios y números no cuentan)
private fun nameHasMinLetters(s: String, min: Int = 3) = s.count { it.isLetter() } >= min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormDialog(
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        nombre: String, telefono: String, correo: String, plan: String?,
        contactoNombre: String, contactoTelefono: String, metodo: String,
        salud: DatosSalud
    ) -> Unit,
    plans: List<String> = emptyList(),
    error: String? = null,
    // Datos con los que arranca el formulario (vacíos al crear, con los del cliente al editar)
    initialName: String = "",
    initialLastName: String = "",
    initialPhone: String = "",
    initialEmail: String = "",
    initialPlan: String? = null,
    // Contacto de emergencia: solo nombre y teléfono (obligatorio)
    initialContactName: String = "",
    initialContactPhone: String = "",
    // Fecha de nacimiento y datos de salud (al editar se cargan los del cliente)
    initialSalud: DatosSalud = DatosSalud()
) {
    var name by remember { mutableStateOf(initialName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var email by remember { mutableStateOf(initialEmail) }
    var selectedPlan by remember { mutableStateOf(initialPlan) }
    var planMenuExpanded by remember { mutableStateOf(false) }
    var contactName by remember(initialContactName) { mutableStateOf(initialContactName) }
    var contactPhone by remember(initialContactPhone) { mutableStateOf(initialContactPhone) }
    var salud by remember(initialSalud) { mutableStateOf(initialSalud) }
    var metodo by remember { mutableStateOf(com.example.gymcontrol.data.remote.METODOS_PAGO.first()) }

    val borderBrush = Brush.linearGradient(colors = listOf(ClienteTema.Borde, ClienteTema.AzulSel))

    // Reglas: nombre y apellido solo letras (mínimo 3), teléfono 10 dígitos, correo con formato válido, plan elegido
    val nameValid = nameHasMinLetters(name)
    val lastNameValid = nameHasMinLetters(lastName)
    val emailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    // El contacto de emergencia es obligatorio: nombre (mínimo 3 letras) y teléfono de 10 dígitos
    val contactValid = nameHasMinLetters(contactName) && contactPhone.length == 10
    // Fecha de nacimiento obligatoria al registrar; al editar, si ya hay una, debe ser válida
    val saludValida = if (salud.fechaNacimiento == null) isEditing else fechaNacimientoValida(salud.fechaNacimiento)
    val canSave = nameValid &&
            saludValida &&
            contactValid &&
            lastNameValid &&
            phone.length == 10 &&
            emailValid &&
            (isEditing || selectedPlan != null)

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
                .background(ClienteTema.Beige, RoundedCornerShape(16.dp))
                .border(1.5.dp, borderBrush, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEditing) stringResource(R.string.encargado_client_form_edit_title) else stringResource(R.string.encargado_client_form_new_title),
                    color = ClienteTema.Texto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = ClienteTema.TextoSec)
                }
            }

            Spacer(Modifier.height(12.dp))

            RestrictedField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.encargado_client_form_name_label),
                accept = { s -> s.all { it.isLetter() || it == ' ' } },
                maxLength = 30,
                warningText = stringResource(R.string.encargado_client_form_name_warning),
                errorText = if (name.isNotEmpty() && !nameValid) stringResource(R.string.encargado_client_form_min_letters) else null
            )
            Spacer(Modifier.height(10.dp))
            RestrictedField(
                value = lastName,
                onValueChange = { lastName = it },
                label = stringResource(R.string.encargado_client_form_lastname_label),
                accept = { s -> s.all { it.isLetter() || it == ' ' } },
                maxLength = 30,
                warningText = stringResource(R.string.encargado_client_form_name_warning),
                errorText = if (lastName.isNotEmpty() && !lastNameValid) stringResource(R.string.encargado_client_form_min_letters) else null
            )
            Spacer(Modifier.height(10.dp))
            RestrictedField(
                value = phone,
                onValueChange = { phone = it },
                label = stringResource(R.string.encargado_client_form_phone_label),
                accept = { s -> s.all { it in '0'..'9' } },
                maxLength = 10,
                warningText = stringResource(R.string.encargado_client_form_phone_warning),
                keyboardType = KeyboardType.Number,
                errorText = if (phone.isNotEmpty() && phone.length < 10) {
                    stringResource(R.string.error_phone_10_digits, phone.length)
                } else null
            )
            Spacer(Modifier.height(10.dp))
            RestrictedField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.encargado_client_form_email_label),
                accept = { s -> s.none { it.isWhitespace() } },
                maxLength = 60,
                warningText = stringResource(R.string.encargado_client_form_email_warning),
                keyboardType = KeyboardType.Email,
                errorText = if (email.isNotEmpty() && !emailValid) stringResource(R.string.encargado_client_form_email_invalid) else null
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.client_form_emergency_title),
                color = ClienteTema.Texto,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            RestrictedField(
                value = contactName,
                onValueChange = { contactName = it },
                label = stringResource(R.string.client_form_emergency_name),
                accept = { s -> s.all { it.isLetter() || it == ' ' } },
                maxLength = 60,
                warningText = stringResource(R.string.encargado_client_form_name_warning),
                errorText = if (contactName.isNotBlank() && !nameHasMinLetters(contactName)) stringResource(R.string.encargado_client_form_min_letters)
                else if (contactName.isBlank() && contactPhone.isNotEmpty()) stringResource(R.string.client_form_emergency_name_missing)
                else null
            )
            Spacer(Modifier.height(10.dp))
            RestrictedField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                label = stringResource(R.string.client_form_emergency_phone),
                accept = { s -> s.all { it in '0'..'9' } },
                maxLength = 10,
                warningText = stringResource(R.string.encargado_client_form_phone_warning),
                keyboardType = KeyboardType.Number,
                errorText = if (contactPhone.isNotEmpty() && contactPhone.length < 10) {
                    "Deben ser 10 dígitos (${contactPhone.length}/10)"
                } else null
            )
            Spacer(Modifier.height(14.dp))
            DatosSaludCampos(
                datos = salud,
                onChange = { salud = it },
                fechaObligatoria = !isEditing
            )
            Spacer(Modifier.height(10.dp))

            if (!isEditing) ExposedDropdownMenuBox(
                expanded = planMenuExpanded,
                onExpandedChange = { planMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedPlan ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(stringResource(R.string.encargado_client_form_plan_placeholder), color = ClienteTema.TextoSec) },
                    label = { Text(stringResource(R.string.encargado_client_form_plan_label), color = ClienteTema.TextoSec) },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = ClienteTema.TextoSec)
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
                    modifier = Modifier.background(ClienteTema.Beige)
                ) {
                    plans.forEach { plan ->
                        DropdownMenuItem(
                            text = { Text(plan, color = ClienteTema.Texto) },
                            onClick = {
                                selectedPlan = plan
                                planMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (!isEditing) {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.client_form_payment_method), color = ClienteTema.TextoSec, fontSize = 13.sp)
                com.example.gymcontrol.data.remote.METODOS_PAGO.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = metodo == m,
                            onClick = { metodo = m },
                            colors = RadioButtonDefaults.colors(selectedColor = ClienteTema.AzulSel)
                        )
                        Text(metodoPagoLocalizado(m), color = ClienteTema.Texto, fontSize = 13.sp)
                    }
                }
                if (metodo == "Transferencia") {
                    Text(stringResource(R.string.client_form_transfer_note), color = ClienteTema.TextoSec, fontSize = 12.sp)
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = ClienteTema.Rojo, fontSize = 13.sp)
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    onSave(
                        "${name.trim()} ${lastName.trim()}".trim(),
                        phone,
                        email.trim(),
                        selectedPlan,
                        contactName.trim(),
                        contactPhone,
                        metodo,
                        salud
                    )
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClienteTema.Azul,
                    contentColor = ClienteTema.Texto,
                    disabledContainerColor = ClienteTema.Azul.copy(alpha = 0.45f),
                    disabledContentColor = ClienteTema.Texto.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) stringResource(R.string.save) else stringResource(R.string.encargado_client_form_create_button), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ClienteTema.AzulSel,
    unfocusedBorderColor = ClienteTema.Borde,
    focusedTextColor = ClienteTema.Texto,
    unfocusedTextColor = ClienteTema.Texto,
    focusedLabelColor = ClienteTema.AzulSel,
    cursorColor = ClienteTema.AzulSel,
    errorBorderColor = ClienteTema.Rojo,
    errorTextColor = ClienteTema.Texto,
    errorLabelColor = ClienteTema.Rojo,
    errorCursorColor = ClienteTema.Rojo,
    errorSupportingTextColor = ClienteTema.Rojo
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
        label = { Text(label, color = if (shownError != null) ClienteTema.Rojo else ClienteTema.TextoSec) },
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