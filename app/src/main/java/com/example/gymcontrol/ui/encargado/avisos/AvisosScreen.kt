package com.example.gymcontrol.ui.encargado.avisos

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.data.remote.ComunicadoRow
import com.example.gymcontrol.data.remote.ComunicadosRepository
import com.example.gymcontrol.data.remote.CorreoRepository
import com.example.gymcontrol.data.remote.TipoComunicado
import com.example.gymcontrol.ui.components.CalendarioDialog
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.manual.manualTarget
import com.example.gymcontrol.ui.manual.rememberManualLista
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private val MX = ZoneId.of("America/Mexico_City")

/**
 * Comunicados por correo a todos los usuarios activos: cambios en los Términos,
 * en la Política de Privacidad y de horario. El aviso de eliminación de cuenta
 * (30 días antes) lo manda solo avisos-diarios.
 */
@Composable
fun AvisosScreen(onNavigate: (String) -> Unit = {}) {
    var comunicados by remember { mutableStateOf<List<ComunicadoRow>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var nuevo by remember { mutableStateOf(false) }
    var reenviando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    suspend fun recargar() {
        try {
            comunicados = ComunicadosRepository.listar()
            error = null
        } catch (e: Exception) {
            error = context.getString(R.string.notices_load_failed, e.message ?: "")
        }
        cargando = false
    }

    // Se recarga al volver a la pantalla para ver el avance del envío
    var recargas by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) recargas++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(recargas) { if (recargas > 0) recargar() }

    GymScaffold(currentSection = "Avisos", onNavigate = onNavigate) {
        LazyColumn(
            state = rememberManualLista(),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.notices_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = ClienteTema.Texto,
                    fontSize = 22.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.notices_subtitle),
                    color = ClienteTema.TextoSec,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    onClick = { info = null; error = null; nuevo = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.AzulSel,
                        contentColor = ClienteTema.SobreDorado
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .manualTarget("avi_nuevo")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.notices_new), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            info?.let { item { Text(it, color = ClienteTema.Verde, fontSize = 14.sp) } }
            error?.let { item { Text(it, color = ClienteTema.Rojo, fontSize = 14.sp) } }
            if (cargando) {
                item { Text(stringResource(R.string.common_loading), color = ClienteTema.TextoSec) }
            } else if (comunicados.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.notices_empty),
                        color = ClienteTema.TextoSec,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }
            }
            items(comunicados, key = { it.id }) { c ->
                ComunicadoCard(
                    c,
                    enviando = reenviando,
                    onEnviarPendientes = {
                        reenviando = true
                        scope.launch {
                            val falla = CorreoRepository.enviarComunicados()
                            if (falla == null) {
                                error = null
                                info = context.getString(R.string.notices_sent)
                            } else {
                                info = null
                                error = context.getString(R.string.notices_send_failed, falla)
                            }
                            recargar()
                            reenviando = false
                        }
                    }
                )
            }
        }

        if (nuevo) {
            NuevoComunicadoDialog(
                onDismiss = { nuevo = false },
                onPublicado = { tipo, titulo, mensaje, fecha, listo ->
                    scope.launch {
                        try {
                            ComunicadosRepository.crear(SesionActual.correo ?: "", tipo, titulo, mensaje, fecha)
                            nuevo = false
                            val falla = CorreoRepository.enviarComunicados()
                            if (falla == null) {
                                error = null
                                info = context.getString(R.string.notices_sent)
                            } else {
                                info = null
                                error = context.getString(R.string.notices_saved_not_sent, falla)
                            }
                            recargar()
                        } catch (e: Exception) {
                            listo(e.message?.substringBefore("\nCode:")?.lineSequence()?.firstOrNull()
                                ?: context.getString(R.string.notices_publish_failed))
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun etiquetaTipo(tipo: String): String = stringResource(
    when (tipo) {
        "TERMINOS" -> R.string.notices_type_terms
        "PRIVACIDAD" -> R.string.notices_type_privacy
        else -> R.string.notices_type_schedule
    }
)

@Composable
private fun ComunicadoCard(c: ComunicadoRow, enviando: Boolean, onEnviarPendientes: () -> Unit) {
    val context = LocalContext.current
    val completo = c.enviados >= c.destinatarios
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClienteTema.Beige, RoundedCornerShape(14.dp))
            .border(1.5.dp, ClienteTema.Borde, RoundedCornerShape(14.dp))
            .padding(16.dp)
            .manualTarget("avi_card")
    ) {
        Text(
            etiquetaTipo(c.tipo).uppercase(),
            color = ClienteTema.AzulSel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(c.titulo, color = ClienteTema.Texto, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(4.dp))
        Text(c.mensaje, color = ClienteTema.TextoSec, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.notices_effective, ClientesRepository.fechaApp(context, c.fechaVigor)),
            color = ClienteTema.Texto,
            fontSize = 13.sp
        )
        Text(
            stringResource(
                R.string.notices_meta,
                ClientesRepository.fechaApp(context, fechaMx(c.creadoEn)),
                c.publico ?: "—"
            ),
            color = ClienteTema.TextoSec,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(
                if (completo) R.string.notices_progress_done else R.string.notices_progress_pending,
                c.enviados, c.destinatarios
            ),
            color = if (completo) ClienteTema.Verde else ClienteTema.Rojo,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        if (!completo) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onEnviarPendientes,
                enabled = !enviando,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClienteTema.AzulSel,
                    contentColor = ClienteTema.SobreDorado
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .manualTarget("avi_enviar")
            ) {
                Text(
                    stringResource(if (enviando) R.string.notices_sending else R.string.notices_send_pending),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun fechaMx(iso: String): String =
    runCatching {
        java.time.OffsetDateTime.parse(iso).atZoneSameInstant(MX).toLocalDate().toString()
    }.getOrDefault(iso)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoComunicadoDialog(
    onDismiss: () -> Unit,
    // listo(error) se llama solo si falla, para volver a habilitar el formulario
    onPublicado: (TipoComunicado, String, String, String, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var tipo by remember { mutableStateOf(TipoComunicado.HORARIO) }
    var titulo by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf<LocalDate?>(null) }
    var calendario by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val hoy = LocalDate.now(MX)
    val recomendada = hoy.plusDays(tipo.diasMinimos.toLong())
    // Se puede elegir desde hoy; si es antes de lo que piden los Términos, solo se advierte
    val conPocaAnticipacion = fecha != null && fecha!! < recomendada

    val campo = OutlinedTextFieldDefaults.colors(
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
    val listo = !enviando && titulo.trim().length >= 3 && mensaje.trim().length >= 10 && fecha != null

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        containerColor = ClienteTema.Beige,
        shape = RoundedCornerShape(16.dp),
        title = { Text(stringResource(R.string.notices_new), color = ClienteTema.Texto, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TipoComunicado.entries.forEach { t ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !enviando) { tipo = t }
                    ) {
                        RadioButton(
                            selected = tipo == t,
                            onClick = { tipo = t },
                            enabled = !enviando,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ClienteTema.AzulSel,
                                unselectedColor = ClienteTema.TextoSec
                            )
                        )
                        Text(etiquetaTipo(t.name), color = ClienteTema.Texto)
                    }
                }
                Text(
                    stringResource(R.string.notices_min_days, tipo.diasMinimos),
                    color = ClienteTema.TextoSec,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it.take(80) },
                    label = { Text(stringResource(R.string.notices_field_title)) },
                    placeholder = { Text(stringResource(R.string.notices_field_title_hint), color = ClienteTema.TextoSec) },
                    singleLine = true,
                    enabled = !enviando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = campo
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = mensaje,
                    onValueChange = { mensaje = it.take(1500) },
                    label = { Text(stringResource(R.string.notices_field_message)) },
                    placeholder = {
                        Text(
                            stringResource(
                                if (tipo == TipoComunicado.HORARIO) R.string.notices_field_message_hint_schedule
                                else R.string.notices_field_message_hint_terms
                            ),
                            color = ClienteTema.TextoSec
                        )
                    },
                    minLines = 4,
                    enabled = !enviando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = campo
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { calendario = true },
                    enabled = !enviando,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, ClienteTema.Borde),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, tint = ClienteTema.AzulSel)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        fecha?.let { stringResource(R.string.notices_effective, ClientesRepository.fechaApp(context, it.toString())) }
                            ?: stringResource(R.string.notices_pick_date)
                    )
                }
                if (conPocaAnticipacion) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(
                            R.string.notices_short_notice_warning,
                            tipo.diasMinimos,
                            java.time.temporal.ChronoUnit.DAYS.between(hoy, fecha!!).toInt()
                        ),
                        color = ClienteTema.Rojo,
                        fontSize = 12.sp
                    )
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = ClienteTema.Rojo, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.notices_recipients_note), color = ClienteTema.TextoSec, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !enviando,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, ClienteTema.Borde),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                ) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = {
                        enviando = true
                        error = null
                        onPublicado(tipo, titulo, mensaje, fecha.toString()) { falla ->
                            error = falla
                            enviando = false
                        }
                    },
                    enabled = listo,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.AzulSel,
                        contentColor = ClienteTema.SobreDorado
                    )
                ) {
                    Text(
                        stringResource(if (enviando) R.string.notices_sending else R.string.notices_publish),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )

    if (calendario) {
        val minMillis = hoy.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = (fecha ?: hoy).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minMillis
            }
        )
        CalendarioDialog(
            titulo = stringResource(R.string.notices_pick_date),
            estado = estado,
            onDismiss = { calendario = false },
            onAceptar = {
                estado.selectedDateMillis?.let {
                    fecha = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                }
                calendario = false
            }
        )
    }
}
