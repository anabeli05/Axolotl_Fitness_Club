package com.example.gymcontrol.ui.encargado.clientes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.ClienteRow
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.data.remote.ContactoEmergencia
import com.example.gymcontrol.data.remote.DatosSalud
import com.example.gymcontrol.data.remote.CorreoRepository
import com.example.gymcontrol.data.remote.PlanRow
import com.example.gymcontrol.ui.components.SesionActual
import kotlinx.coroutines.launch
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.FotoClienteAvatar
import com.example.gymcontrol.ui.components.FotoClienteDialog
import androidx.compose.ui.graphics.ImageBitmap
import java.util.Locale

private val CardBorder = Brush.linearGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel))
private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Pagos", "Gastos", "Reportes")

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
    var viendoFoto by remember { mutableStateOf<Pair<ClienteRow, ImageBitmap>?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val msgRegisterFormat = stringResource(R.string.encargado_confirm_register_client, "%s")
    val msgSaveFormat = stringResource(R.string.encargado_confirm_save_client, "%s")
    val msgRenewFormat = stringResource(R.string.encargado_confirm_renew_client, "%s", "%s")

    suspend fun recargar() {
        try {
            todos = ClientesRepository.listar()
            errorMsg = null
        } catch (e: Exception) {
            errorMsg = "No se pudo cargar: ${e.message}"
        }
    }

    fun mensaje(e: Exception, def: String) = e.message?.substringBefore("\nCode:") ?: def

    LaunchedEffect(Unit) {
        recargar()
        try { planes = ClientesRepository.planesEncargado() } catch (_: Exception) {}
    }

    val clients = todos.filter {
        it.nombre.contains(search, true) || it.numeroMembresia.contains(search, true)
    }

    GymScaffold(
        currentSection = "Clientes",
        sections = ADMIN_SECTIONS,
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
                        colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Azul),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_new_client), fontWeight = FontWeight.Bold)
                    }
                }

                errorMsg?.let { msg -> item { Text(msg, color = ClienteTema.Rojo, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) } }
                info?.let { msg -> item { Text(msg, color = ClienteTema.Verde, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) } }

                itemsIndexed(clients) { _, client ->
                    ClientCard(
                        clienteId = client.id,
                        name = client.nombre,
                        membershipNum = client.numeroMembresia,
                        fechaFin = client.fechaFin,
                        registrationDate = ClientesRepository.fechaApp(context, client.fechaRegistro),
                        isActive = client.estado == "ACTIVO" && client.activa,
                        onEdit = {
                            editing = client
                            errorForm = null
                            showForm = true
                        },
                        onRenovar = { renovando = client },
                        onFoto = { foto -> viendoFoto = client to foto }
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
                // Al editar se carga el contacto de emergencia que ya tiene el cliente
                var contacto by remember(c?.id) { mutableStateOf<ContactoEmergencia?>(null) }
                // y también su fecha de nacimiento y datos de salud
                val msgSaludFallo = stringResource(R.string.client_form_health_save_failed)
                var salud by remember(c?.id) { mutableStateOf<DatosSalud?>(null) }
                LaunchedEffect(c?.id) {
                    if (c != null) contacto = try {
                        ClientesRepository.contactoEmergencia(SesionActual.correo ?: "", c.id)
                    } catch (_: Exception) { null }
                }
                LaunchedEffect(c?.id) {
                    if (c != null) salud = try {
                        ClientesRepository.datosSalud(SesionActual.correo ?: "", c.id)
                    } catch (_: Exception) { null }
                }
                ClientFormDialog(
                    isEditing = c != null,
                    plans = planes.map { it.nombre },
                    error = errorForm,
                    initialName = partes?.getOrNull(0) ?: "",
                    initialLastName = partes?.getOrNull(1) ?: "",
                    initialPhone = c?.telefono ?: "",
                    initialEmail = c?.gmail ?: "",
                    initialContactName = contacto?.nombre ?: "",
                    initialContactPhone = contacto?.telefono ?: "",
                    initialSalud = salud ?: DatosSalud(),
                    onDismiss = {
                        showForm = false
                        editing = null
                        errorForm = null
                    },
                    onSave = { nombre, telefono, correo, plan, contactoNombre, contactoTelefono, metodo, datosSalud ->
                        if (c == null) {
                            confirmar = String.format(Locale.getDefault(), msgRegisterFormat, nombre) to {
                                scope.launch {
                                    try {
                                        val nuevo = ClientesRepository.crear(
                                            nombre, telefono, correo, plan ?: "",
                                            SesionActual.correo ?: "",
                                            contactoNombre, contactoTelefono, metodo
                                        )
                                        // El cliente ya quedó creado; si esto falla solo se avisa
                                        val saludGuardada = runCatching {
                                            ClientesRepository.guardarDatosSalud(SesionActual.correo ?: "", nuevo.id, datosSalud)
                                        }.isSuccess
                                        showForm = false
                                        errorForm = null
                                        recargar()
                                        val enviado = CorreoRepository.enviarBienvenida(correo)
                                        info = if (enviado) "Cliente ${nuevo.numero} registrado. Se envió su membresía y contraseña al correo."
                                        else "Cliente ${nuevo.numero} registrado, pero no se pudo enviar el correo. Contraseña inicial: Axolotl2026"
                                        if (!saludGuardada) info = "$info\n$msgSaludFallo"
                                    } catch (e: Exception) {
                                        errorForm = mensaje(e, "No se pudo registrar")
                                    }
                                }
                                Unit
                            }
                        } else {
                            confirmar = String.format(Locale.getDefault(), msgSaveFormat, c.nombre) to {
                                scope.launch {
                                    try {
                                        ClientesRepository.editar(c.id, nombre, telefono, correo, c.estado, contactoNombre, contactoTelefono)
                                        ClientesRepository.guardarDatosSalud(SesionActual.correo ?: "", c.id, datosSalud)
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

            viendoFoto?.let { (cl, foto) ->
                FotoClienteDialog(nombre = cl.nombre, foto = foto, onDismiss = { viendoFoto = null })
            }

            renovando?.let { cl ->
                RenovarDialog(
                    cliente = cl,
                    planes = planes,
                    onDismiss = { renovando = null },
                    onRenovar = { plan ->
                        renovando = null
                        confirmar = String.format(Locale.getDefault(), msgRenewFormat, cl.nombre, plan) to {
                            scope.launch {
                                try {
                                    val vence = ClientesRepository.renovar(cl.id, plan, SesionActual.correo ?: "")
                                    recargar()
                                    val renovada = "Membresía de ${cl.nombre} renovada hasta ${ClientesRepository.fechaApp(vence)}. Pago registrado."
                                    info = renovada
                                    // Pago en efectivo ya aceptado: el comprobante va al correo registrado del cliente
                                    if (vence != null) {
                                        val falla = CorreoRepository.enviarRecibo(cl.id)
                                        if (falla == null) info = "$renovada Comprobante enviado a ${cl.gmail}."
                                        else {
                                            info = null
                                            errorMsg = "$renovada No se pudo enviar el comprobante por correo: $falla"
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMsg = mensaje(e, "No se pudo renovar")
                                }
                            }
                            Unit
                        }
                    }
                )
            }

            confirmar?.let { (texto, accion) ->
                AlertDialog(
                    onDismissRequest = { confirmar = null },
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
                            texto,
                            color = ClienteTema.TextoSec,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                confirmar = null
                                accion()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Azul)
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
    clienteId: Long,
    name: String,
    membershipNum: String,
    fechaFin: String?,
    registrationDate: String,
    isActive: Boolean,
    onEdit: () -> Unit,
    onRenovar: () -> Unit,
    onFoto: (ImageBitmap) -> Unit = {}
) {
    val context = LocalContext.current
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
        // Foto de perfil; al tocarla se ve en grande
        FotoClienteAvatar(
            clienteId = clienteId,
            size = 52.dp,
            onClick = { foto -> if (foto != null) onFoto(foto) }
        )

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
                    text = stringResource(R.string.label_expires_format, ClientesRepository.fechaApp(context, fechaFin)),
                    color = ClienteTema.TextoSec,
                    fontSize = 17.sp
                )
            }
            Spacer(Modifier.height(2.dp))
            Text("$labelRegistration: $registrationDate", color = ClienteTema.TextoSec, fontSize = 17.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            StatusBadge(isActive = isActive)
            Spacer(Modifier.height(10.dp))

            // Editar siempre está disponible, aunque el plan no esté vigente
            IconSquareButton(
                icon = Icons.Filled.Edit,
                color = ClienteTema.AzulSel,
                onClick = onEdit,
                modifier = Modifier.size(ActionButtonWidth, ActionButtonHeight)
            )
            if (!isActive) {
                Spacer(Modifier.height(8.dp))
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
    color: androidx.compose.ui.graphics.Color,
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
                            colors = RadioButtonDefaults.colors(selectedColor = ClienteTema.AzulSel)
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
                    colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Azul)
                ) { Text(stringResource(R.string.encargado_clients_renew_button), fontWeight = FontWeight.Bold) }
            }
        }
    )
}
