package com.example.gymcontrol.ui.cliente.Pag

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.data.remote.ComprobantesRepository
import com.example.gymcontrol.data.remote.MiComprobanteRow
import com.example.gymcontrol.ui.components.ClienteScaffoldClaro
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.components.dinero
import android.graphics.BitmapFactory
import kotlinx.coroutines.launch

/**
 * Un plan que el cliente puede pagar. Sale de la pestaña "Servicios" del Encargado
 * (vista planes_pago), así que nombre, duración y precio siempre son los actuales.
 */
data class PlanPago(val nombre: String, val duracionDias: Int, val precio: Double) {
    val icono: ImageVector
        get() = when {
            duracionDias < 30 -> Icons.Filled.FitnessCenter
            duracionDias < 90 -> Icons.Filled.CalendarMonth
            else -> Icons.Filled.DateRange
        }
}

/** Ej: "$500.00 MXN (Mensualidad)" */
private fun montoTexto(plan: PlanPago) = "${dinero(plan.precio)} MXN (${plan.nombre})"

/**
 * El cliente elige plan y método, sube su comprobante y este queda en revisión
 * hasta que el Encargado lo acepta en su pestaña "Pagos".
 */
@Composable
fun PagHomeScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val correo = SesionActual.correo.orEmpty()
    val nombreUsuario = SesionActual.nombre.orEmpty()

    var selectedPlan by remember { mutableStateOf<PlanPago?>(null) }
    var selectedMethod by remember { mutableStateOf<PaymentMethodInfo?>(null) }
    var numeroMembresia by remember { mutableStateOf("") }
    var planes by remember { mutableStateOf<List<PlanPago>>(emptyList()) }
    var cargandoPlanes by remember { mutableStateOf(true) }
    var ultimo by remember { mutableStateOf<MiComprobanteRow?>(null) }
    var enviando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val enRevision = ultimo?.estado == "PENDIENTE"

    suspend fun cargarUltimo() {
        if (correo.isBlank()) return
        try { ultimo = ComprobantesRepository.miUltimo(correo) } catch (_: Exception) {}
    }

    LaunchedEffect(correo) {
        try {
            planes = ComprobantesRepository.planesPago().map { PlanPago(it.nombre, it.duracionDias, it.precio) }
        } catch (e: Exception) {
            error = context.resources.getString(R.string.error_loading_plans, e.message?.substringBefore("\nCode:") ?: "")
        }
        cargandoPlanes = false
        if (correo.isNotBlank()) {
            try { numeroMembresia = ClientesRepository.porCorreo(correo)?.numeroMembresia.orEmpty() } catch (_: Exception) {}
        }
        cargarUltimo()
    }

    // Con un plan elegido, "atrás" regresa a la selección de plan
    BackHandler(enabled = selectedPlan != null) { selectedPlan = null }

    val transTitle = stringResource(R.string.cliente_pago_transferencia_dialog_title)
    val transInstructions = stringResource(R.string.cliente_pago_transferencia_instructions)
    val labelBanco = stringResource(R.string.cliente_pago_label_banco)
    val bankTitle = stringResource(R.string.bank_title)
    val labelClabe = stringResource(R.string.cliente_pago_label_clabe)
    val labelCuenta = stringResource(R.string.cliente_pago_label_cuenta)
    val labelBeneficiario = stringResource(R.string.cliente_pago_label_beneficiario)
    val valBeneficiarioGym = stringResource(R.string.cliente_pago_val_beneficiario_gym)

    val depTitle = stringResource(R.string.cliente_pago_deposito_title)
    val depInstructions = stringResource(R.string.cliente_pago_deposito_instructions)
    val labelNoTarjeta = stringResource(R.string.cliente_pago_label_no_tarjeta)
    val labelCorresponsales = stringResource(R.string.cliente_pago_label_corresponsales)
    val corresponsalesFormatted = stringResource(R.string.corresponsales_formatted)
    val labelSucursal = stringResource(R.string.cliente_pago_label_sucursal)

    val efecTitle = stringResource(R.string.cliente_pago_efectivo_title)
    val efecInstructions = stringResource(R.string.cliente_pago_efectivo_instructions)
    val labelUbicacion = stringResource(R.string.cliente_pago_label_ubicacion)
    val labelHorario = stringResource(R.string.cliente_pago_label_horario)
    val labelAtencion = stringResource(R.string.cliente_pago_label_atencion)
    val valHorario = stringResource(R.string.cliente_pago_val_horario)
    val valCaja = stringResource(R.string.cliente_pago_val_caja)
    val valRecepcion = stringResource(R.string.cliente_pago_val_recepcion)

    ClienteScaffoldClaro(
        currentSection = "Pagos",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_final
    ) {
        val plan = selectedPlan

        if (plan == null) {
            // ---------- PASO 1: selección de plan ----------
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.client_select_plan),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClienteTema.Texto,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
                item {
                    EstadoComprobante(ultimo = ultimo, mensaje = mensaje)
                }
                items(planes, key = { it.nombre }) { p ->
                    PlanCard(
                        plan = p,
                        habilitado = !enRevision,
                        onClick = { mensaje = null; error = null; selectedPlan = p }
                    )
                }
                if (planes.isEmpty()) {
                    item {
                        val err = error
                        Text(
                            text = when {
                                cargandoPlanes -> stringResource(R.string.loading_plans)
                                err != null -> err
                                else -> stringResource(R.string.no_plans_available)
                            },
                            color = if (err != null) ClienteTema.Rojo else ClienteTema.TextoSec,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        )
                    }
                }
            }
        } else {
            // ---------- PASO 2: métodos de pago ----------
            val concepto = buildConcepto(plan, nombreUsuario, numeroMembresia)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.cliente_pagos_title),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClienteTema.Texto,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Resumen del plan elegido + opción para cambiarlo
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ClienteTema.Beige, RoundedCornerShape(12.dp))
                            .border(1.dp, ClienteTema.Borde, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.plan_selected_format, plan.nombre),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClienteTema.Texto
                            )
                            Text(
                                text = stringResource(R.string.total_to_pay_format, montoTexto(plan)),
                                fontSize = 13.sp,
                                color = ClienteTema.TextoSec
                            )
                        }
                        TextButton(onClick = { selectedPlan = null }) {
                            Text(stringResource(R.string.btn_change), color = ClienteTema.AzulSel, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 1. Pago por Transferencia
                item {
                    MetodoPagoCard(
                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                        title = stringResource(R.string.cliente_pago_transferencia_title),
                        description = null,
                        subtitle = stringResource(R.string.cliente_pago_transferencia_sub),
                        onClick = {
                            selectedMethod = PaymentMethodInfo(
                                metodo = "Transferencia",
                                title = transTitle,
                                details = listOf(
                                    labelBanco to bankTitle,
                                    labelClabe to "012 095 01569158521 0",
                                    labelCuenta to "1234567890",
                                    labelBeneficiario to valBeneficiarioGym
                                ),
                                instructions = transInstructions
                            )
                        }
                    )
                }

                // 2. Pago por Depósito
                item {
                    MetodoPagoCard(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = stringResource(R.string.cliente_pago_deposito_title),
                        description = stringResource(R.string.cliente_pago_deposito_sub),
                        subtitle = stringResource(R.string.cliente_pago_deposito_sub),
                        onClick = {
                            selectedMethod = PaymentMethodInfo(
                                metodo = "Depósito",
                                title = depTitle,
                                details = listOf(
                                    labelNoTarjeta to "4152 3138 9012 3456",
                                    labelCorresponsales to corresponsalesFormatted,
                                    labelSucursal to "BBVA Bancomer",
                                    labelBeneficiario to "Gimnasio Axolotl"
                                ),
                                instructions = depInstructions
                            )
                        }
                    )
                }

                // 3. Pago en Efectivo
                item {
                    MetodoPagoCard(
                        icon = Icons.Filled.Payments,
                        title = stringResource(R.string.cliente_pago_efectivo_title),
                        description = stringResource(R.string.cliente_pago_efectivo_desc),
                        subtitle = stringResource(R.string.cliente_pago_efectivo_sub),
                        onClick = {
                            selectedMethod = PaymentMethodInfo(
                                metodo = "Efectivo",
                                title = efecTitle,
                                details = listOf(
                                    labelUbicacion to valRecepcion,
                                    labelHorario to valHorario,
                                    labelAtencion to valCaja
                                ),
                                instructions = efecInstructions
                            )
                        }
                    )
                }
            }

            selectedMethod?.let { info ->
                PaymentDetailDialog(
                    info = info,
                    montoTexto = montoTexto(plan),
                    concepto = concepto,
                    enviando = enviando,
                    error = error,
                    onDismiss = { if (!enviando) { selectedMethod = null; error = null } },
                    onSubirComprobante = { uri ->
                        scope.launch {
                            enviando = true
                            error = null
                            try {
                                val imagen = ComprobantesRepository.imagenABase64(context, uri)
                                ComprobantesRepository.enviar(correo, plan.nombre, info.metodo, imagen)
                                selectedMethod = null
                                selectedPlan = null
                                mensaje = context.resources.getString(R.string.receipt_sent_message)
                                cargarUltimo()
                            } catch (e: Exception) {
                                error = "No se pudo enviar: ${e.message?.substringBefore("\nCode:")?.substringAfterLast("ERROR: ")}"
                            } finally {
                                enviando = false
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun buildConcepto(plan: PlanPago, nombre: String, membresia: String): String {
    val partes = mutableListOf("Pago plan ${plan.nombre} (${dinero(plan.precio)})")
    if (nombre.isNotBlank()) partes += nombre
    if (membresia.isNotBlank()) partes += "Membresía $membresia"
    return partes.joinToString(" - ")
}

@Composable
private fun PlanCard(
    plan: PlanPago,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (habilitado) 1f else 0.45f)
            .clickable(enabled = habilitado) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
        border = BorderStroke(1.5.dp, ClienteTema.Borde)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = plan.icono,
                contentDescription = null,
                tint = ClienteTema.AzulSel,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.nombre,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClienteTema.Texto
                )
                Spacer(modifier = Modifier.height(4.dp))
                val descripcion = when {
                    plan.duracionDias == 1 -> stringResource(R.string.plan_access_1_day)
                    plan.duracionDias >= 30 && plan.duracionDias % 30 == 0 ->
                        (plan.duracionDias / 30).let { if (it == 1) stringResource(R.string.plan_access_1_month) else stringResource(R.string.plan_access_months, it) }
                    else -> stringResource(R.string.plan_access_days, plan.duracionDias)
                }
                Text(
                    text = descripcion,
                    fontSize = 13.sp,
                    color = ClienteTema.TextoSec
                )
            }
            Text(
                text = dinero(plan.precio),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ClienteTema.AzulSel
            )
        }
    }
}

@Composable
private fun MetodoPagoCard(
    icon: ImageVector,
    title: String,
    description: String?,
    subtitle: String,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = forma,
        colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
        border = BorderStroke(1.5.dp, ClienteTema.Borde)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ClienteTema.AzulSel,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClienteTema.Texto,
                        lineHeight = 24.sp
                    )
                    if (description != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            fontSize = 13.sp,
                            color = ClienteTema.TextoSec,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = ClienteTema.Borde.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = ClienteTema.TextoSec
            )
        }
    }
}

