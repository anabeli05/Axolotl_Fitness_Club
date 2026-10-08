package com.example.gymcontrol.ui.recepcion.solicitudes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.data.remote.SolicitudAdminRow
import com.example.gymcontrol.data.remote.SolicitudesRepository
import kotlinx.coroutines.launch
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val CardBorder = Brush.linearGradient(listOf(GymColors.Purple, GymColors.Gold))
private val RECEPCION_SECTIONS = listOf("Home", "Clientes", "Solicitudes")

// Mismo tamaño que en ClientesScreen: badge, chip de costo y botones de acción
private val ChipWidth = 96.dp
private val ChipHeight = 36.dp
private val ActionWidth = 44.dp // dos botones (44 + 8 + 44) = 96.dp

// Resolución pendiente de confirmar
private class Resolucion(val solicitud: SolicitudAdminRow, val estado: String)

@Composable
fun SolicitudesScreen(onNavigate: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<SolicitudAdminRow>>(emptyList()) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf<Resolucion?>(null) }

    suspend fun recargar() {
        try {
            requests = SolicitudesRepository.listar()
        } catch (e: Exception) {
            mensaje = "No se pudo cargar: ${e.message?.substringBefore("\nCode:")}"
        }
    }

    LaunchedEffect(Unit) { recargar() }

    val pendientes = requests.count { it.estado == "PENDIENTE" }

    GymScaffold(
        currentSection = "Solicitudes",
        sections = RECEPCION_SECTIONS,
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
                Text(
                    text = "Pendientes: $pendientes",
                    color = GymColors.TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            mensaje?.let { msg ->
                item { Text(msg, color = GymColors.Red, fontSize = 14.sp) }
            }

            items(requests, key = { it.id }) { req ->
                SolicitudCard(
                    clientName = req.cliente,
                    membership = req.numeroMembresia.ifBlank { "—" },
                    instructor = req.instructor,
                    cost = if (req.costo % 1.0 == 0.0) "%.0f".format(req.costo) else "%.2f".format(req.costo),
                    date = ClientesRepository.fechaApp(req.fecha),
                    status = req.estado,
                    onApprove = { confirmar = Resolucion(req, "APROBADA") },
                    onReject = { confirmar = Resolucion(req, "RECHAZADA") }
                )
            }

            if (requests.isEmpty() && mensaje == null) {
                item {
                    Text(
                        text = "No hay solicitudes",
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
    }

    confirmar?.let { r ->
        val aprobar = r.estado == "APROBADA"
        AlertDialog(
            onDismissRequest = { confirmar = null },
            containerColor = GymColors.Surface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Confirmar", color = GymColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Estás seguro de ${if (aprobar) "aprobar" else "rechazar"} la solicitud de " +
                            "${r.solicitud.cliente} con ${r.solicitud.instructor}?",
                    color = GymColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmar = null
                        scope.launch {
                            try {
                                SolicitudesRepository.resolver(r.solicitud.id, r.estado)
                                mensaje = null
                            } catch (e: Exception) {
                                mensaje = e.message?.substringBefore("\nCode:") ?: "No se pudo guardar"
                            }
                            recargar()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (aprobar) GymColors.Green else GymColors.Red,
                        contentColor = Color.White
                    )
                ) { Text(if (aprobar) "Aprobar" else "Rechazar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { confirmar = null },
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GymColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.TextPrimary)
                ) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SolicitudCard(
    clientName: String,
    membership: String,
    instructor: String,
    cost: String,
    date: String,
    status: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
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
                text = clientName,
                color = GymColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text("Membresía: $membership", color = GymColors.TextSecondary, fontSize = 17.sp)
            Spacer(Modifier.height(2.dp))
            Text("Instructor: $instructor", color = GymColors.TextSecondary, fontSize = 17.sp)
            Spacer(Modifier.height(2.dp))
            Text("Fecha: $date", color = GymColors.TextSecondary, fontSize = 17.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            StatusChip(status = status)
            Spacer(Modifier.height(10.dp))
            CostChip(cost = cost)

            // Los botones solo aparecen mientras la solicitud está pendiente
            if (status == "PENDIENTE") {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconSquareButton(
                        icon = Icons.Filled.Check,
                        color = GymColors.Green,
                        description = "Aprobar",
                        onClick = onApprove
                    )
                    IconSquareButton(
                        icon = Icons.Filled.Close,
                        color = GymColors.Red,
                        description = "Rechazar",
                        onClick = onReject
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        "APROBADA" -> GymColors.Green
        "RECHAZADA" -> GymColors.Red
        else -> GymColors.Gold
    }
    val label = when (status) {
        "APROBADA" -> "Aprobada"
        "RECHAZADA" -> "Rechazada"
        else -> "Pendiente"
    }

    Box(
        modifier = Modifier
            .size(ChipWidth, ChipHeight)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .border(1.dp, color, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CostChip(cost: String) {
    Box(
        modifier = Modifier
            .size(ChipWidth, ChipHeight)
            .background(GymColors.Purple.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .border(1.dp, GymColors.Purple, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$$cost",
            color = GymColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IconSquareButton(
    icon: ImageVector,
    color: Color,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(ActionWidth, ChipHeight)
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .border(1.dp, color, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = description, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}