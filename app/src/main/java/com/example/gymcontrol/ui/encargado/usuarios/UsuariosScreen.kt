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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.window.DialogProperties
import com.example.gymcontrol.R
import com.example.gymcontrol.data.model.Status
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.data.remote.CorreoRepository
import com.example.gymcontrol.data.remote.PersonalRepository
import com.example.gymcontrol.data.remote.PersonalRow
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.components.ClienteTema
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Pagos", "Gastos", "Reportes")

private val CardBorder = Brush.linearGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel))

private val DeleteBorder = Brush.linearGradient(
    listOf(Color(0xFFFF6B6B), ClienteTema.Rojo, Color(0xFF7A0A0A))
)

private val STAFF_ROLES = listOf(UserRole.RECEPCION, UserRole.INSTRUCTOR, UserRole.ENCARGADO)

private fun nameHasMinLetters(s: String, min: Int = 3) = s.count { it.isLetter() } >= min

@Composable
fun UserRole.toTranslatedString(): String = when (this) {
    UserRole.RECEPCION -> stringResource(R.string.role_reception)
    UserRole.INSTRUCTOR -> stringResource(R.string.role_instructor)
    UserRole.ENCARGADO -> stringResource(R.string.role_manager)
    else -> this.name
}

@Composable
private fun Status.etiqueta(): String = when (this) {
    Status.ACTIVO -> stringResource(R.string.status_active)
    Status.INACTIVO -> stringResource(R.string.status_inactive)
}

private class StaffItem(
    val id: Long,
    val name: String,
    val email: String,
    val role: UserRole,
    val status: Status
)

private class Pendiente(val mensaje: String, val accion: () -> Unit)

private fun mensajeError(e: Exception, porDefecto: String): String =
    e.message?.substringBefore("\nCode:") ?: porDefecto

@Composable
fun UsuariosScreen(
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
                    colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Azul),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_add_staff), fontWeight = FontWeight.Bold)
                }
            }

            errorMsg?.let { msg ->
                item { Text(msg, color = ClienteTema.Rojo) }
            }
            info?.let { msg ->
                item { Text(msg, color = ClienteTema.Verde, fontSize = 13.sp) }
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
        val confirmCreateMsg = stringResource(R.string.confirm_add_staff, "%s", "%s")
        val successMsg = stringResource(R.string.staff_added_success)
        val warningMsg = stringResource(R.string.staff_added_warning)
        NewStaffDialog(
            error = errorAlta,
            onDismiss = {
                showNewForm = false
                errorAlta = null
            },
            onSave = { nombre, tel, correo, rol, costo, cap, roleName ->
                val msg = String.format(Locale.getDefault(), confirmCreateMsg, nombre, roleName)
                pendiente = Pendiente(msg) {
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
                            info = if (enviado) successMsg else warningMsg
                        } catch (e: Exception) {
                            errorAlta = mensajeError(e, "No se pudo crear")
                        }
                    }
                }
            }
        )
    }

    editing?.let { target ->
        val confirmSaveMsg = stringResource(R.string.confirm_edit_staff, "%s")
        EditStaffDialog(
            target = target,
            error = errorEditar,
            onDismiss = {
                editing = null
                errorEditar = null
            },
            onSave = { nombre, correo, rol, estado, tel, costo, cap ->
                val msg = String.format(Locale.getDefault(), confirmSaveMsg, target.name)
                pendiente = Pendiente(msg) {
                    scope.launch {
                        try {
                            PersonalRepository.editar(
                                SesionActual.correo ?: "", target.id,
                                nombre, correo, rol.name, estado.name,
                                tel, costo, cap
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
        val deleteMsg = stringResource(R.string.delete_staff_confirm, target.name)
        AlertDialog(
            onDismissRequest = {
                deleting = null
                errorBaja = null
            },
            modifier = Modifier.border(1.5.dp, DeleteBorder, RoundedCornerShape(16.dp)),
            containerColor = ClienteTema.Beige,
            shape = RoundedCornerShape(16.dp),
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = ClienteTema.Rojo) },
            title = {
                Text(
                    stringResource(R.string.title_delete_staff),
                    color = ClienteTema.Texto,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        deleteMsg,
                        color = ClienteTema.TextoSec,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorBaja?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = ClienteTema.Rojo, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    OutlinedButton(
                        onClick = {
                            deleting = null
                            errorBaja = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ClienteTema.Borde),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                    ) { Text(stringResource(R.string.cancel)) }

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
                            containerColor = ClienteTema.Rojo,
                            contentColor = Color.White
                        )
                    ) { Text(stringResource(R.string.common_delete), fontWeight = FontWeight.Bold) }
                }
            }
        )
    }
}

@Composable
private fun ConfirmarDialog(pendiente: Pendiente, onCancelar: () -> Unit, onAceptar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = ClienteTema.Beige,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                stringResource(R.string.common_confirm),
                color = ClienteTema.Texto,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                pendiente.mensaje,
                color = ClienteTema.TextoSec,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(
                    onClick = onCancelar,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClienteTema.Borde),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                ) { Text(stringResource(R.string.cancel)) }

                Button(
                    onClick = onAceptar,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Azul)
                ) { Text(stringResource(R.string.common_yes_continue), fontWeight = FontWeight.Bold) }
            }
        }
    )
}

