package com.example.gymcontrol.ui.encargado.reportes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import androidx.compose.ui.platform.LocalContext
import com.example.gymcontrol.data.remote.CancelacionRow
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.data.remote.EvaluacionCancelacion
import com.example.gymcontrol.data.remote.ReporteRow
import com.example.gymcontrol.data.remote.SolicitudCancelacionRow
import com.example.gymcontrol.ui.recepcion.clientes.ResultadoPolitica
import com.example.gymcontrol.data.remote.ReportesRepository
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.components.dinero
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Pagos", "Gastos", "Reportes")

data class ReporteMensualItem(
    val periodo: String,
    val servicios: Double,
    val asesorias: Double,
    val gastos: Double,
    val total: Double
)

@Composable
fun ReportesScreen(onNavigate: (String) -> Unit = {}) {
    var reportes by remember { mutableStateOf<List<ReporteRow>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var cancelaciones by remember { mutableStateOf<List<CancelacionRow>>(emptyList()) }
    var entregando by remember { mutableStateOf<CancelacionRow?>(null) }
    var solicitudes by remember { mutableStateOf<List<SolicitudCancelacionRow>>(emptyList()) }
    // Solicitud que se va a autorizar (true) o rechazar (false)
    var resolviendo by remember { mutableStateOf<Pair<SolicitudCancelacionRow, Boolean>?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Se recarga cada vez que la pantalla vuelve a mostrarse (p. ej. después de
    // aceptar un pago en "Pagos" o registrar un gasto), no solo la primera vez.
    var recargas by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) recargas++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(recargas) {
        if (recargas == 0) return@LaunchedEffect
        try {
            reportes = ReportesRepository.listar()
            cancelaciones = ReportesRepository.cancelaciones()
            solicitudes = ReportesRepository.solicitudesCancelacion()
            errorMsg = null
        } catch (e: Exception) {
            errorMsg = "No se pudo cargar: ${e.message}"
        }
        cargando = false
    }

    GymScaffold(
        currentSection = "Reportes",
        sections = ADMIN_SECTIONS,
        onNavigate = onNavigate
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (cargando) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.common_loading), color = ClienteTema.TextoSec)
                }
                Spacer(Modifier.height(8.dp))
            }
            errorMsg?.let { msg ->
                Text(msg, color = ClienteTema.Rojo)
                Spacer(Modifier.height(8.dp))
            }
            info?.let { msg ->
                Text(msg, color = ClienteTema.Verde, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Solicitudes de cancelación que registró Recepción: van primero porque
                // esperan la decisión del Encargado
                if (solicitudes.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.reports_requests_title, solicitudes.size),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = ClienteTema.Rojo,
                            fontSize = 20.sp
                        )
                    }
                    items(solicitudes, key = { "sol-${it.id}" }) { sol ->
                        SolicitudCard(
                            sol,
                            onAutorizar = { resolviendo = sol to true },
                            onRechazar = { resolviendo = sol to false }
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.title_monthly_report),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ClienteTema.Texto,
                        fontSize = 22.sp
                    )
                }

                items(reportes) { r ->
                    val total = r.servicios + r.asesorias - r.gastos
                    MonthlyReportCard(
                        item = ReporteMensualItem(
                            periodo = ReportesRepository.nombrePeriodo(r.periodo),
                            servicios = r.servicios,
                            asesorias = r.asesorias,
                            gastos = r.gastos,
                            total = total
                        )
                    )
                }

                // Cancelaciones y reembolsos: los pendientes se marcan como entregados aquí
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.reports_cancellations_title),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = ClienteTema.Texto,
                        fontSize = 20.sp
                    )
                }
                if (!cargando && cancelaciones.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.reports_cancellations_empty),
                            color = ClienteTema.TextoSec,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                items(cancelaciones, key = { it.id }) { c ->
                    CancelacionCard(c, onEntregar = { entregando = c })
                }
            }
        }

        resolviendo?.let { (sol, autorizar) ->
            var nota by remember(sol.id, autorizar) { mutableStateOf("") }
            val msgError = stringResource(R.string.reports_request_failed)
            val msgAutorizada = stringResource(R.string.reports_request_authorized, sol.cliente)
            val msgRechazada = stringResource(R.string.reports_request_rejected, sol.cliente)
            AlertDialog(
                onDismissRequest = { resolviendo = null },
                containerColor = ClienteTema.Beige,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        stringResource(
                            if (autorizar) R.string.reports_request_authorize_title
                            else R.string.reports_request_reject_title,
                            sol.cliente
                        ),
                        color = ClienteTema.Texto,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            stringResource(
                                if (autorizar) {
                                    if (sol.reembolso > 0) R.string.reports_request_authorize_refund
                                    else R.string.reports_request_authorize_no_refund
                                } else R.string.reports_request_reject_text
                            ),
                            color = ClienteTema.TextoSec
                        )
                        if (!autorizar) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = nota,
                                onValueChange = { nota = it.take(300) },
                                label = { Text(stringResource(R.string.reports_request_reject_reason)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ClienteTema.AzulSel,
                                    unfocusedBorderColor = ClienteTema.Borde,
                                    focusedTextColor = ClienteTema.Texto,
                                    unfocusedTextColor = ClienteTema.Texto,
                                    cursorColor = ClienteTema.AzulSel,
                                    focusedLabelColor = ClienteTema.AzulSel,
                                    unfocusedLabelColor = ClienteTema.TextoSec,
                                    focusedContainerColor = ClienteTema.CampoFondo,
                                    unfocusedContainerColor = ClienteTema.CampoFondo
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            resolviendo = null
                            scope.launch {
                                try {
                                    ReportesRepository.resolverCancelacion(
                                        SesionActual.correo ?: "", sol.id, autorizar,
                                        nota.takeIf { it.isNotBlank() }
                                    )
                                    reportes = ReportesRepository.listar()
                                    cancelaciones = ReportesRepository.cancelaciones()
                                    solicitudes = ReportesRepository.solicitudesCancelacion()
                                    errorMsg = null
                                    info = if (autorizar) msgAutorizada else msgRechazada
                                } catch (e: Exception) {
                                    info = null
                                    errorMsg = e.message?.substringBefore("\nCode:") ?: msgError
                                }
                            }
                        },
                        enabled = autorizar || nota.trim().length >= 3,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (autorizar) ClienteTema.AzulSel else ClienteTema.Rojo,
                            contentColor = ClienteTema.SobreDorado
                        )
                    ) {
                        Text(
                            stringResource(
                                if (autorizar) R.string.reports_request_authorize
                                else R.string.reports_request_reject
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { resolviendo = null },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ClienteTema.Borde),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                    ) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        entregando?.let { c ->
            val msgError = stringResource(R.string.reports_refund_deliver_failed)
            AlertDialog(
                onDismissRequest = { entregando = null },
                containerColor = ClienteTema.Beige,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(stringResource(R.string.common_confirm), color = ClienteTema.Texto, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        stringResource(
                            R.string.reports_refund_confirm_delivered,
                            dinero(c.reembolso), c.cliente, c.metodoReembolso ?: "—"
                        ),
                        color = ClienteTema.TextoSec
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            entregando = null
                            scope.launch {
                                try {
                                    ReportesRepository.entregarReembolso(SesionActual.correo ?: "", c.reembolsoId!!)
                                    reportes = ReportesRepository.listar()
                                    cancelaciones = ReportesRepository.cancelaciones()
                                    errorMsg = null
                                } catch (e: Exception) {
                                    info = null
                                    errorMsg = e.message?.substringBefore("\nCode:") ?: msgError
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClienteTema.AzulSel,
                            contentColor = ClienteTema.SobreDorado
                        )
                    ) { Text(stringResource(R.string.common_yes_continue), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { entregando = null },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ClienteTema.Borde),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                    ) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

/** Solicitud pendiente: quién la registró, el motivo y lo que aplicaría al autorizarla. */
@Composable
private fun SolicitudCard(s: SolicitudCancelacionRow, onAutorizar: () -> Unit, onRechazar: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
        border = BorderStroke(1.5.dp, ClienteTema.Rojo),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${s.cliente} · ${s.plan ?: ""}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ClienteTema.Texto
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    R.string.reports_request_meta,
                    ClientesRepository.fechaApp(context, fechaMx(s.solicitadaEn)) + " " + horaMx(s.solicitadaEn),
                    s.solicito ?: "—",
                    stringResource(
                        if (s.tipo == "ERROR_COBRO") R.string.cancel_membership_type_billing_error
                        else R.string.cancel_membership_type_voluntary
                    )
                ),
                fontSize = 13.sp,
                color = ClienteTema.TextoSec
            )
            Text(stringResource(R.string.reports_cancel_reason, s.motivo), fontSize = 13.sp, color = ClienteTema.TextoSec)
            Spacer(Modifier.height(8.dp))
            ResultadoPolitica(
                EvaluacionCancelacion(
                    membresiaId = 0, plan = s.plan, fechaFin = s.fechaFin, horas = s.horas,
                    reembolso = s.reembolso, metodo = s.metodo, regla = s.regla,
                    fechaLimite = s.fechaLimite
                )
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRechazar,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClienteTema.Rojo),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Rojo)
                ) { Text(stringResource(R.string.reports_request_reject)) }
                Button(
                    onClick = onAutorizar,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.AzulSel,
                        contentColor = ClienteTema.SobreDorado
                    )
                ) { Text(stringResource(R.string.reports_request_authorize), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private val HORA_MX = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.getDefault())

private fun horaMx(iso: String): String =
    runCatching {
        OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.of("America/Mexico_City")).format(HORA_MX)
    }.getOrDefault("")

