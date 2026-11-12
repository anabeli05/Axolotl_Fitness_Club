package com.example.gymcontrol.ui.recepcion.perfil

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.R
import com.example.gymcontrol.data.biometria.FotoPerfil
import com.example.gymcontrol.data.remote.AuthRepository
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.data.remote.PerfilPersonalRow
import com.example.gymcontrol.data.remote.PersonalRepository
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.SesionActual
import kotlinx.coroutines.launch

/**
 * Perfil de Recepción: foto (la toma el Encargado), rol, estado y datos.
 * Puede cambiar su teléfono y su contraseña; nombre y correo los administra el Encargado.
 */
@Composable
fun RecepcionPerfilScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var perfil by remember { mutableStateOf<PerfilPersonalRow?>(null) }
    var foto by remember { mutableStateOf<ImageBitmap?>(null) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    val correo = SesionActual.correo ?: ""
    val name = perfil?.nombre ?: SesionActual.nombre ?: ""

    LaunchedEffect(Unit) {
        if (correo.isBlank()) return@LaunchedEffect
        try {
            perfil = PersonalRepository.miPerfil(correo)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.perfil_load_error), Toast.LENGTH_SHORT).show()
        }
        // Si falla, se quedan las iniciales
        try { foto = FotoPerfil.aImagen(PersonalRepository.fotoPorCorreo(correo)) } catch (_: Exception) { }
    }

    GymScaffold(currentSection = "Perfil", onNavigate = onNavigate) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp)
        ) {
            item {
                val forma = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ClienteTema.Beige, forma)
                        .border(2.dp, ClienteTema.Borde, forma)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(name = name, foto = foto)
                    Spacer(Modifier.height(12.dp))
                    Text(name, color = ClienteTema.Texto, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.role_reception), color = ClienteTema.AzulSel, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.perfil_status_label), color = ClienteTema.Texto, fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(isActive = perfil?.estado?.let { it == "ACTIVO" } ?: true)
                    }
                    if (foto == null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.perfil_staff_photo_hint),
                            color = ClienteTema.TextoSec,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    ActionRow(Icons.Filled.Edit, stringResource(R.string.perfil_action_edit_phone)) { showPhoneDialog = true }
                    Spacer(Modifier.height(8.dp))
                    ActionRow(Icons.Filled.Lock, stringResource(R.string.perfil_action_change_password)) { showPasswordDialog = true }

                    Spacer(Modifier.height(12.dp))

                    // Datos personales (mismos tonos que el perfil del cliente)
                    InfoCard(title = stringResource(R.string.perfil_personal_data_title)) {
                        InfoRow(Icons.Filled.Person, stringResource(R.string.perfil_label_name), name)
                        Spacer(Modifier.height(14.dp))
                        InfoRow(Icons.Filled.Phone, stringResource(R.string.perfil_label_phone), perfil?.telefono?.ifBlank { "—" } ?: "—")
                        Spacer(Modifier.height(14.dp))
                        InfoRow(Icons.Filled.Email, stringResource(R.string.perfil_label_email), perfil?.gmail ?: correo)
                        Spacer(Modifier.height(14.dp))
                        InfoRow(
                            Icons.Filled.DateRange, "${stringResource(R.string.label_registration)}:",
                            ClientesRepository.fechaApp(perfil?.fechaRegistro)
                        )
                    }
                }
            }
        }
    }

    if (showPhoneDialog) {
        PhoneDialog(
            initialPhone = perfil?.telefono ?: "",
            onDismiss = { showPhoneDialog = false },
            onSave = { nuevo ->
                scope.launch {
                    try {
                        PersonalRepository.editarMiTelefono(correo, nuevo)
                        perfil = perfil?.copy(telefono = nuevo)
                        showPhoneDialog = false
                        Toast.makeText(context, context.getString(R.string.perfil_data_updated), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        val msg = e.message?.substringBefore("\nCode:") ?: ""
                        Toast.makeText(context, "${context.getString(R.string.error_change_failed)}: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showPasswordDialog) {
        PasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onSave = { actual, nueva ->
                scope.launch {
                    try {
                        AuthRepository.cambiarContrasena(correo, actual, nueva)
                        showPasswordDialog = false
                        Toast.makeText(context, context.getString(R.string.perfil_password_updated), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        val msg = e.message?.substringBefore("\nCode:") ?: ""
                        Toast.makeText(context, "${context.getString(R.string.error_change_failed)}: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

// ---------- Componentes ----------

@Composable
private fun Avatar(name: String, foto: ImageBitmap?) {
    Box(
        modifier = Modifier
            .size(116.dp)
            .clip(CircleShape)
            .background(ClienteTema.AzulSel.copy(alpha = 0.18f), CircleShape)
            .border(3.dp, ClienteTema.Azul, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (foto != null) {
            Image(
                bitmap = foto,
                contentDescription = stringResource(R.string.perfil_photo_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
                color = ClienteTema.Texto,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Caja de acción: usa los tokens Caja/CajaAcento/CajaValor (claro y oscuro)
@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ClienteTema.Caja)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ClienteTema.CajaAcento, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = ClienteTema.CajaValor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ClienteTema.CajaAcento, modifier = Modifier.size(24.dp))
    }
}

// Caja de información con título y divisor (igual que en el perfil del cliente)
@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClienteTema.Caja, RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Text(
            title,
            color = ClienteTema.CajaAcento,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ClienteTema.CajaAcento.copy(alpha = 0.5f))
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ClienteTema.CajaAcento, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = ClienteTema.CajaAcento, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(88.dp))
        Text(
            value,
            color = ClienteTema.CajaValor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    val color = if (isActive) ClienteTema.Verde else ClienteTema.Rojo
    Box(
        modifier = Modifier.size(64.dp, 24.dp).background(color, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(if (isActive) R.string.status_active else R.string.status_inactive),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------- Diálogos ----------

@Composable
private fun PerfilDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Beige, RoundedCornerShape(16.dp))
                .border(1.5.dp, ClienteTema.Borde, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                color = ClienteTema.Texto,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            content()
        }
    }
}

@Composable
private fun DialogButtons(confirmText: String, confirmEnabled: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Rojo),
            border = BorderStroke(1.dp, ClienteTema.Rojo)
        ) { Text(stringResource(R.string.cancel), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Azul, contentColor = Color.White)
        ) { Text(confirmText, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PhoneDialog(initialPhone: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var phone by remember { mutableStateOf(initialPhone) }
    PerfilDialog(title = stringResource(R.string.perfil_action_edit_phone), onDismiss = onDismiss) {
        PerfilTextField(
            value = phone,
            onValueChange = { new -> if (new.length <= 10 && new.all { it in '0'..'9' }) phone = new },
            label = stringResource(R.string.perfil_field_phone),
            keyboardType = KeyboardType.Number,
            isError = phone.isNotEmpty() && phone.length < 10,
            errorText = stringResource(R.string.perfil_phone_length, phone.length)
        )
        DialogButtons(
            confirmText = stringResource(R.string.save),
            confirmEnabled = phone.length == 10 && phone != initialPhone,
            onConfirm = { onSave(phone) },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val tooShort = newPassword.isNotEmpty() && newPassword.length < 8
    val mismatch = confirmPassword.isNotEmpty() && confirmPassword != newPassword
    val canSave = currentPassword.isNotEmpty() && newPassword.length >= 8 && confirmPassword == newPassword

    PerfilDialog(title = stringResource(R.string.change_password), onDismiss = onDismiss) {
        PerfilTextField(currentPassword, { currentPassword = it }, stringResource(R.string.current_password), isPassword = true)
        PerfilTextField(
            newPassword, { newPassword = it }, stringResource(R.string.new_password),
            isPassword = true, isError = tooShort, errorText = stringResource(R.string.perfil_password_min_length)
        )
        PerfilTextField(
            confirmPassword, { confirmPassword = it }, stringResource(R.string.perfil_confirm_password),
            isPassword = true, isError = mismatch, errorText = stringResource(R.string.passwords_dont_match)
        )
        DialogButtons(
            confirmText = stringResource(R.string.perfil_confirm_update),
            confirmEnabled = canSave,
            onConfirm = { onSave(currentPassword, newPassword) },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PerfilTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError && errorText != null) { { Text(errorText) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClienteTema.Azul,
            unfocusedBorderColor = ClienteTema.Borde,
            focusedTextColor = ClienteTema.Texto,
            unfocusedTextColor = ClienteTema.Texto,
            focusedLabelColor = ClienteTema.Azul,
            unfocusedLabelColor = ClienteTema.TextoSec,
            cursorColor = ClienteTema.Azul,
            errorBorderColor = ClienteTema.Rojo,
            errorTextColor = ClienteTema.Texto,
            errorLabelColor = ClienteTema.Rojo,
            errorSupportingTextColor = ClienteTema.Rojo
        )
    )
}