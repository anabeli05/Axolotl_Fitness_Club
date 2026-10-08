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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.data.remote.ClienteRow
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.data.remote.CorreoRepository
import com.example.gymcontrol.data.remote.PlanRow
import com.example.gymcontrol.ui.components.SesionActual
import kotlinx.coroutines.launch
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val CardBorder = Brush.linearGradient(listOf(GymColors.Purple, GymColors.Gold))
private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")

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
    // Pregunta "¿Estás seguro?" pendiente: mensaje + acción
    var confirmar by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val scope = rememberCoroutineScope()

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
        try { planes = ClientesRepository.planes() } catch (_: Exception) {}
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
                        placeholder = { Text("Nombre o membresía", color = GymColors.TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = GymColors.TextSecondary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GymColors.Purple,
                            unfocusedBorderColor = GymColors.Border,
                            focusedTextColor = GymColors.TextPrimary,
                            unfocusedTextColor = GymColors.TextPrimary,
                            cursorColor = GymColors.Gold
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
                        colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nuevo Cliente", fontWeight = FontWeight.Bold)
                    }
                }

                errorMsg?.let { msg -> item { Text(msg, color = GymColors.Red) } }
                info?.let { msg -> item { Text(msg, color = GymColors.Green, fontSize = 13.sp) } }

                itemsIndexed(clients) { _, client ->
                    ClientCard(
                        name = client.nombre,
                        membership = client.numeroMembresia.ifBlank { "—" } +
                                if (client.fechaFin != null) " · vence ${ClientesRepository.fechaApp(client.fechaFin)}"
                                else " · sin membresía",
                        registrationDate = ClientesRepository.fechaApp(client.fechaRegistro),
                        isActive = client.estado == "ACTIVO" && client.activa,
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
                            text = "No se encontraron clientes",
                            color = GymColors.TextSecondary,
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
                            confirmar = "¿Estás seguro de registrar a $nombre?" to {
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
                                    } catch (e: Exception) {
                                        errorForm = mensaje(e, "No se pudo registrar")
                                    }
                                }
                                Unit
                            }
                        } else {
                            confirmar = "¿Estás seguro de guardar los cambios de ${c.nombre}?" to {
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
                        confirmar = "¿Estás seguro de renovar a ${cl.nombre} con el plan $plan?" to {
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

            confirmar?.let { (texto, accion) ->
                AlertDialog(
                    onDismissRequest = { confirmar = null },
                    containerColor = GymColors.Surface,
                    shape = RoundedCornerShape(16.dp),
                    title = { Text("Confirmar", color = GymColors.TextPrimary, fontWeight = FontWeight.Bold) },
                    text = { Text(texto, color = GymColors.TextSecondary) },
                    confirmButton = {
                        Button(
                            onClick = {
                                confirmar = null
                                accion()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple)
                        ) { Text("Sí, continuar", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { confirmar = null },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GymColors.Border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.TextPrimary)
                        ) { Text("Cancelar") }
                    }
                )
            }
        }
    }
}

@Composable
private fun ClientCard(
    name: String,
    membership: String,
    registrationDate: String,
    isActive: Boolean,
    onEdit: () -> Unit,
    onRenovar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GymColors.Surface, RoundedCornerShape(14.dp))
            .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = GymColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text("Membresía: $membership", color = GymColors.TextSecondary, fontSize = 17.sp)
            Spacer(Modifier.height(2.dp))
            Text("Registro: $registrationDate", color = GymColors.TextSecondary, fontSize = 17.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            StatusBadge(isActive = isActive)
            Spacer(Modifier.height(10.dp))

            if (isActive) {
                IconSquareButton(
                    icon = Icons.Filled.Edit,
                    color = GymColors.Orange,
                    onClick = onEdit,
                    modifier = Modifier.size(ActionButtonWidth, ActionButtonHeight)
                )
            } else {
                OutlinedButton(
                    onClick = onRenovar,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.Orange),
                    border = BorderStroke(1.dp, GymColors.Orange),
                    modifier = Modifier.size(ActionButtonWidth, ActionButtonHeight),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Renovar", fontSize = 17.sp)
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
            Icon(icon, contentDescription = "Editar", tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    val color = if (isActive) GymColors.Green else GymColors.Red
    val label = if (isActive) "Activo" else "Inactivo"

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
        containerColor = GymColors.Surface,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Renovar a ${cliente.nombre}", color = GymColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Elige el plan:", color = GymColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                planes.forEach { p ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { plan = p.nombre }
                    ) {
                        RadioButton(
                            selected = plan == p.nombre,
                            onClick = { plan = p.nombre },
                            colors = RadioButtonDefaults.colors(selectedColor = GymColors.Gold)
                        )
                        Text("${p.nombre} · $${p.precio.toInt()}", color = GymColors.TextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRenovar(plan) },
                enabled = plan.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple)
            ) { Text("Renovar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GymColors.Border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.TextPrimary)
            ) { Text("Cancelar") }
        }
    )
}