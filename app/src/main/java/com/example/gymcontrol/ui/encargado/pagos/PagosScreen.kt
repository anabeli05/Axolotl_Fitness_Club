package com.example.gymcontrol.ui.encargado.pagos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.data.biometria.FotoPerfil
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.data.remote.ComprobantesRepository
import com.example.gymcontrol.data.remote.PagoPendienteRow
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.components.dinero
import com.example.gymcontrol.ui.components.metodoPagoLocalizado
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.gymcontrol.R
import kotlinx.coroutines.launch

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Pagos", "Gastos", "Reportes")

private class Resolucion(val pago: PagoPendienteRow, val aceptar: Boolean)

/**
 * Pagos pendientes de validar: los comprobantes que suben los clientes y las
 * transferencias que registra Recepción. Al aceptar, el pago entra en los
 * ingresos (Dashboard y Reportes) y se activa la membresía del cliente.
 */
@Composable
fun PagosScreen(onNavigate: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val correo = SesionActual.correo.orEmpty()

    var pagos by remember { mutableStateOf<List<PagoPendienteRow>>(emptyList()) }
    // Comprobantes ya descargados (id del pago -> imagen; null = no tiene o no cargó)
    val imagenes = remember { mutableStateMapOf<Long, ImageBitmap?>() }
    var cargando by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf<Resolucion?>(null) }
    var procesando by remember { mutableStateOf(false) }
    var motivo by remember { mutableStateOf("") }
    var imagenAmpliada by remember { mutableStateOf<ImageBitmap?>(null) }

    val ctx = LocalContext.current

    suspend fun recargar() {
        try {
            pagos = ClientesRepository.pagosPendientes()
            error = null
        } catch (e: Exception) {
            error = ctx.applicationContext.getString(R.string.visits_load_error, e.message?.substringBefore("\nCode:").orEmpty())
        }
        cargando = false
        pagos.filter { it.tieneComprobante && it.id !in imagenes }.forEach { p ->
            imagenes[p.id] = try {
                FotoPerfil.aImagen(ComprobantesRepository.imagen(correo, p.id))
            } catch (_: Exception) {
                null
            }
        }
    }

    LaunchedEffect(Unit) { recargar() }

    GymScaffold(
        currentSection = "Pagos",
        sections = ADMIN_SECTIONS,
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
                    text = stringResource(R.string.tab_payments),
                    color = ClienteTema.Texto,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.manager_pending_payments, pagos.size),
                    color = ClienteTema.TextoSec,
                    fontSize = 15.sp
                )
            }

            mensaje?.let { item { Text(it, color = ClienteTema.Verde, fontSize = 14.sp) } }
            error?.let { item { Text(it, color = ClienteTema.Rojo, fontSize = 14.sp) } }

            items(pagos, key = { it.id }) { pago ->
                PagoCard(
                    pago = pago,
                    imagen = imagenes[pago.id],
                    cargandoImagen = pago.tieneComprobante && pago.id !in imagenes,
                    habilitado = !procesando,
                    onAmpliar = { imagenAmpliada = it },
                    onAceptar = { confirmar = Resolucion(pago, true) },
                    onRechazar = { motivo = ""; confirmar = Resolucion(pago, false) }
                )
            }

            if (pagos.isEmpty() && !cargando && error == null) {
                item {
                    Text(
                        text = stringResource(R.string.no_pending_payments),
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
        val ctx = LocalContext.current
        val strMsgRejected = stringResource(R.string.manager_payment_rejected_msg, r.pago.cliente)

        AlertDialog(
            onDismissRequest = { if (!procesando) confirmar = null },
            containerColor = ClienteTema.Beige,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    if (r.aceptar) stringResource(R.string.manager_accept_payment) else stringResource(R.string.reject_payment_title),
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
                            stringResource(R.string.manager_accept_confirmation, montoTexto(r.pago.monto), r.pago.cliente)
                        else
                            stringResource(R.string.manager_reject_confirmation, r.pago.cliente),
                        color = ClienteTema.TextoSec,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!r.aceptar) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = motivo,
                            onValueChange = { if (it.length <= 200) motivo = it },
                            label = { Text(stringResource(R.string.reject_reason_label)) },
                            placeholder = { Text(stringResource(R.string.manager_rejection_reason_placeholder)) },
                            enabled = !procesando,
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
                    enabled = !procesando && (r.aceptar || motivo.trim().length >= 3),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (r.aceptar) ClienteTema.Verde else ClienteTema.Rojo
                    ),
                    onClick = {
                        scope.launch {
                            procesando = true
                            try {
                                mensaje = if (r.aceptar) {
                                    val vence = ClientesRepository.confirmarPago(correo, r.pago.id, true)
                                    ctx.resources.getString(R.string.manager_payment_validated_msg, r.pago.cliente, ClientesRepository.fechaApp(vence).orEmpty())
                                } else {
                                    ComprobantesRepository.rechazar(correo, r.pago.id, motivo)
                                    strMsgRejected
                                }
                                error = null
                                imagenes.remove(r.pago.id)
                                recargar()
                            } catch (e: Exception) {
                                error = ctx.getString(R.string.visits_save_error, e.message?.substringBefore("\nCode:").orEmpty())
                            } finally {
                                procesando = false
                                confirmar = null
                            }
                        }
                    }
                ) { Text(if (r.aceptar) stringResource(R.string.btn_accept) else stringResource(R.string.btn_reject)) }
            },
            dismissButton = {
                TextButton(enabled = !procesando, onClick = { confirmar = null }) {
                    Text(stringResource(R.string.cancel), color = ClienteTema.TextoSec)
                }
            }
        )
    }

    // Comprobante a pantalla completa
    imagenAmpliada?.let { img ->
        Dialog(onDismissRequest = { imagenAmpliada = null }) {
            Image(
                bitmap = img,
                contentDescription = stringResource(R.string.receipt_selected),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .clickable { imagenAmpliada = null }
            )
        }
    }
}

