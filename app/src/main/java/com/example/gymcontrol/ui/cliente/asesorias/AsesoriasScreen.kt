package com.example.gymcontrol.ui.cliente.asesorias

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.AsesoriasRepository
import com.example.gymcontrol.data.remote.InstructorRow
import com.example.gymcontrol.ui.components.SesionActual
import kotlinx.coroutines.launch
import com.example.gymcontrol.ui.components.ClienteScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val CardBorder = Brush.linearGradient(listOf(GymColors.Purple, GymColors.Gold))

// Borde degradado en tonos rojos para el cuadro de confirmación
private val DeleteBorder = Brush.linearGradient(
    listOf(Color(0xFFFF6B6B), GymColors.Red, Color(0xFF7A0A0A))
)

private val ButtonHeight = 44.dp

private fun formatPrice(cost: Double): String =
    if (cost % 1.0 == 0.0) "$${"%.0f".format(cost)}" else "$${"%.2f".format(cost)}"

// "Marco Díaz" -> "MD"
private fun initials(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

// Solicitud que se quiere cancelar
private class CancelTarget(val instructorId: Long, val instructorName: String)

@Composable
fun AsesoriasScreen(onNavigate: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var instructors by remember { mutableStateOf<List<InstructorRow>>(emptyList()) }
    // instructorId -> "PENDIENTE" | "APROBADA"
    var estados by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var cancelling by remember { mutableStateOf<CancelTarget?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    val correo = SesionActual.correo ?: ""

    suspend fun recargar() {
        try {
            instructors = AsesoriasRepository.instructores()
            estados = AsesoriasRepository.misSolicitudes(correo).associate { it.instructorId to it.estado }
        } catch (e: Exception) {
            mensaje = "No se pudo cargar: ${e.message?.substringBefore("\nCode:")}"
        }
    }

    LaunchedEffect(Unit) { recargar() }

    ClienteScaffold(
        currentSection = "Asesorías",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_axolotl
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Asesorías",
                        color = GymColors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Elige tu instructor",
                        color = GymColors.TextSecondary,
                        fontSize = 16.sp
                    )
                }
            }

            mensaje?.let { msg ->
                item { Text(msg, color = GymColors.Red, fontSize = 14.sp) }
            }

            if (instructors.isEmpty() && mensaje == null) {
                item {
                    Text(
                        "Aún no hay instructores disponibles",
                        color = GymColors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            items(instructors) { instructor ->
                val estado = estados[instructor.id]
                val isPending = estado == "PENDIENTE"
                val isApproved = estado == "APROBADA"
                val sinCupo = instructor.activos >= instructor.maxClientes

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GymColors.Surface, RoundedCornerShape(14.dp))
                        .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(20.dp)
                ) {
                    // Avatar + datos del instructor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InstructorAvatar(name = instructor.nombre)

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = instructor.nombre,
                                color = GymColors.TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Cupo: ${instructor.activos}/${instructor.maxClientes}",
                                color = GymColors.Purple,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${formatPrice(instructor.costo)} /mes",
                                color = GymColors.Gold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Botones
                    if (isApproved) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ButtonHeight)
                                .background(GymColors.Green.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, GymColors.Green, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Asesoría aprobada",
                                color = GymColors.Green,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isPending) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Estado pendiente
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(ButtonHeight)
                                    .background(GymColors.Gold.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .border(1.dp, GymColors.Gold, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Pendiente",
                                    color = GymColors.Gold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Pide confirmación antes de cancelar la solicitud
                            OutlinedButton(
                                onClick = {
                                    cancelling = CancelTarget(instructor.id, instructor.nombre)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(ButtonHeight),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, GymColors.Red),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = GymColors.Red.copy(alpha = 0.1f),
                                    contentColor = GymColors.Red
                                )
                            ) {
                                Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        AsesoriasRepository.solicitar(correo, instructor.id)
                                        mensaje = null
                                        recargar()
                                    } catch (e: Exception) {
                                        mensaje = e.message?.substringBefore("\nCode:") ?: "No se pudo solicitar"
                                    }
                                }
                            },
                            enabled = !sinCupo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ButtonHeight),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GymColors.Purple,
                                contentColor = GymColors.TextPrimary
                            )
                        ) {
                            Text(if (sinCupo) "Sin cupo" else "Solicitar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Cuadro de confirmación para cancelar la solicitud
    cancelling?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelling = null },
            modifier = Modifier.border(1.5.dp, DeleteBorder, RoundedCornerShape(16.dp)),
            containerColor = GymColors.Surface,
            shape = RoundedCornerShape(16.dp),
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = GymColors.Red) },
            title = {
                Text(
                    "¿Cancelar solicitud?",
                    color = GymColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "Se cancelará tu solicitud de asesoría con ${target.instructorName}.",
                    color = GymColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        cancelling = null
                        scope.launch {
                            try {
                                AsesoriasRepository.cancelar(correo, target.instructorId)
                                mensaje = null
                                recargar()
                            } catch (e: Exception) {
                                mensaje = e.message?.substringBefore("\nCode:") ?: "No se pudo cancelar"
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymColors.Red,
                        contentColor = Color.White
                    )
                ) { Text("Sí, cancelar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { cancelling = null },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GymColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.TextPrimary)
                ) { Text("Volver") }
            }
        )
    }
}

// Círculo con las iniciales del instructor (más adelante se puede reemplazar por su foto)
@Composable
private fun InstructorAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .background(GymColors.Purple.copy(alpha = 0.18f), CircleShape)
            .border(2.dp, CardBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials(name),
            color = GymColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}