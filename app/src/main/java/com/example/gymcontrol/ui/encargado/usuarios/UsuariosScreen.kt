package com.example.gymcontrol.ui.encargado.usuarios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymcontrol.data.model.Status
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.data.remote.CorreoRepository
import com.example.gymcontrol.data.remote.PersonalRepository
import com.example.gymcontrol.data.remote.PersonalRow
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.theme.GymColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")

private val CardBorder = Brush.linearGradient(listOf(GymColors.Purple, GymColors.Gold))

// Borde degradado en tonos rojos para el cuadro de eliminar
private val DeleteBorder = Brush.linearGradient(
    listOf(Color(0xFFFF6B6B), GymColors.Red, Color(0xFF7A0A0A))
)

private val STAFF_ROLES = listOf(UserRole.RECEPCION, UserRole.INSTRUCTOR, UserRole.ENCARGADO)

// El nombre debe tener al menos `min` letras (espacios y números no cuentan)
private fun nameHasMinLetters(s: String, min: Int = 3) = s.count { it.isLetter() } >= min

// Datos mínimos de la persona seleccionada para editar o dar de baja
private class StaffItem(
    val id: Long,
    val name: String,
    val email: String,
    val role: UserRole,
    val status: Status
)

// Acción que espera la confirmación "¿Estás seguro?"
private class Pendiente(val mensaje: String, val accion: () -> Unit)

private fun mensajeError(e: Exception, porDefecto: String): String =
    e.message?.substringBefore("\nCode:") ?: porDefecto

@Composable
fun UsuariosScreen(
    // Se conserva por compatibilidad con GymNavGraph; el alta ahora es un formulario flotante
    onNewUser: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var showNewForm by remember { mutableStateOf(false) }
    var errorAlta by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<StaffItem?>(null) }
    var errorEditar by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<StaffItem?>(null) }
    var errorBaja by remember { mutableStateOf<String?>(null) }
    var pendiente by remember { mutableStateOf<Pendiente?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    var personal by remember { mutableStateOf<List<PersonalRow>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            personal = PersonalRepository.listar()
            errorMsg = null
        } catch (e: Exception) {
            errorMsg = "No se pudo cargar: ${e.message}"
        }
    }

    GymScaffold(
        currentSection = "Personal",
        sections = ADMIN_SECTIONS,
        onNavigate = onNavigate
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Button(
                    onClick = { showNewForm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar personal", fontWeight = FontWeight.Bold)
                }
            }

            errorMsg?.let { msg ->
                item { Text(msg, color = GymColors.Red) }
            }
            info?.let { msg ->
                item { Text(msg, color = GymColors.Green, fontSize = 13.sp) }
            }

            items(personal) { user ->
                val item = StaffItem(
                    user.id,
                    user.nombre,
                    user.gmail,
                    UserRole.valueOf(user.rol),
                    Status.valueOf(user.estado)
                )
                StaffCard(
                    item = item,
                    onEdit = { editing = item },
                    onDelete = { deleting = item }
                )
            }
        }
    }

    if (showNewForm) {
        NewStaffDialog(
            error = errorAlta,
            onDismiss = {
                showNewForm = false
                errorAlta = null
            },
            onSave = { nombre, tel, correo, rol, costo, cap ->
                pendiente = Pendiente("¿Estás seguro de dar de alta a $nombre como ${rol.name}?") {
                    scope.launch {
                        try {
                            PersonalRepository.crear(
                                nombre, tel, correo.trim(),
                                rol.name, costo.toDoubleOrNull(), cap.toIntOrNull()
                            )
                            errorAlta = null
                            showNewForm = false
                            personal = PersonalRepository.listar()
                            val enviado = CorreoRepository.enviarBienvenida(correo.trim())
                            info = if (enviado) "Alta realizada. Se envió la contraseña al correo."
                            else "Alta realizada, pero no se pudo enviar el correo. Contraseña inicial: Axolotl2026"
                        } catch (e: Exception) {
                            errorAlta = mensajeError(e, "No se pudo crear")
                        }
                    }
                }
            }
        )
    }

    editing?.let { target ->
        EditStaffDialog(
            target = target,
            error = errorEditar,
            onDismiss = {
                editing = null
                errorEditar = null
            },
            onSave = { nombre, correo, rol, estado ->
                pendiente = Pendiente("¿Estás seguro de guardar los cambios de ${target.name}?") {
                    scope.launch {
                        try {
                            PersonalRepository.editar(
                                SesionActual.correo ?: "", target.id,
                                nombre, correo, rol.name, estado.name
                            )
                            errorEditar = null
                            editing = null
                            personal = PersonalRepository.listar()
                        } catch (e: Exception) {
                            errorEditar = mensajeError(e, "No se pudo guardar")
                        }
                    }
                }
            }
        )
    }

    pendiente?.let { p ->
        ConfirmarDialog(
            pendiente = p,
            onCancelar = { pendiente = null },
            onAceptar = {
                pendiente = null
                p.accion()
            }
        )
    }

    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = {
                deleting = null
                errorBaja = null
            },
            modifier = Modifier.border(1.5.dp, DeleteBorder, RoundedCornerShape(16.dp)),
            containerColor = GymColors.Surface,
            shape = RoundedCornerShape(16.dp),
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = GymColors.Red) },
            title = {
                Text(
                    "¿Eliminar al personal?",
                    color = GymColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${target.name} se eliminará definitivamente del sistema. Esta acción no se puede deshacer.",
                        color = GymColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorBaja?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = GymColors.Red, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                PersonalRepository.eliminar(SesionActual.correo ?: "", target.id)
                                deleting = null
                                errorBaja = null
                                personal = PersonalRepository.listar()
                            } catch (e: Exception) {
                                errorBaja = mensajeError(e, "No se pudo eliminar")
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymColors.Red,
                        contentColor = Color.White
                    )
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        deleting = null
                        errorBaja = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GymColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.TextPrimary)
                ) { Text("Cancelar") }
            }
        )
    }
}

