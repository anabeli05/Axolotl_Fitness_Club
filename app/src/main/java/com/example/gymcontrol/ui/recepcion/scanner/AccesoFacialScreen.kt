package com.example.gymcontrol.ui.recepcion.scanner

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.biometria.CapturaFacial
import com.example.gymcontrol.data.biometria.PasoFacial
import com.example.gymcontrol.data.biometria.ReconocedorFacial
import com.example.gymcontrol.data.remote.AccesoFacialRepository
import com.example.gymcontrol.data.remote.MovimientoRow
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.GymScaffold
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

private val CardBorder = Brush.linearGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel))

data class ClienteScan(
    val id: Long,
    val nombre: String,
    val membresia: String,
    val activo: Boolean,
    val dentroDesde: String? = null,
    val fotoUrl: String? = null
)

private data class Aviso(val texto: String, val esError: Boolean, val id: Long = System.nanoTime())

@Composable
fun AccesoFacialScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val main = remember { Handler(Looper.getMainLooper()) }

    var cliente by remember { mutableStateOf<ClienteScan?>(null) }
    var aviso by remember { mutableStateOf<Aviso?>(null) }
    var dentro by remember { mutableStateOf(0) }
    var progreso by remember { mutableStateOf(0f) }
    var indicacion by remember { mutableStateOf("") }
    val initialIndicacion = stringResource(R.string.facial_prompt_camera)
    LaunchedEffect(Unit) { if (indicacion.isEmpty()) indicacion = initialIndicacion }

    val reconocedor = remember { runCatching { ReconocedorFacial(context) }.getOrNull() }
    val captura = remember(reconocedor) { reconocedor?.let { CapturaFacial(it, muestrasNecesarias = 3) } }
    val pausa = remember { AtomicBoolean(false) }
    DisposableEffect(reconocedor) { onDispose { reconocedor?.close() } }

    val msgDbError = stringResource(R.string.recepcion_scanner_db_error)
    val msgDeniedInactive = stringResource(R.string.recepcion_scanner_denied_inactive)
    val msgUnregistered = stringResource(R.string.recepcion_scanner_denied_unregistered)
    val msgVerifying = stringResource(R.string.facial_prompt_verifying)
    val msgModelError = stringResource(R.string.recepcion_face_model_load_error)

    LaunchedEffect(Unit) {
        while (true) {
            try { dentro = AccesoFacialRepository.ocupacionActual() } catch (_: Exception) { }
            delay(5_000)
        }
    }

    LaunchedEffect(aviso) {
        val actual = aviso
        if (actual != null) {
            delay(3500)
            if (!actual.esError) cliente = null
            aviso = null
        }
    }

    fun mostrar(r: MovimientoRow) {
        dentro = r.dentroTotal
        val id = r.clienteId
        cliente = if (id == null) null else ClienteScan(
            id = id,
            nombre = r.nombre ?: "Cliente",
            membresia = r.membresia ?: "—",
            activo = r.activa == true,
            dentroDesde = if (r.esEntrada == true) r.entradaDesde ?: r.fechaHora else null
        )
        val nombre = r.nombre ?: "El cliente"
        aviso = when (r.resultado) {
            "ENTRADA" -> Aviso(context.getString(R.string.access_entered_at, nombre, formatHora(r.fechaHora)), esError = false)
            "SALIDA" -> {
                val desde = r.entradaDesde
                val hasta = r.fechaHora
                val desdeInst = instante(desde)
                val hastaInst = instante(hasta)
                val text = if (desdeInst != null && hastaInst != null) {
                    val minutos = java.time.Duration.between(desdeInst, hastaInst).toMinutes()
                    when {
                        minutos < 1 -> context.getString(R.string.access_exited_under_minute, nombre)
                        minutos < 60 -> context.getString(R.string.access_exited_short_time, nombre, minutos.toInt())
                        else -> {
                            val durStr = "${minutos / 60} h ${minutos % 60} min"
                            context.getString(R.string.access_exited_duration, nombre, durStr)
                        }
                    }
                } else {
                    context.getString(R.string.access_exited_under_minute, nombre)
                }
                Aviso(text, esError = false)
            }
            "DENEGADO" -> Aviso(msgDeniedInactive, esError = true)
            "REPETIDO" -> Aviso(context.getString(R.string.access_already_registered_just_now, nombre), esError = false)
            "YA_DENTRO" -> Aviso("$nombre ya tiene una entrada registrada", esError = true)
            "SIN_ENTRADA" -> Aviso("$nombre no tiene una entrada abierta", esError = true)
            else -> Aviso(msgUnregistered, esError = true)
        }
    }

    fun identificar(vector: FloatArray) {
        scope.launch {
            try {
                val r = AccesoFacialRepository.acceso(vector)
                mostrar(r)
                if (r.resultado == "NO_RECONOCIDO") captura?.olvidarProcesado()
            } catch (e: Exception) {
                captura?.olvidarProcesado()
                aviso = Aviso(msgDbError, esError = true)
            }
            delay(2_500)
            progreso = 0f
            pausa.set(false)
        }
    }

    val onFrame: (Bitmap, List<Face>) -> Unit = onFrame@{ frame, rostros ->
        val c = captura ?: return@onFrame
        if (pausa.get()) return@onFrame
        when (val paso = c.procesar(frame, rostros)) {
            is PasoFacial.Indicacion -> main.post {
                indicacion = context.getString(paso.textRes)
                progreso = paso.progreso
            }
            is PasoFacial.Listo -> {
                pausa.set(true)
                c.marcarProcesado()
                main.post {
                    indicacion = msgVerifying
                    progreso = 1f
                    identificar(paso.vector)
                }
            }
        }
    }

    fun manual(esEntrada: Boolean) {
        val c = cliente ?: return
        scope.launch {
            try {
                mostrar(AccesoFacialRepository.manual(c.id, esEntrada))
            } catch (e: Exception) {
                aviso = Aviso(msgDbError, esError = true)
            }
        }
    }

    GymScaffold(
        currentSection = "Home",
        onNavigate = onNavigate
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.facial_recognition_title),
                    color = ClienteTema.Texto,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                AforoChip(personas = dentro)
            }

            GymCard {
                Text(
                    stringResource(R.string.scan_face),
                    color = ClienteTema.Texto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (captura != null) {
                        FaceCameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            onFrame = onFrame
                        )
                    }
                    ScannerCorners(Modifier.fillMaxSize())
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    if (captura == null) msgModelError else indicacion,
                    color = if (captura == null) ClienteTema.Rojo else ClienteTema.TextoSec,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progreso },
                    color = ClienteTema.AzulSel,
                    trackColor = ClienteTema.Borde.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            GymCard {
                Text(
                    stringResource(R.string.client_info_title),
                    color = ClienteTema.Texto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = ClienteTema.Borde.copy(alpha = 0.6f))
                Spacer(Modifier.height(16.dp))

                val actual = cliente
                if (actual == null) {
                    Text(
                        stringResource(R.string.scan_face_prompt),
                        color = ClienteTema.TextoSec,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(2.dp, ClienteTema.AzulSel, CircleShape)
                                .background(ClienteTema.Navy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = ClienteTema.TextoSec,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                actual.nombre,
                                color = ClienteTema.Texto,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.recepcion_scanner_membership_prefix, actual.membresia), color = ClienteTema.TextoSec, fontSize = 14.sp)
                            Text(
                                stringResource(
                                    R.string.status_label,
                                    if (actual.activo) stringResource(R.string.status_active) else stringResource(R.string.status_inactive)
                                ),
                                color = if (actual.activo) ClienteTema.Verde else ClienteTema.Rojo,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            actual.dentroDesde?.let { desde ->
                                Text(
                                    stringResource(R.string.inside_since_format, formatHora(desde)),
                                    color = ClienteTema.AzulSel,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = ClienteTema.Borde.copy(alpha = 0.6f))
                Spacer(Modifier.height(16.dp))

                aviso?.let {
                    AvisoBanner(it)
                    Spacer(Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SolidButton(
                        text = stringResource(R.string.recepcion_scanner_entry_btn),
                        color = ClienteTema.Verde,
                        enabled = cliente != null,
                        onClick = { manual(esEntrada = true) },
                        modifier = Modifier.weight(1f)
                    )
                    SolidButton(
                        text = stringResource(R.string.recepcion_scanner_exit_btn),
                        color = ClienteTema.Rojo,
                        enabled = cliente != null,
                        onClick = { manual(esEntrada = false) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun instante(iso: String?): OffsetDateTime? =
    iso?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

private fun formatHora(iso: String?): String {
    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    return instante(iso)?.atZoneSameInstant(ZoneId.systemDefault())?.format(formatter) ?: "—"
}

private fun duracion(desdeIso: String?, hastaIso: String?): String? {
    val desde = instante(desdeIso) ?: return null
    val hasta = instante(hastaIso) ?: return null
    val minutos = java.time.Duration.between(desde, hasta).toMinutes()
    return when {
        minutos < 1 -> "menos de 1 min"
        minutos < 60 -> "$minutos min"
        else -> "${minutos / 60} h ${minutos % 60} min"
    }
}

@Composable
private fun AforoChip(personas: Int) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .background(ClienteTema.AzulSel.copy(alpha = 0.15f), shape)
            .border(1.dp, ClienteTema.AzulSel, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = stringResource(R.string.recepcion_scanner_inside_count, personas),
            color = ClienteTema.Texto,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AvisoBanner(aviso: Aviso) {
    val color = if (aviso.esError) ClienteTema.Rojo else ClienteTema.Verde
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.15f), shape)
            .border(1.dp, color, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = aviso.texto,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GymCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ClienteTema.Beige)
            .border(BorderStroke(1.5.dp, CardBorder), shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
internal fun ScannerCorners(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 4.dp.toPx()
        val len = 36.dp.toPx()
        val w = size.width
        val h = size.height
        fun line(a: Offset, b: Offset) =
            drawLine(ClienteTema.AzulSel, a, b, strokeWidth = stroke, cap = StrokeCap.Round)

        line(Offset(0f, 0f), Offset(len, 0f)); line(Offset(0f, 0f), Offset(0f, len))
        line(Offset(w, 0f), Offset(w - len, 0f)); line(Offset(w, 0f), Offset(w, len))
        line(Offset(0f, h), Offset(len, h)); line(Offset(0f, h), Offset(0f, h - len))
        line(Offset(w, h), Offset(w - len, h)); line(Offset(w, h), Offset(w, h - len))
    }
}

@Composable
private fun SolidButton(
    text: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(if (enabled) color else color.copy(alpha = 0.3f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}