@Composable
private fun StaffCard(item: StaffItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val statusColor = if (item.status == Status.ACTIVO) ClienteTema.Verde else ClienteTema.Rojo
    val statusText = item.status.etiqueta()
    val roleText = item.role.toTranslatedString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClienteTema.Beige, RoundedCornerShape(14.dp))
            .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ClienteTema.AzulSel.copy(alpha = 0.18f), CircleShape)
                .border(2.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = ClienteTema.TextoSec,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = ClienteTema.Texto,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = roleText,
                color = ClienteTema.AzulSel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.email,
                color = ClienteTema.TextoSec,
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
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconCircleButton(icon = Icons.Filled.Edit, color = ClienteTema.AzulSel, onClick = onEdit)
            IconCircleButton(icon = Icons.Filled.Close, color = ClienteTema.Rojo, onClick = onDelete)
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
                .background(ClienteTema.Beige, RoundedCornerShape(16.dp))
                .border(1.5.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = ClienteTema.Texto,
                    fontSize = 18.sp,
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
            content()
        }
    }
}

@Composable
private fun NewStaffDialog(
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, UserRole, String, String, String) -> Unit
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

    val roleName = role.toTranslatedString()

    StaffDialog(title = stringResource(R.string.encargado_usuarios_new_staff_title), onDismiss = onDismiss) {
        NameField(name, { name = it })
        Spacer(Modifier.height(10.dp))
        PhoneField(phone, { phone = it })
        Spacer(Modifier.height(10.dp))
        GymTextField(email, { email = it }, stringResource(R.string.encargado_usuarios_email_assigned), KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = ClienteTema.Navy,
            border = BorderStroke(1.dp, ClienteTema.Borde.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = ClienteTema.AzulSel,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.encargado_usuarios_initial_pwd_notice),
                    color = ClienteTema.TextoSec,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.label_role),
            color = ClienteTema.Texto,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            listOf(
                UserRole.RECEPCION,
                UserRole.INSTRUCTOR
            ).forEach { value ->
                val label = value.toTranslatedString()
                FilterChip(
                    selected = role == value,
                    onClick = { role = value },
                    label = { Text(label, fontWeight = if (role == value) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ClienteTema.Navy,
                        labelColor = ClienteTema.Texto,
                        selectedContainerColor = ClienteTema.Azul,
                        selectedLabelColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (role == value) ClienteTema.Azul else ClienteTema.Borde
                    )
                )
            }
        }

        if (isInstructor) {
            Spacer(Modifier.height(10.dp))
            DigitsField(cost, { cost = it }, stringResource(R.string.encargado_usuarios_advisory_cost_label), maxLength = 6)
            Spacer(Modifier.height(10.dp))
            DigitsField(capacity, { capacity = it }, stringResource(R.string.encargado_usuarios_max_capacity_label), maxLength = 3)
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = ClienteTema.Rojo, fontSize = 13.sp)
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = { onSave(name.trim(), phone, email, role, cost, capacity, roleName) },
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
            Text(stringResource(R.string.encargado_usuarios_create_staff_button), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditStaffDialog(
    target: StaffItem,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, UserRole, Status, String?, Double?, Int?) -> Unit
) {
    var name by remember { mutableStateOf(target.name) }
    var email by remember { mutableStateOf(target.email) }
    var role by remember { mutableStateOf(target.role) }
    var status by remember { mutableStateOf(target.status) }
    var roleExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    var phone by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var detalleCargado by remember { mutableStateOf(false) }
    var errorDetalle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(target.id) {
        try {
            PersonalRepository.detalle(target.id)?.let { d ->
                phone = d.telefono
                cost = d.costo?.let { c -> if (c % 1.0 == 0.0) c.toLong().toString() else c.toString() } ?: ""
                capacity = d.maxClientes?.toString() ?: ""
            }
            detalleCargado = true
        } catch (e: Exception) {
            errorDetalle = mensajeError(e, "No se pudieron cargar teléfono, costo y capacidad")
        }
    }

    val isInstructor = role == UserRole.INSTRUCTOR
    val phoneOk = phone.isEmpty() || phone.length == 10
    val instructorOk = !isInstructor || !detalleCargado ||
            ((cost.toDoubleOrNull() ?: -1.0) >= 0.0 && (capacity.toIntOrNull() ?: 0) >= 1)

    StaffDialog(title = stringResource(R.string.encargado_usuarios_edit_staff_title), onDismiss = onDismiss) {
        NameField(name, { name = it })
        Spacer(Modifier.height(10.dp))
        if (detalleCargado) {
            PhoneField(phone, { phone = it })
            Spacer(Modifier.height(10.dp))
        }
        GymTextField(email, { email = it }, stringResource(R.string.label_email), KeyboardType.Email)
        Spacer(Modifier.height(10.dp))

        ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
            OutlinedTextField(
                value = role.toTranslatedString(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_role), color = ClienteTema.TextoSec) },
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
                expanded = roleExpanded,
                onDismissRequest = { roleExpanded = false },
                modifier = Modifier.background(ClienteTema.Beige)
            ) {
                STAFF_ROLES.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r.toTranslatedString(), color = ClienteTema.Texto) },
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
                value = status.etiqueta(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.encargado_usuarios_status_label), color = ClienteTema.TextoSec) },
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
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false },
                modifier = Modifier.background(ClienteTema.Beige)
            ) {
                Status.values().forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.etiqueta(), color = ClienteTema.Texto) },
                        onClick = {
                            status = s
                            statusExpanded = false
                        }
                    )
                }
            }
        }

        if (isInstructor && detalleCargado) {
            Spacer(Modifier.height(10.dp))
            DigitsField(cost, { cost = it }, stringResource(R.string.encargado_usuarios_advisory_cost_label), maxLength = 6)
            Spacer(Modifier.height(10.dp))
            DigitsField(capacity, { capacity = it }, stringResource(R.string.encargado_usuarios_max_capacity_label), maxLength = 3)
        }

        (error ?: errorDetalle)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = ClienteTema.Rojo, fontSize = 13.sp)
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                onSave(
                    name.trim(), email.trim(), role, status,
                    if (detalleCargado) phone else null,
                    if (detalleCargado && isInstructor) cost.toDoubleOrNull() else null,
                    if (detalleCargado && isInstructor) capacity.toIntOrNull() else null
                )
            },
            enabled = nameHasMinLetters(name) && email.isNotBlank() && phoneOk && instructorOk,
            colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Azul),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
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
    val labelName = stringResource(R.string.perfil_field_name)

    GymTextField(
        value = value,
        onValueChange = { new ->
            when {
                !new.all { it.isLetter() || it == ' ' } -> tick++
                new.length <= 40 -> onValueChange(new)
            }
        },
        label = labelName,
        isError = warning || tooShort,
        errorText = if (warning) stringResource(R.string.perfil_error_letters_only) else stringResource(R.string.perfil_error_min_letters)
    )
}

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
    val labelPhone = stringResource(R.string.label_phone)

    GymTextField(
        value = value,
        onValueChange = { new ->
            when {
                !new.all { it in '0'..'9' } -> tick++
                new.length <= 10 -> onValueChange(new)
            }
        },
        label = labelPhone,
        keyboardType = KeyboardType.Number,
        isError = warning || incomplete,
        errorText = if (warning) stringResource(R.string.perfil_error_phone_digits)
        else stringResource(R.string.error_phone_10_digits, value.length)
    )
}

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

    val errorDigits = stringResource(R.string.perfil_error_phone_digits)

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
        errorText = errorDigits
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
        label = { Text(label, color = if (isError) ClienteTema.Rojo else ClienteTema.TextoSec) },
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