// ---------- Confirmación genérica ----------

@Composable
private fun ConfirmarDialog(pendiente: Pendiente, onCancelar: () -> Unit, onAceptar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = GymColors.Surface,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Confirmar", color = GymColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(pendiente.mensaje, color = GymColors.TextSecondary) },
        confirmButton = {
            Button(
                onClick = onAceptar,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple)
            ) { Text("Sí, continuar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancelar,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GymColors.Border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.TextPrimary)
            ) { Text("Cancelar") }
        }
    )
}

// ---------- Tarjeta (mismo estilo que ServiceCard) ----------

@Composable
private fun StaffCard(item: StaffItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val statusColor = if (item.status == Status.ACTIVO) GymColors.Green else GymColors.Red

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GymColors.Surface, RoundedCornerShape(14.dp))
            .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(GymColors.Purple.copy(alpha = 0.18f), CircleShape)
                .border(2.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = GymColors.TextSecondary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = GymColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${item.role}",
                color = GymColors.Purple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.email,
                color = GymColors.TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                    .border(1.dp, statusColor, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${item.status}",
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconCircleButton(icon = Icons.Filled.Edit, color = GymColors.Orange, onClick = onEdit)
            IconCircleButton(icon = Icons.Filled.Close, color = GymColors.Red, onClick = onDelete)
        }
    }
}

@Composable
private fun IconCircleButton(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(1.5.dp, color, CircleShape)
            .background(color.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
    }
}

// ---------- Formularios (mismo estilo que ServiceFormDialog) ----------