/** Aviso arriba de los planes: comprobante en revisión, validado o rechazado. */
@Composable
private fun EstadoComprobante(ultimo: MiComprobanteRow?, mensaje: String?) {
    if (ultimo?.estado == "RECHAZADO" && mensaje == null) {
        // Rechazado: recuadro rojo con el motivo que escribió el Encargado
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.RojoPastel, RoundedCornerShape(12.dp))
                .border(1.5.dp, ClienteTema.Rojo, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text(
                text = stringResource(R.string.payment_rejected_title, ultimo.plan.orEmpty()),
                color = ClienteTema.Rojo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.payment_rejected_reason, ultimo.motivo?.takeIf { it.isNotBlank() } ?: "sin especificar"),
                color = ClienteTema.Rojo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.payment_rejected_instruction),
                color = ClienteTema.Rojo,
                fontSize = 13.sp
            )
        }
        return
    }

    val (texto, color) = when {
        ultimo?.estado == "PENDIENTE" ->
            stringResource(R.string.receipt_under_review, ultimo.plan.orEmpty()) to ClienteTema.AzulSel
        mensaje != null -> mensaje to ClienteTema.Verde
        ultimo?.estado == "ACEPTADO" ->
            stringResource(R.string.payment_validated_on, ultimo.plan.orEmpty(), ClientesRepository.fechaApp(ultimo.resuelto).orEmpty()) to ClienteTema.Verde
        else -> return
    }
    Text(
        text = texto,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(ClienteTema.Beige, RoundedCornerShape(12.dp))
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .padding(12.dp)
    )
}

