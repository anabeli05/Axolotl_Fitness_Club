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
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val CardBorder = Brush.linearGradient(listOf(GymColors.Purple, GymColors.Gold))
private val RECEPCION_SECTIONS = listOf("Home", "Clientes", "Solicitudes")

// Mismo tamaño que en ClientesScreen: badge, chip de costo y botones de acción
private val ChipWidth = 96.dp
private val ChipHeight = 36.dp
private val ActionWidth = 44.dp // dos botones (44 + 8 + 44) = 96.dp

@Composable
fun SolicitudesScreen(onNavigate: (String) -> Unit = {}) {
    val requests = GymApp.repository.requests()
    val resolved = remember { mutableStateMapOf<Int, String>() }
    val pendientes = requests.count { resolved[it.id] == null }

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

            items(requests, key = { it.id }) { req ->
                SolicitudCard(
                    clientName = req.clientName,
                    membership = req.membershipNumber.toString(),
                    instructor = req.instructorName,
                    cost = req.cost.toString(),
                    date = req.requestDate.toString(),
                    status = resolved[req.id] ?: "PENDIENTE",
                    onApprove = { resolved[req.id] = "APROBADA" },
                    onReject = { resolved[req.id] = "RECHAZADA" }
                )
            }

            if (requests.isEmpty()) {
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