@Composable
private fun StaffDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
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
                .border(1.5.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = GymColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = GymColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun NewStaffDialog(
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, UserRole, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.RECEPCION) }
    var cost by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }

    val isInstructor = role == UserRole.INSTRUCTOR
    val canSave = nameHasMinLetters(name) && phone.length == 10 &&
            email.isNotBlank() &&
            (!isInstructor || (cost.isNotBlank() && capacity.isNotBlank()))

    StaffDialog(title = "Nuevo personal", onDismiss = onDismiss) {
        NameField(name, { name = it })
        Spacer(Modifier.height(10.dp))
        PhoneField(phone, { phone = it })
        Spacer(Modifier.height(10.dp))
        GymTextField(email, { email = it }, "Correo asignado", KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        Text(
            "La contraseña inicial es Axolotl2026 y se envía al correo del usuario.",
            color = GymColors.TextSecondary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(14.dp))
        Text("Rol", color = GymColors.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
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
                        labelColor = GymColors.TextPrimary,
                        selectedContainerColor = GymColors.Purple,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        if (isInstructor) {
            Spacer(Modifier.height(10.dp))
            DigitsField(cost, { cost = it }, "Costo asesoría", maxLength = 6)
            Spacer(Modifier.height(10.dp))
            DigitsField(capacity, { capacity = it }, "Capacidad máxima", maxLength = 3)
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = GymColors.Red, fontSize = 13.sp)
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = { onSave(name.trim(), phone, email, role, cost, capacity) },
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
            Text("Crear personal", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditStaffDialog(
    target: StaffItem,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, UserRole, Status) -> Unit
) {
    var name by remember { mutableStateOf(target.name) }
    var email by remember { mutableStateOf(target.email) }
    var role by remember { mutableStateOf(target.role) }
    var status by remember { mutableStateOf(target.status) }
    var roleExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    StaffDialog(title = "Editar personal", onDismiss = onDismiss) {
        NameField(name, { name = it })
        Spacer(Modifier.height(10.dp))
        GymTextField(email, { email = it }, "Correo", KeyboardType.Email)
        Spacer(Modifier.height(10.dp))

        ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
            OutlinedTextField(
                value = role.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Rol", color = GymColors.TextSecondary) },
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
                expanded = roleExpanded,
                onDismissRequest = { roleExpanded = false },
                modifier = Modifier.background(GymColors.Surface)
            ) {
                STAFF_ROLES.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r.name, color = GymColors.TextPrimary) },
                        onClick = {
                            role = r
                            roleExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
            OutlinedTextField(
                value = status.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Estado", color = GymColors.TextSecondary) },
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
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false },
                modifier = Modifier.background(GymColors.Surface)
            ) {
                Status.values().forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.name, color = GymColors.TextPrimary) },
                        onClick = {
                            status = s
                            statusExpanded = false
                        }
                    )
                }
            }
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = GymColors.Red, fontSize = 13.sp)
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = { onSave(name.trim(), email.trim(), role, status) },
            enabled = nameHasMinLetters(name) && email.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar", fontWeight = FontWeight.Bold)
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

// Nombre: solo letras y espacios (acepta acentos y ñ), máximo 40 caracteres, mínimo 3 letras
@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit) {
    var warning by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(tick) {
        if (tick > 0) {
            warning = true
            delay(2500)
            warning = false
        }
    }

    val tooShort = value.isNotEmpty() && !nameHasMinLetters(value)

    GymTextField(
        value = value,
        onValueChange = { new ->
            when {
                !new.all { it.isLetter() || it == ' ' } -> tick++
                new.length <= 40 -> onValueChange(new)
            }
        },
        label = "Nombre",
        isError = warning || tooShort,
        errorText = if (warning) "Solo se permiten letras" else "Mínimo 3 letras"
    )
}

// Teléfono: solo números, exactamente 10 dígitos
@Composable
private fun PhoneField(value: String, onValueChange: (String) -> Unit) {
    var warning by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(tick) {
        if (tick > 0) {
            warning = true
            delay(2500)
            warning = false
        }
    }

    val incomplete = value.isNotEmpty() && value.length < 10

    GymTextField(
        value = value,
        onValueChange = { new ->
            when {
                !new.all { it in '0'..'9' } -> tick++
                new.length <= 10 -> onValueChange(new)
            }
        },
        label = "Teléfono",
        keyboardType = KeyboardType.Number,
        isError = warning || incomplete,
        errorText = if (warning) "Solo se permiten números"
        else "Deben ser 10 dígitos (${value.length}/10)"
    )
}

// Campo numérico genérico (costo, capacidad)
@Composable
private fun DigitsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxLength: Int
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

    GymTextField(
        value = value,
        onValueChange = { new ->
            when {
                !new.all { it in '0'..'9' } -> tick++
                new.length <= maxLength -> onValueChange(new)
            }
        },
        label = label,
        keyboardType = KeyboardType.Number,
        isError = warning,
        errorText = "Solo se permiten números"
    )
}

@Composable
private fun GymTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = if (isError) GymColors.Red else GymColors.TextSecondary) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError && errorText != null) {
            { Text(errorText) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = fieldColors()
    )
}