private fun montoTexto(m: Double) = dinero(m)

/** Recuadro con: cliente, plan, monto, método, el comprobante y los botones. */
@Composable
private fun PagoCard(
    pago: PagoPendienteRow,
    imagen: ImageBitmap?,
    cargandoImagen: Boolean,
    habilitado: Boolean,
    onAmpliar: (ImageBitmap) -> Unit,
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
                text = "${stringResource(R.string.label_plan_prefix, pago.plan ?: "—")} · ${montoTexto(pago.monto)}",
                color = ClienteTema.AzulSel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            val metodoTraducido = metodoPagoLocalizado(pago.metodo)
            Text(
                text = stringResource(R.string.payment_method_sent_on, metodoTraducido, ClientesRepository.fechaApp(pago.fecha).orEmpty()),
                color = ClienteTema.TextoSec,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 260.dp)
                    .background(ClienteTema.Fondo, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    imagen != null -> Image(
                        bitmap = imagen,
                        contentDescription = stringResource(R.string.receipt_selected),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onAmpliar(imagen) }
                    )
                    cargandoImagen -> CircularProgressIndicator(color = ClienteTema.AzulSel)
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = ClienteTema.TextoSec,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            if (pago.tieneComprobante) stringResource(R.string.receipt_load_error)
                            else stringResource(R.string.receipt_no_receipt),
                            color = ClienteTema.TextoSec,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            if (imagen != null) {
                Text(
                    stringResource(R.string.tap_image_to_enlarge),
                    color = ClienteTema.TextoSec,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRechazar,
                    enabled = habilitado,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.5.dp, ClienteTema.Rojo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = ClienteTema.Rojo)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_reject), color = ClienteTema.Rojo, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAceptar,
                    enabled = habilitado,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Verde),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_accept), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
