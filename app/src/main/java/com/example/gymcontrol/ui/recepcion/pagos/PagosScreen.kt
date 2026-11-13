package com.example.gymcontrol.ui.recepcion.pagos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.dinero

/**
 * Modelo SOLO de vista para la pantalla de Pagos de Recepción.
 * TODO: reemplazar por PagoPendienteRow cuando se conecte la base de datos.
 */
private data class PagoUi(
    val id: Long,
    val cliente: String,
    val numeroMembresia: String,
    val plan: String,
    val monto: Double,
    val metodo: String,
    val fecha: String,
    val tieneComprobante: Boolean
)

private class Resolucion(val pago: PagoUi, val aceptar: Boolean)

// Datos de ejemplo para poder ver el diseño. TODO: quitar al conectar la base de datos.
private val PAGOS_EJEMPLO = listOf(
    PagoUi(1, "Juan Pérez", "MEM-0001", "Mensual", 450.0, "Transferencia", "12/11/2026", true),
    PagoUi(2, "María López", "MEM-0002", "Trimestral", 1200.0, "Transferencia", "11/11/2026", false)
)

/**
 * Pagos pendientes de validar (vista de Recepción, misma que la del Encargado).
 * Por ahora solo UI: aceptar/rechazar no guarda nada.
 */
@Composable
fun RecepcionPagosScreen(onNavigate: (String) -> Unit = {}) {
    // TODO: cargar desde la base de datos (ClientesRepository.pagosPendientes())
    val pagos = remember { mutableStateListOf<PagoUi>().apply { addAll(PAGOS_EJEMPLO) } }
    var confirmar by remember { mutableStateOf<Resolucion?>(null) }
    var motivo by remember { mutableStateOf("") }

    GymScaffold(
        currentSection = "Pagos",
        onNavigate = onNavigate
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Pagos de Clientes",
                    color = ClienteTema.Texto,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pendientes de validar: ${pagos.size}",
                    color = ClienteTema.TextoSec,
                    fontSize = 15.sp
                )
            }

            items(pagos, key = { it.id }) { pago ->
                PagoCard(
                    pago = pago,
                    onAceptar = { confirmar = Resolucion(pago, true) },
                    onRechazar = { motivo = ""; confirmar = Resolucion(pago, false) }
                )
            }

            if (pagos.isEmpty()) {
                item {
                    Text(
                        text = "No hay pagos pendientes por validar.",
                        color = ClienteTema.TextoSec,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    )
                }
            }
        }
    }

    confirmar?.let { r ->
        AlertDialog(
            onDismissRequest = { confirmar = null },
            containerColor = ClienteTema.Beige,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    if (r.aceptar) "Aceptar pago" else "Rechazar pago",
                    color = ClienteTema.Texto,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column {
                    Text(
                        if (r.aceptar)
                            "¿Validar el pago de ${dinero(r.pago.monto)} de ${r.pago.cliente}? Se sumará a los ingresos de hoy y se activará su membresía."
                        else
                            "¿Rechazar el pago de ${r.pago.cliente}? No se sumará a los ingresos y el cliente verá el motivo en su pestaña Pagos.",
                        color = ClienteTema.TextoSec,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!r.aceptar) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = motivo,
                            onValueChange = { if (it.length <= 200) motivo = it },
                            label = { Text("Motivo del rechazo") },
                            placeholder = { Text("Ej. El monto no coincide con el plan") },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ClienteTema.Texto,
                                unfocusedTextColor = ClienteTema.Texto,
                                focusedBorderColor = ClienteTema.Rojo,
                                unfocusedBorderColor = ClienteTema.Borde,
                                focusedLabelColor = ClienteTema.Rojo,
                                unfocusedLabelColor = ClienteTema.TextoSec,
                                cursorColor = ClienteTema.Rojo
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = r.aceptar || motivo.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (r.aceptar) ClienteTema.Verde else ClienteTema.Rojo
                    ),
                    onClick = {
                        // TODO: conectar a la base de datos
                        //  aceptar  -> ClientesRepository.confirmarPago(correo, r.pago.id, true)
                        //  rechazar -> ComprobantesRepository.rechazar(correo, r.pago.id, motivo)
                        pagos.remove(r.pago)
                        confirmar = null
                    }
                ) { Text(if (r.aceptar) "Aceptar" else "Rechazar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = null }) {
                    Text("Cancelar", color = ClienteTema.TextoSec)
                }
            }
        )
    }
}

/** Recuadro con: cliente, plan, monto, método, el comprobante y los botones. */
@Composable
private fun PagoCard(
    pago: PagoUi,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
        border = BorderStroke(1.5.dp, ClienteTema.Borde)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = pago.cliente,
                color = ClienteTema.Texto,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (pago.numeroMembresia.isNotBlank()) {
                Text(pago.numeroMembresia, color = ClienteTema.TextoSec, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Plan: ${pago.plan} · ${dinero(pago.monto)}",
                color = ClienteTema.AzulSel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${pago.metodo} · enviado el ${pago.fecha}",
                color = ClienteTema.TextoSec,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(12.dp))

            // Espacio del comprobante. TODO: mostrar la imagen real al conectar la base de datos.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 260.dp)
                    .background(ClienteTema.Fondo, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = ClienteTema.TextoSec,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        if (pago.tieneComprobante) "Comprobante de pago"
                        else "Sin comprobante (registrado en Recepción)",
                        color = ClienteTema.TextoSec,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRechazar,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.5.dp, ClienteTema.Rojo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = ClienteTema.Rojo)
                    Spacer(Modifier.width(6.dp))
                    Text("Rechazar", color = ClienteTema.Rojo, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAceptar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Verde),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Aceptar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
