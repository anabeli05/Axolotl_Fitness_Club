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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// ---------- Modelo de UI ----------
data class ClienteScan(
    val id: Long,
    val nombre: String,
    val membresia: String,
    val activo: Boolean,
    val dentroDesde: String? = null,
    val fotoUrl: String? = null
)

// Aviso que aparece unos segundos después de registrar (o rechazar) una entrada/salida
private data class Aviso(val texto: String, val esError: Boolean, val id: Long = System.nanoTime())

/**
 * Acceso por reconocimiento facial (reemplaza al escáner QR).
 * La cámara detecta el rostro más cercano, lo identifica y registra
 * ENTRADA (1) o SALIDA (0) sola, sin botones. Los botones quedan para corregir a mano.
 */
@Composable
fun AccesoFacialScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val main = remember { Handler(Looper.getMainLooper()) }

    var cliente by remember { mutableStateOf<ClienteScan?>(null) }
    var aviso by remember { mutableStateOf<Aviso?>(null) }
    var dentro by remember { mutableStateOf(0) }
    var indicacion by remember { mutableStateOf("Colócate frente a la cámara") }
    var progreso by remember { mutableStateOf(0f) }

    // Motor facial (MobileFaceNet) y captura: 3 muestras seguidas del rostro más cercano
    val reconocedor = remember { runCatching { ReconocedorFacial(context) }.getOrNull() }
    val captura = remember(reconocedor) { reconocedor?.let { CapturaFacial(it, muestrasNecesarias = 3) } }
    val pausa = remember { AtomicBoolean(false) }
    DisposableEffect(reconocedor) { onDispose { reconocedor?.close() } }

    // Personas dentro: se consulta cada 5 s (además de actualizarse con cada movimiento)
    LaunchedEffect(Unit) {
        while (true) {
            try { dentro = AccesoFacialRepository.ocupacionActual() } catch (_: Exception) { }
            delay(5_000)
        }
    }

    // El aviso se quita solo; si fue un registro exitoso también se limpia la tarjeta del cliente
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
            "ENTRADA" -> Aviso("$nombre ha ingresado · ${formatHora(r.fechaHora)}", esError = false)
            "SALIDA" -> Aviso(
                "$nombre ha salido" + (duracion(r.entradaDesde, r.fechaHora)?.let { " · Estuvo $it" } ?: ""),
                esError = false
            )
            "DENEGADO" -> Aviso("Acceso denegado: membresía inactiva. Pasa a renovar", esError = true)
            "REPETIDO" -> Aviso("$nombre ya fue registrado hace un momento", esError = false)
            "YA_DENTRO" -> Aviso("$nombre ya tiene una entrada registrada", esError = true)
            "SIN_ENTRADA" -> Aviso("$nombre no tiene una entrada abierta", esError = true)
            else -> Aviso("Acceso denegado: rostro no registrado", esError = true)
        }
    }

    // Se llama cuando la captura entregó un vector
    fun identificar(vector: FloatArray) {
        scope.launch {
            try {
                val r = AccesoFacialRepository.acceso(vector)
                mostrar(r)
                // Si no lo reconoció se le deja intentar de nuevo con el mismo rostro
                if (r.resultado == "NO_RECONOCIDO") captura?.olvidarProcesado()
            } catch (e: Exception) {
                captura?.olvidarProcesado()
                aviso = Aviso("Sin conexión con la base de datos", esError = true)
            }
            delay(2_500)
            progreso = 0f
            pausa.set(false)
        }
    }

    // Se ejecuta en el hilo de la cámara
    val onFrame: (Bitmap, List<Face>) -> Unit = onFrame@{ frame, rostros ->
        val c = captura ?: return@onFrame
        if (pausa.get()) return@onFrame
        when (val paso = c.procesar(frame, rostros)) {
            is PasoFacial.Indicacion -> main.post {
                indicacion = paso.texto
                progreso = paso.progreso
            }
            is PasoFacial.Listo -> {
                pausa.set(true)
                c.marcarProcesado()
                main.post {
                    indicacion = "Verificando…"
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
                aviso = Aviso("Sin conexión con la base de datos", esError = true)
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
                    "Reconocimiento facial",
                    color = ClienteTema.Texto,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                AforoChip(personas = dentro)
            }

            // ---------- Tarjeta: Escanear rostro (cámara real) ----------
            GymCard {
                Text(
                    "Escanear rostro",
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
                    if (captura == null) "No se pudo cargar el modelo facial" else indicacion,
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

            // ---------- Tarjeta: Información del cliente ----------
            GymCard {
                Text(
                    "Información del cliente",
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
                        "Escanea un rostro para ver la información del cliente",
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
                            Text("Membresía: ${actual.membresia}", color = ClienteTema.TextoSec, fontSize = 14.sp)
                            Row {
                                Text("Estado: ", color = ClienteTema.TextoSec, fontSize = 14.sp)
                                Text(
                                    if (actual.activo) "Activo" else "Inactivo",
                                    color = if (actual.activo) ClienteTema.Verde else ClienteTema.Rojo,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            actual.dentroDesde?.let { desde ->
                                Text(
                                    "Dentro desde ${formatHora(desde)}",
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

                // Aviso de resultado (éxito o error)
                aviso?.let {
                    AvisoBanner(it)
                    Spacer(Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SolidButton(
                        text = "Entrada",
                        color = ClienteTema.Verde,
                        enabled = cliente != null,
                        onClick = { manual(esEntrada = true) },
                        modifier = Modifier.weight(1f)
                    )
                    SolidButton(
                        text = "Salida",
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

// ---------- Utilidades ----------

private val HORA = DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es-MX"))

private fun instante(iso: String?): OffsetDateTime? =
    iso?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

private fun formatHora(iso: String?): String =
    instante(iso)?.atZoneSameInstant(ZoneId.systemDefault())?.format(HORA) ?: "—"

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

// ---------- Componentes reutilizables ----------

/** Contador de personas dentro del gimnasio. */
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
            text = "Dentro: $personas",
            color = ClienteTema.Texto,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Mensaje verde (éxito) o rojo (error) con fondo tintado. */
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

/** Tarjeta grafito con borde degradado dorado (igual que el resto de la app del cliente). */
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

/** Las 4 esquinas doradas del visor. */
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
            // Un solo fondo: opaco si está activo, apagado si no
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
