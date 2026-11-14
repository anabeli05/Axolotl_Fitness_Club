package com.example.gymcontrol.ui.instructor.perfil

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.InstructorRepository
import com.example.gymcontrol.data.remote.PersonalRepository
import com.example.gymcontrol.data.biometria.FotoPerfil
import com.example.gymcontrol.ui.components.ClienteScaffold
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.components.ScaffoldTab
import com.example.gymcontrol.ui.components.ClienteTema
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// OJO: son getters (get() = ...) para que cambien al alternar claro/oscuro
private val CardBorder
    get() = Brush.linearGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel))

private val InnerBackground
    get() = ClienteTema.Caja

private val INSTRUCTOR_TABS = listOf(
    ScaffoldTab("Mis clientes", Icons.Filled.Group),
    ScaffoldTab("Perfil", Icons.Filled.Person)
)

@Composable
fun InstructorPerfilScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(SesionActual.nombre ?: "") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(SesionActual.correo ?: "") }

    var activo by remember { mutableStateOf(true) }
    var capacity by remember { mutableStateOf(0) }
    var activeClients by remember { mutableStateOf(0) }
    // Foto que le toma el Encargado (el instructor no la cambia desde aquí)
    var foto by remember { mutableStateOf<ImageBitmap?>(null) }

    // Datos reales del instructor que inició sesión
    LaunchedEffect(Unit) {
        val correo = SesionActual.correo ?: return@LaunchedEffect
        try {
            val p = InstructorRepository.perfil(correo)
            if (p != null) {
                name = p.nombre
                phone = p.telefono
                email = p.gmail
                activo = p.estado == "ACTIVO"
                capacity = p.maxClientes
                activeClients = p.activos
            }
        } catch (e: Exception) {
            val msg = e.message?.substringBefore("\nCode:") ?: ""
            Toast.makeText(context, "No se pudo cargar el perfil: $msg", Toast.LENGTH_LONG).show()
        }
        // Si falla, se quedan las iniciales
        try { foto = FotoPerfil.aImagen(PersonalRepository.fotoPorCorreo(correo)) } catch (_: Exception) { }
    }

    var showEditDialog by remember { mutableStateOf(false) }

    ClienteScaffold(
        currentSection = "Perfil",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_final,
        tabs = INSTRUCTOR_TABS
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.perfil_title),
                    color = ClienteTema.Texto,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ClienteTema.Beige, RoundedCornerShape(14.dp))
                        .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileAvatar(name = name, foto = foto)

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = name,
                        color = ClienteTema.Texto,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.perfil_status_label),
                            color = ClienteTema.TextoSec,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        if (activo) {
                            Chip(text = stringResource(R.string.status_active), color = ClienteTema.Verde, fondo = ClienteTema.VerdePastel, width = 84.dp, height = 30.dp)
                        } else {
                            Chip(text = stringResource(R.string.status_inactive), color = ClienteTema.Rojo, fondo = ClienteTema.RojoPastel, width = 96.dp, height = 30.dp)
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    ActionRow(
                        icon = Icons.Filled.Edit,
                        label = stringResource(R.string.perfil_action_edit_data),
                        onClick = { showEditDialog = true }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Datos personales
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(InnerBackground, RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.perfil_personal_data_title),
                            color = ClienteTema.CajaAcento,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(ClienteTema.CajaAcento.copy(alpha = 0.6f))
                        )
                        Spacer(Modifier.height(12.dp))

                        InfoRow(icon = Icons.Filled.Person, label = stringResource(R.string.perfil_label_name), value = name)
                        Spacer(Modifier.height(14.dp))
                        InfoRow(icon = Icons.Filled.Phone, label = stringResource(R.string.perfil_label_phone), value = formatPhone(phone))
                        Spacer(Modifier.height(14.dp))
                        InfoRow(icon = Icons.Filled.Email, label = stringResource(R.string.perfil_label_email), value = email)
                        Spacer(Modifier.height(14.dp))

                        // Capacidad máxima de clientes y chip "Cupo libre" / lleno alineados horizontalmente
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.Group,
                                contentDescription = null,
                                tint = ClienteTema.CajaAcento,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.instructor_max_capacity_label),
                                color = ClienteTema.CajaAcento,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = capacity.toString(),
                                color = ClienteTema.CajaValor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (activeClients >= capacity) {
                                Chip(text = stringResource(R.string.instructor_capacity_full), color = ClienteTema.Rojo, fondo = ClienteTema.RojoPastel, height = 26.dp, fontSize = 12.sp, borderWidth = 1.5.dp)
                            } else {
                                Chip(text = stringResource(R.string.available_spots_label), color = ClienteTema.Verde, fondo = ClienteTema.VerdePastel, height = 26.dp, fontSize = 12.sp, borderWidth = 1.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditDataDialog(
            name = name,
            initialPhone = phone,
            email = email,
            onDismiss = { showEditDialog = false },
            onSave = { newPhone ->
                scope.launch {
                    try {
                        // Solo el teléfono es editable; nombre y correo se mandan como están
                        InstructorRepository.editarPerfil(email, name, newPhone.trim(), email)
                        phone = newPhone.trim()
                        showEditDialog = false
                        Toast.makeText(context, context.resources.getString(R.string.data_updated_success), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        val msg = e.message?.substringBefore("\nCode:") ?: ""
                        Toast.makeText(context, "No se pudo guardar: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

// "2487693720" -> "+52 248 769 3720"
private fun formatPhone(phone: String): String =
    if (phone.length == 10) {
        "+52 ${phone.substring(0, 3)} ${phone.substring(3, 6)} ${phone.substring(6)}"
    } else phone

// Avatar: la foto que le tomó el Encargado, o las iniciales si aún no tiene
@Composable
private fun ProfileAvatar(name: String, foto: ImageBitmap?) {
    Box(
        modifier = Modifier
            .size(116.dp)
            .clip(CircleShape)
            .background(ClienteTema.AzulSel.copy(alpha = 0.18f), CircleShape)
            .border(3.dp, CardBorder, CircleShape),
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
                text = initials(name),
                color = ClienteTema.Texto,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun initials(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

// Caja de acción (café): icono y flecha dorados, texto blanco cálido
@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ClienteTema.Caja)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ClienteTema.CajaAcento, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = ClienteTema.CajaValor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = ClienteTema.CajaAcento,
            modifier = Modifier.size(24.dp)
        )
    }
}

// Fila de información: icono y etiqueta en dorado, valor en blanco cálido
@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ClienteTema.CajaAcento, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = ClienteTema.CajaAcento,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = value,
            color = ClienteTema.CajaValor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// Chip de estado: fondo pastel, borde del mismo color. Si no se da width, se ajusta al texto.
@Composable
private fun Chip(
    text: String,
    color: Color,
    height: androidx.compose.ui.unit.Dp,
    fondo: Color = color.copy(alpha = 0.15f),
    width: androidx.compose.ui.unit.Dp? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp
) {
    val ancho = if (width != null) Modifier.width(width) else Modifier
    Box(
        modifier = Modifier
            .height(height)
            .then(ancho)
            .background(fondo, RoundedCornerShape(10.dp))
            .border(borderWidth, color, RoundedCornerShape(10.dp))
            .padding(horizontal = if (width == null) 10.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

// ---------- Diálogos con el mismo estilo de las tarjetas (borde degradado) ----------

@Composable
private fun GymDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Beige, RoundedCornerShape(14.dp))
                .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    color = ClienteTema.Texto,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                )
                content()
            }

            // X para cerrar
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = ClienteTema.TextoSec,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun EditDataDialog(
    name: String,
    initialPhone: String,
    email: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var phone by remember { mutableStateOf(initialPhone) }

    // Aviso que aparece unos segundos cuando se intenta escribir algo que no es número
    var phoneWarning by remember { mutableStateOf(false) }
    var phoneTick by remember { mutableStateOf(0) }

    LaunchedEffect(phoneTick) {
        if (phoneTick > 0) {
            phoneWarning = true
            delay(2500)
            phoneWarning = false
        }
    }

    // Regla: teléfono exactamente 10 dígitos numéricos
    val phoneIncomplete = phone.isNotEmpty() && phone.length < 10
    val isValid = phone.length == 10

    GymDialog(title = stringResource(R.string.perfil_edit_dialog_title), onDismiss = onDismiss) {
        // Nombre y correo: solo lectura
        GymTextField(
            value = name,
            onValueChange = {},
            label = stringResource(R.string.perfil_field_name),
            enabled = false
        )
        GymTextField(
            value = email,
            onValueChange = {},
            label = stringResource(R.string.perfil_field_email),
            enabled = false
        )

        // Único campo editable
        GymTextField(
            value = phone,
            onValueChange = { new ->
                when {
                    !new.all { it in '0'..'9' } -> phoneTick++
                    new.length <= 10 -> phone = new
                }
            },
            label = stringResource(R.string.perfil_field_phone),
            keyboardType = KeyboardType.Number,
            isError = phoneWarning || phoneIncomplete,
            errorText = if (phoneWarning) stringResource(R.string.perfil_error_phone_digits)
            else stringResource(R.string.error_phone_10_digits, phone.length)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Rojo),
                border = BorderStroke(1.dp, ClienteTema.Rojo)
            ) {
                Text(stringResource(R.string.cancel), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onSave(phone) },
                enabled = isValid,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClienteTema.Azul,
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GymTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError && errorText != null) {
            { Text(errorText) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),  // píldora, igual que el inicio de sesión
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClienteTema.AzulSel,
            unfocusedBorderColor = ClienteTema.Borde,
            focusedTextColor = ClienteTema.Texto,
            unfocusedTextColor = ClienteTema.Texto,
            focusedLabelColor = ClienteTema.AzulSel,
            unfocusedLabelColor = ClienteTema.TextoSec,
            cursorColor = ClienteTema.AzulSel,
            errorBorderColor = ClienteTema.Rojo,
            errorTextColor = ClienteTema.Texto,
            errorLabelColor = ClienteTema.Rojo,
            errorCursorColor = ClienteTema.Rojo,
            errorSupportingTextColor = ClienteTema.Rojo,
            // Campos de solo lectura (nombre y correo)
            disabledTextColor = ClienteTema.TextoSec.copy(alpha = 0.6f),
            disabledBorderColor = ClienteTema.Borde.copy(alpha = 0.4f),
            disabledLabelColor = ClienteTema.TextoSec.copy(alpha = 0.5f)
        )
    )
}