private data class PaymentMethodInfo(
    /** Como se guarda en pagos.metodo_pago */
    val metodo: String,
    val title: String,
    val details: List<Pair<String, String>>,
    val instructions: String
)

@Composable
private fun PaymentDetailDialog(
    info: PaymentMethodInfo,
    montoTexto: String,
    concepto: String,
    enviando: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubirComprobante: (Uri) -> Unit
) {
    val context = LocalContext.current
    var comprobanteUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) comprobanteUri = uri
    }

    // Vista previa del comprobante seleccionado
    val preview = remember(comprobanteUri) {
        comprobanteUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .background(ClienteTema.Beige, RoundedCornerShape(20.dp))
                .border(1.5.dp, ClienteTema.Borde, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Título a la izquierda, X a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = info.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClienteTema.Texto,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = ClienteTema.Texto
                    )
                }
            }

            HorizontalDivider(
                color = ClienteTema.Borde.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            info.details.forEach { (label, value) ->
                DetailRow(label, value)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recuadro: Monto a pagar + Concepto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        ClienteTema.Navy.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, ClienteTema.Borde, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_amount_to_pay),
                    fontSize = 14.sp,
                    color = ClienteTema.Texto
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = montoTexto,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClienteTema.Azul
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.label_concept),
                    fontSize = 14.sp,
                    color = ClienteTema.Texto
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = concepto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClienteTema.Texto,
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Instrucciones
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        ClienteTema.Navy.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, ClienteTema.Borde.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = info.instructions,
                    fontSize = 14.sp,
                    color = ClienteTema.Texto,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = stringResource(R.string.receipt_selected),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ClienteTema.Borde, RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Botón dorado: Subir comprobante
            if (error != null) {
                Text(error, color = ClienteTema.Rojo, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }

            Button(
                onClick = { picker.launch("image/*") },
                enabled = !enviando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClienteTema.Azul,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    if (comprobanteUri == null) stringResource(R.string.btn_upload_receipt) else stringResource(R.string.btn_change_receipt),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Confirmar envío (solo cuando ya hay imagen)
            if (comprobanteUri != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { comprobanteUri?.let(onSubirComprobante) },
                    enabled = !enviando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, ClienteTema.Azul)
                ) {
                    if (enviando) {
                        CircularProgressIndicator(
                            color = ClienteTema.Azul,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            stringResource(R.string.btn_send_receipt),
                            fontSize = 16.sp,
                            color = ClienteTema.Azul,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = ClienteTema.Texto,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = ClienteTema.Texto,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
    }
}