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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val CardBorder = Brush.linearGradient(listOf(GymColors.Purple, GymColors.Gold))
private val RECEPCION_SECTIONS = listOf("Home", "Clientes", "Solicitudes")

// ---------- Modelo de UI ----------
data class ClienteScan(
    val nombre: String,
    val membresia: String,
    val activo: Boolean,
    val fotoUrl: String? = null
)

@Composable
fun QrScannerScreen(
    cliente: ClienteScan? = null,              // null = aún no se escanea nada
    onNavigate: (String) -> Unit = {},
    onEntrada: () -> Unit = {},
    onSalida: () -> Unit = {}
) {
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
            Text(
                "Escanear código QR",
                color = GymColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // ---------- Tarjeta: Escanear código ----------
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
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // TODO: aquí va tu CameraX PreviewView (AndroidView) o ML Kit
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.75f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GymColors.Background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = GymColors.TextSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Apunta al código QR", color = GymColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
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

                if (cliente == null) {
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
                                cliente.nombre,
                                color = GymColors.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Membresía: ${cliente.membresia}", color = GymColors.TextSecondary, fontSize = 14.sp)
                            Row {
                                Text("Estado: ", color = GymColors.TextSecondary, fontSize = 14.sp)
                                Text(
                                    if (cliente.activo) "Activo" else "Inactivo",
                                    color = if (cliente.activo) GymColors.Green else GymColors.Red,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = GymColors.Gold.copy(alpha = 0.6f))
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SolidButton(
                        text = "Entrada",
                        color = GymColors.Green,
                        enabled = cliente != null,
                        onClick = onEntrada,
                        modifier = Modifier.weight(1f)
                    )
                    SolidButton(
                        text = "Salida",
                        color = GymColors.Red,
                        enabled = cliente != null,
                        onClick = onSalida,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ---------- Componentes reutilizables ----------

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
            .background(color, shape)
            .then(if (!enabled) Modifier.background(color.copy(alpha = 0.35f), shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}