/** Fecha y hora de la base (UTC) -> día en hora de México ("yyyy-MM-dd"). */
private fun fechaMx(iso: String?): String? = iso?.let {
    runCatching {
        OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.of("America/Mexico_City")).toLocalDate().toString()
    }.getOrDefault(it)
}

@Composable
private fun CancelacionCard(c: CancelacionRow, onEntregar: () -> Unit) {
    val context = LocalContext.current
    val hoy = LocalDate.now(ZoneId.of("America/Mexico_City")).toString()
    val atrasado = c.reembolsoPendiente && c.fechaLimite != null && c.fechaLimite < hoy
    val metodo = c.metodoReembolso ?: "—"
    val (estado, color) = when {
        c.reembolsoId == null -> stringResource(
            R.string.reports_no_refund, ClientesRepository.fechaApp(context, c.fechaFin)
        ) to ClienteTema.TextoSec
        c.reembolsoPendiente -> stringResource(
            if (atrasado) R.string.reports_refund_overdue else R.string.reports_refund_pending,
            dinero(c.reembolso), metodo, ClientesRepository.fechaApp(context, c.fechaLimite)
        ) to ClienteTema.Rojo
        else -> stringResource(
            R.string.reports_refund_delivered,
            dinero(c.reembolso), metodo, ClientesRepository.fechaApp(context, fechaMx(c.entregadoEn))
        ) to ClienteTema.Verde
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
        border = BorderStroke(1.5.dp, if (atrasado) ClienteTema.Rojo else ClienteTema.Borde),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${c.cliente} · ${c.detalle ?: ""}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ClienteTema.Texto
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    R.string.reports_cancel_meta,
                    ClientesRepository.fechaApp(context, fechaMx(c.canceladaEn)),
                    stringResource(
                        if (c.tipoCancelacion == "ERROR_COBRO") R.string.cancel_membership_type_billing_error
                        else R.string.cancel_membership_type_voluntary
                    ),
                    c.autorizo ?: "—"
                ),
                fontSize = 13.sp,
                color = ClienteTema.TextoSec
            )
            c.motivo?.takeIf { it.isNotBlank() }?.let {
                Text(stringResource(R.string.reports_cancel_reason, it), fontSize = 13.sp, color = ClienteTema.TextoSec)
            }
            Spacer(Modifier.height(6.dp))
            Text(estado, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
            if (c.reembolsoPendiente) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onEntregar,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.AzulSel,
                        contentColor = ClienteTema.SobreDorado
                    )
                ) { Text(stringResource(R.string.reports_refund_mark_delivered), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun MonthlyReportCard(item: ReporteMensualItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
        border = BorderStroke(1.5.dp, ClienteTema.Borde),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.periodo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ClienteTema.Texto
                )
                val totalColor = if (item.total >= 0) ClienteTema.Verde else ClienteTema.Rojo
                Text(
                    text = stringResource(R.string.label_total_format, dinero(item.total)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = totalColor
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = ClienteTema.Borde.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReportDetailItem(label = stringResource(R.string.label_services), value = item.servicios)
                ReportDetailItem(label = stringResource(R.string.label_advisories), value = item.asesorias)
                ReportDetailItem(label = stringResource(R.string.label_expenses), value = item.gastos)
            }
        }
    }
}

@Composable
fun ReportDetailItem(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = ClienteTema.TextoSec)
        Spacer(Modifier.height(2.dp))
        Text(text = dinero(value), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ClienteTema.Texto)
    }
}
