package com.example.gymcontrol.ui.recepcion.scanner

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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardBorder = Brush.linearGradient(listOf(GymColors.Purple, GymColors.Gold))
private val RECEPCION_SECTIONS = listOf("Home", "Clientes", "Solicitudes")

// ---------- Modelo de UI ----------
data class ClienteScan(
    val nombre: String,
    val membresia: String,
    val activo: Boolean,
    val fotoUrl: String? = null
)

// Aviso que aparece unos segundos después de registrar (o rechazar) una entrada/salida
private data class Aviso(val texto: String, val esError: Boolean, val id: Long = System.nanoTime())

@Composable
fun QrScannerScreen(
    onNavigate: (String) -> Unit = {},
    // Ganchos para guardar en Supabase cuando esté conectado
    onEntrada: (ClienteScan, Long) -> Unit = { _, _ -> },
    onSalida: (ClienteScan, Long, Long) -> Unit = { _, _, _ -> }
) {
    var cliente by remember { mutableStateOf<ClienteScan?>(null) }
    var aviso by remember { mutableStateOf<Aviso?>(null) }

    // Solo para la demo: quién está dentro (membresía -> hora de entrada). TODO: reemplazar por Supabase
    val dentro = remember { mutableStateMapOf<String, Long>() }

    // Evita procesar el mismo QR muchas veces seguidas mientras la cámara lo sigue viendo
    var ultimoCodigo by remember { mutableStateOf("") }
    var ultimoEscaneo by remember { mutableStateOf(0L) }

    // El aviso se quita solo; si fue un registro exitoso también se limpia la tarjeta del cliente
    LaunchedEffect(aviso) {
        val actual = aviso
        if (actual != null) {
            delay(3500)
            if (!actual.esError) cliente = null
            aviso = null
        }
    }

    fun escanear(codigo: String) {
        val texto = codigo.trim()
        val ahora = System.currentTimeMillis()
        if (texto == ultimoCodigo && ahora - ultimoEscaneo < 3000) return
        ultimoCodigo = texto
        ultimoEscaneo = ahora

        val encontrado = GymApp.repository.users()
            .firstOrNull { it.role == UserRole.CLIENTE && it.membershipNumber == texto }

        aviso = null
        if (encontrado == null) {
            cliente = null
            aviso = Aviso("Código no reconocido", esError = true)
        } else {
            cliente = ClienteScan(
                nombre = encontrado.name,
                membresia = texto,
                activo = encontrado.status.name == "ACTIVO"
            )
        }
    }

    fun registrarEntrada() {
        val c = cliente ?: return
        val ahora = System.currentTimeMillis()
        aviso = when {
            !c.activo -> Aviso("Membresía vencida. Pasa a renovar", esError = true)
            c.membresia in dentro -> Aviso("${c.nombre} ya tiene una entrada registrada", esError = true)
            else -> {
                dentro[c.membresia] = ahora
                onEntrada(c, ahora)
                Aviso("Entrada registrada · ${formatHora(ahora)}", esError = false)
            }
        }
    }

    fun registrarSalida() {
        val c = cliente ?: return
        val ahora = System.currentTimeMillis()
        val horaEntrada = dentro[c.membresia]
        aviso = if (horaEntrada == null) {
            Aviso("${c.nombre} no tiene una entrada abierta", esError = true)
        } else {
            dentro.remove(c.membresia)
            onSalida(c, horaEntrada, ahora)
            Aviso(
                "Salida registrada · Estuvo ${formatDuracion(ahora - horaEntrada)}",
                esError = false
            )
        }
    }

    GymScaffold(
        currentSection = "Home",
        sections = RECEPCION_SECTIONS,
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
                    "Escanear código QR",
                    color = GymColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                AforoChip(personas = dentro.size)
            }

            // ---------- Tarjeta: Escanear código (cámara real) ----------
            GymCard {
                Text(
                    "Escanear código",
                    color = GymColors.TextPrimary,
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
                    QrCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onQrDetected = { qrText -> escanear(qrText) }
                    )
                    ScannerCorners(Modifier.fillMaxSize())
                }
            }

            // ---------- Tarjeta: Información del cliente ----------
            GymCard {
                Text(
                    "Información del cliente",
                    color = GymColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = GymColors.Gold.copy(alpha = 0.6f))
                Spacer(Modifier.height(16.dp))

                val actual = cliente
                if (actual == null) {
                    Text(
                        "Escanea un código para ver la información del cliente",
                        color = GymColors.TextSecondary,
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
                                .border(2.dp, GymColors.TextPrimary, CircleShape)
                                .background(GymColors.Border),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = GymColors.TextSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                actual.nombre,
                                color = GymColors.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Membresía: ${actual.membresia}", color = GymColors.TextSecondary, fontSize = 14.sp)
                            Row {
                                Text("Estado: ", color = GymColors.TextSecondary, fontSize = 14.sp)
                                Text(
                                    if (actual.activo) "Activo" else "Inactivo",
                                    color = if (actual.activo) GymColors.Green else GymColors.Red,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            dentro[actual.membresia]?.let { desde ->
                                Text(
                                    "Dentro desde ${formatHora(desde)}",
                                    color = GymColors.Gold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = GymColors.Gold.copy(alpha = 0.6f))
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
                        color = GymColors.Green,
                        enabled = cliente != null,
                        onClick = { registrarEntrada() },
                        modifier = Modifier.weight(1f)
                    )
                    SolidButton(
                        text = "Salida",
                        color = GymColors.Red,
                        enabled = cliente != null,
                        onClick = { registrarSalida() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ---------- Utilidades ----------

private fun formatHora(millis: Long): String =
    SimpleDateFormat("hh:mm a", Locale.forLanguageTag("es-MX")).format(Date(millis))

private fun formatDuracion(millis: Long): String {
    val minutos = millis / 60_000
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
            .background(GymColors.Purple.copy(alpha = 0.15f), shape)
            .border(1.dp, GymColors.Purple, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Dentro: $personas",
            color = GymColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Mensaje verde (éxito) o rojo (error) con fondo tintado. */
@Composable
private fun AvisoBanner(aviso: Aviso) {
    val color = if (aviso.esError) GymColors.Red else GymColors.Green
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

/** Tarjeta oscura con borde degradado morado → dorado (igual que el resto de la app). */
@Composable
private fun GymCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GymColors.Surface)
            .border(BorderStroke(1.5.dp, CardBorder), shape)
            .padding(16.dp),
        content = content
    )
}

/** Las 4 esquinas moradas del visor. */
@Composable
private fun ScannerCorners(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 4.dp.toPx()
        val len = 36.dp.toPx()
        val w = size.width
        val h = size.height
        fun line(a: Offset, b: Offset) =
            drawLine(GymColors.Purple, a, b, strokeWidth = stroke, cap = StrokeCap.Round)

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