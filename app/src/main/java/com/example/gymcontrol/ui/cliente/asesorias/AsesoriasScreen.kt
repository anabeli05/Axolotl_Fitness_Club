package com.example.gymcontrol.ui.cliente.asesorias

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.AsesoriasRepository
import com.example.gymcontrol.data.remote.InstructorRow
import com.example.gymcontrol.ui.components.ClienteScaffoldClaro
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.SesionActual
import kotlinx.coroutines.launch

private fun formatPrice(cost: Double): String =
    if (cost % 1.0 == 0.0) "$${"%.0f".format(cost)}" else "$${"%.2f".format(cost)}"

private class CancelTarget(val instructorId: Long, val instructorName: String)

@Composable
fun AsesoriasScreen(onNavigate: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var instructors by remember { mutableStateOf<List<InstructorRow>>(emptyList()) }
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

    ClienteScaffoldClaro(
        currentSection = "Asesorías",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_final
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        stringResource(R.string.cliente_asesorias_title),
                        color = ClienteTema.Texto,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.cliente_asesorias_choose_instructor),
                        color = ClienteTema.Texto,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            mensaje?.let { msg ->
                item { Text(msg, color = ClienteTema.Rojo, fontSize = 14.sp) }
            }

            if (instructors.isEmpty() && mensaje == null) {
                item {
                    Text(
                        stringResource(R.string.cliente_asesorias_no_instructors),
                        color = ClienteTema.TextoSec,
                        fontSize = 14.sp
                    )
                }
            }

            items(instructors) { instructor ->
                val estado = estados[instructor.id]
                val isPending = estado == "PENDIENTE"
                val isApproved = estado == "APROBADA"
                val sinCupo = instructor.activos >= instructor.maxClientes
                val forma = com.example.gymcontrol.ui.components.GymCardShape

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ClienteTema.Beige, forma)
                        .border(1.dp, ClienteTema.Borde, forma)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFB9B5A6), CircleShape)
                            .border(1.dp, ClienteTema.Borde, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = ClienteTema.Borde,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            instructor.nombre,
                            color = ClienteTema.Texto,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.cliente_asesorias_quota, instructor.activos, instructor.maxClientes),
                            color = ClienteTema.TextoSec,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            stringResource(R.string.cliente_asesorias_per_month, formatPrice(instructor.costo)),
                            color = ClienteTema.Texto,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when {
                            isApproved -> EstadoChip(stringResource(R.string.cliente_asesorias_status_approved), ClienteTema.Verde, Color.White)
                            isPending -> {
                                EstadoChip(stringResource(R.string.cliente_asesorias_status_pending), ClienteTema.AzulClaro, ClienteTema.Texto)
                                Text(
                                    stringResource(R.string.cliente_asesorias_cancel),
                                    color = ClienteTema.Rojo,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickableSinRipple {
                                            cancelling = CancelTarget(instructor.id, instructor.nombre)
                                        }
                                        .padding(2.dp)
                                )
                            }
                            else -> Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            AsesoriasRepository.solicitar(correo, instructor.id)
                                            mensaje = null
                                            recargar()
                                        } catch (e: Exception) {
                                            mensaje = e.message?.substringBefore("\nCode:")
                                                ?: "No se pudo solicitar"
                                        }
                                    }
                                },
                                enabled = !sinCupo,
                                modifier = Modifier.defaultMinSize(minHeight = com.example.gymcontrol.ui.components.GymButtonMinHeight),
                                shape = com.example.gymcontrol.ui.components.GymButtonShape,
                                contentPadding = com.example.gymcontrol.ui.components.GymButtonPadding,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ClienteTema.Azul,
                                    contentColor = Color.White,
                                    disabledContainerColor = ClienteTema.Azul.copy(alpha = 0.4f),
                                    disabledContentColor = Color.White
                                )
                            ) {
                                com.example.gymcontrol.ui.components.GymButtonText(
                                    if (sinCupo) stringResource(R.string.cliente_asesorias_no_quota) else stringResource(R.string.cliente_asesorias_request)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    cancelling?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelling = null },
            modifier = Modifier.border(1.dp, ClienteTema.Borde, com.example.gymcontrol.ui.components.GymCardShape),
            containerColor = ClienteTema.Beige,
            shape = com.example.gymcontrol.ui.components.GymCardShape,
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = ClienteTema.Rojo) },
            title = {
                Text(
                    stringResource(R.string.cliente_asesorias_cancel_dialog_title),
                    color = ClienteTema.Texto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    stringResource(R.string.cliente_asesorias_cancel_dialog_desc, target.instructorName),
                    color = ClienteTema.TextoSec,
                    style = MaterialTheme.typography.bodyMedium,
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
                        onClick = { cancelling = null },
                        shape = com.example.gymcontrol.ui.components.GymButtonShape,
                        border = BorderStroke(1.dp, ClienteTema.Borde),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto),
                        modifier = Modifier.defaultMinSize(minHeight = com.example.gymcontrol.ui.components.GymButtonMinHeight),
                        contentPadding = com.example.gymcontrol.ui.components.GymButtonPadding
                    ) { com.example.gymcontrol.ui.components.GymButtonText(stringResource(R.string.common_back)) }

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
                        shape = com.example.gymcontrol.ui.components.GymButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClienteTema.Rojo,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = com.example.gymcontrol.ui.components.GymButtonMinHeight),
                        contentPadding = com.example.gymcontrol.ui.components.GymButtonPadding
                    ) { com.example.gymcontrol.ui.components.GymButtonText(stringResource(R.string.cliente_asesorias_cancel)) }
                }
            }
        )
    }
}

@Composable
private fun EstadoChip(etiqueta: String, fondo: Color, texto: Color) {
    Box(
        modifier = Modifier
            .background(fondo, com.example.gymcontrol.ui.components.GymButtonShape)
            .border(1.dp, ClienteTema.Borde, com.example.gymcontrol.ui.components.GymButtonShape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            etiqueta,
            color = texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun Modifier.clickableSinRipple(onClick: () -> Unit): Modifier =
    this.clickable(
        indication = null,
        interactionSource = MutableInteractionSource(),
        onClick = onClick
    )