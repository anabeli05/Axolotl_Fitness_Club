package com.example.gymcontrol.ui.recepcion.clientes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.AccesoFacialRepository
import com.example.gymcontrol.data.remote.ClienteRow
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.data.remote.CorreoRepository
import com.example.gymcontrol.data.remote.PlanRow
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.SesionActual
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardBorder = Brush.linearGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel))

private val ActionButtonWidth = 96.dp
private val ActionButtonHeight = 36.dp

@Composable
fun ClientesScreen(onNavigate: (String) -> Unit = {}) {
    var search by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ClienteRow?>(null) }
    var todos by remember { mutableStateOf<List<ClienteRow>>(emptyList()) }
    var planes by remember { mutableStateOf<List<PlanRow>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var errorForm by remember { mutableStateOf<String?>(null) }
    var renovando by remember { mutableStateOf<ClienteRow?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var conRostro by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var registrandoRostro by remember { mutableStateOf<ClienteRow?>(null) }
    var dentro by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var confirmar by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val scope = rememberCoroutineScope()

    val msgRegister = stringResource(R.string.encargado_confirm_register_client, "%s")
    val msgSave = stringResource(R.string.encargado_confirm_save_client, "%s")
    val msgRenew = stringResource(R.string.encargado_confirm_renew_client, "%s", "%s")

    suspend fun recargar() {
        try {
            todos = ClientesRepository.listar()
            errorMsg = null
        } catch (e: Exception) {
            errorMsg = "No se pudo cargar: ${e.message}"
        }
        try { conRostro = AccesoFacialRepository.clientesConRostro() } catch (_: Exception) {}
    }

    fun mensaje(e: Exception, def: String) = e.message?.substringBefore("\nCode:") ?: def

    LaunchedEffect(Unit) {
        recargar()
        try { planes = ClientesRepository.planes() } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        launch {
            try {
                AccesoFacialRepository.movimientos().collect {
                    try { dentro = AccesoFacialRepository.clientesDentro() } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        while (true) {
            try { dentro = AccesoFacialRepository.clientesDentro() } catch (_: Exception) {}
            delay(30_000)
        }
    }

    val clients = todos.filter {
        it.nombre.contains(search, true) || it.numeroMembresia.contains(search, true)
    }

    GymScaffold(
        currentSection = "Clientes",
        onNavigate = onNavigate
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text(stringResource(R.string.placeholder_search_client), color = ClienteTema.TextoSec) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = ClienteTema.TextoSec)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClienteTema.AzulSel,
                            unfocusedBorderColor = ClienteTema.Borde,
                            focusedTextColor = ClienteTema.Texto,
                            unfocusedTextColor = ClienteTema.Texto,
                            cursorColor = ClienteTema.AzulSel
                        )
                    )
                }

                item {
                    Button(
                        onClick = {
                            editing = null
                            errorForm = null
                            showForm = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClienteTema.AzulSel,
                            contentColor = ClienteTema.Navy
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_new_client), fontWeight = FontWeight.Bold)
                    }
                }

                errorMsg?.let { msg -> item { Text(msg, color = ClienteTema.Rojo) } }
                info?.let { msg -> item { Text(msg, color = ClienteTema.Verde, fontSize = 13.sp) } }

                itemsIndexed(clients) { _, client ->
                    ClientCard(
                        name = client.nombre,
                        membershipNum = client.numeroMembresia,
                        fechaFin = client.fechaFin,
                        registrationDate = ClientesRepository.fechaApp(client.fechaRegistro),
                        isActive = client.estado == "ACTIVO" && client.activa,
                        tieneRostro = client.id in conRostro,
                        dentroDesde = dentro[client.id],
                        onRostro = { registrandoRostro = client },
                        onEdit = {
                            editing = client
                            errorForm = null
                            showForm = true
                        },
                        onRenovar = { renovando = client }
                    )
                }

                if (clients.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.encargado_clients_not_found),
                            color = ClienteTema.TextoSec,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (showForm) {
                val c = editing
                val partes = c?.nombre?.trim()?.split(" ", limit = 2)
                ClientFormDialog(
                    isEditing = c != null,
                    plans = planes.map { it.nombre },
                    error = errorForm,
                    initialName = partes?.getOrNull(0) ?: "",
                    initialLastName = partes?.getOrNull(1) ?: "",
                    initialPhone = c?.telefono ?: "",
                    initialEmail = c?.gmail ?: "",
                    onDismiss = {
                        showForm = false
                        editing = null
                        errorForm = null
                    },
                    onSave = { nombre, telefono, correo, plan ->
                        if (c == null) {
                            confirmar = String.format(Locale.getDefault(), msgRegister, nombre) to {
                                scope.launch {
                                    try {
                                        val nuevo = ClientesRepository.crear(
                                            nombre, telefono, correo, plan ?: "",
                                            SesionActual.correo ?: ""
                                        )
                                        showForm = false
                                        errorForm = null
                                        recargar()
                                        val enviado = CorreoRepository.enviarBienvenida(correo)
                                        info = if (enviado) "Cliente ${nuevo.numero} registrado. Se envió su membresía y contraseña al correo."
                                        else "Cliente ${nuevo.numero} registrado, pero no se pudo enviar el correo. Contraseña inicial: Axolotl2026"
                                        registrandoRostro = todos.firstOrNull { it.id == nuevo.id }
                                    } catch (e: Exception) {
                                        errorForm = mensaje(e, "No se pudo registrar")
                                    }
                                }
                                Unit
                            }
                        } else {
                            confirmar = String.format(Locale.getDefault(), msgSave, c.nombre) to {
                                scope.launch {
                                    try {
                                        ClientesRepository.editar(c.id, nombre, telefono, correo, c.estado)
                                        showForm = false
                                        editing = null
                                        errorForm = null
                                        recargar()
                                    } catch (e: Exception) {
                                        errorForm = mensaje(e, "No se pudo guardar")
                                    }
                                }
                                Unit
                            }
                        }
                    }
                )
            }

            renovando?.let { cl ->
                RenovarDialog(
                    cliente = cl,
                    planes = planes,
                    onDismiss = { renovando = null },
                    onRenovar = { plan ->
                        renovando = null
                        confirmar = String.format(Locale.getDefault(), msgRenew, cl.nombre, plan) to {
                            scope.launch {
                                try {
                                    val vence = ClientesRepository.renovar(cl.id, plan, SesionActual.correo ?: "")
                                    recargar()
                                    info = "Membresía de ${cl.nombre} renovada hasta ${ClientesRepository.fechaApp(vence)}. Pago registrado."
                                } catch (e: Exception) {
                                    errorMsg = mensaje(e, "No se pudo renovar")
                                }
                            }
                            Unit
                        }
                    }
                )
            }

            registrandoRostro?.let { cl ->
                RegistroRostroDialog(
                    cliente = cl,
                    yaTieneRostro = cl.id in conRostro,
                    onDismiss = { registrandoRostro = null },
                    onRegistrado = {
                        conRostro = conRostro + cl.id
                        registrandoRostro = null
                        info = "Rostro de ${cl.nombre} registrado para el acceso"
                    }
                )
            }

            confirmar?.let { (texto, accion) ->
                AlertDialog(
                    onDismissRequest = { confirmar = null },
                    containerColor = ClienteTema.Beige,
                    shape = RoundedCornerShape(16.dp),
                    title = { Text(stringResource(R.string.common_confirm), color = ClienteTema.Texto, fontWeight = FontWeight.Bold) },
                    text = { Text(texto, color = ClienteTema.TextoSec) },
                    confirmButton = {
                        Button(
                            onClick = {
                                confirmar = null
                                accion()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClienteTema.AzulSel,
                                contentColor = ClienteTema.Navy
                            )
                        ) { Text(stringResource(R.string.common_yes_continue), fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { confirmar = null },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ClienteTema.Borde),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                        ) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
        }
    }
}

@Composable
private fun ClientCard(
    name: String,
    membershipNum: String,
    fechaFin: String?,
    registrationDate: String,
    isActive: Boolean,
    tieneRostro: Boolean = false,
    dentroDesde: String? = null,
    onRostro: () -> Unit = {},
    onEdit: () -> Unit,
    onRenovar: () -> Unit
) {
    val labelMembership = stringResource(R.string.label_membership)
    val labelRegistration = stringResource(R.string.label_registration)

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
                .background(ClienteTema.Navy, CircleShape)
                .border(
                    if (tieneRostro) 2.dp else 1.dp,
                    if (tieneRostro) ClienteTema.Verde else ClienteTema.Borde,
                    CircleShape
                )
                .clip(CircleShape)
                .clickable(onClick = onRostro),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = ClienteTema.TextoSec,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = ClienteTema.Texto,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$labelMembership: " + membershipNum.ifBlank { "—" },
                color = ClienteTema.TextoSec,
                fontSize = 17.sp
            )
            if (fechaFin != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.label_expires_format, ClientesRepository.fechaApp(fechaFin)),
                    color = ClienteTema.TextoSec,
                    fontSize = 17.sp
                )
            }
            Spacer(Modifier.height(2.dp))
            Text("$labelRegistration: $registrationDate", color = ClienteTema.TextoSec, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            AccesoBadge(dentroDesde)
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            StatusBadge(isActive = isActive)
            Spacer(Modifier.height(10.dp))

            if (isActive) {
                IconSquareButton(
                    icon = Icons.Filled.Edit,
                    color = ClienteTema.AzulSel,
                    onClick = onEdit,
                    modifier = Modifier.size(ActionButtonWidth, ActionButtonHeight)
                )
            } else {
                OutlinedButton(
                    onClick = onRenovar,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.AzulSel),
                    border = BorderStroke(1.dp, ClienteTema.AzulSel),
                    modifier = Modifier.size(ActionButtonWidth, ActionButtonHeight),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(stringResource(R.string.encargado_clients_renew_button), fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun IconSquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .border(1.dp, color, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = stringResource(R.string.common_edit), tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AccesoBadge(dentroDesde: String?) {
    val color = if (dentroDesde != null) ClienteTema.Verde else ClienteTema.TextoSec
    val labelInsideSince = stringResource(R.string.label_inside_since, horaEntrada(dentroDesde ?: ""))
    val labelOutside = stringResource(R.string.label_outside)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (dentroDesde != null) labelInsideSince else labelOutside,
            color = color,
            fontSize = 15.sp,
            fontWeight = if (dentroDesde != null) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private val HORA = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

private fun horaEntrada(iso: String): String =
    runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(HORA) }
        .getOrDefault("—")

@Composable
private fun StatusBadge(isActive: Boolean) {
    val color = if (isActive) ClienteTema.Verde else ClienteTema.Rojo
    val label = if (isActive) stringResource(R.string.status_active) else stringResource(R.string.status_inactive)

    Box(
        modifier = Modifier
            .size(ActionButtonWidth, ActionButtonHeight)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RenovarDialog(
    cliente: ClienteRow,
    planes: List<PlanRow>,
    onDismiss: () -> Unit,
    onRenovar: (String) -> Unit
) {
    var plan by remember { mutableStateOf(cliente.plan ?: planes.firstOrNull()?.nombre ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ClienteTema.Beige,
        shape = RoundedCornerShape(16.dp),
        title = { Text(stringResource(R.string.encargado_clients_renew_title, cliente.nombre), color = ClienteTema.Texto, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.encargado_clients_choose_plan), color = ClienteTema.TextoSec)
                Spacer(Modifier.height(8.dp))
                planes.forEach { p ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { plan = p.nombre }
                    ) {
                        RadioButton(
                            selected = plan == p.nombre,
                            onClick = { plan = p.nombre },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ClienteTema.AzulSel,
                                unselectedColor = ClienteTema.TextoSec
                            )
                        )
                        Text("${p.nombre} · $${p.precio.toInt()}", color = ClienteTema.Texto)
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClienteTema.Borde),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                ) { Text(stringResource(R.string.cancel)) }

                Button(
                    onClick = { onRenovar(plan) },
                    enabled = plan.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.AzulSel,
                        contentColor = ClienteTema.Navy
                    )
                ) { Text(stringResource(R.string.encargado_clients_renew_button), fontWeight = FontWeight.Bold) }
            }
        }
    )
}
