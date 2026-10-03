package com.example.gymcontrol.ui.cliente.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.ui.components.BarEntry
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.SimpleBarChart
import com.example.gymcontrol.ui.theme.GymColors

private val CLIENT_SECTIONS = listOf("Inicio", "Asesorías", "Perfil")

private val Purple = Color(0xFFA54FD9)
private val CardBackground = Color(0xFF1E1F20)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB5B5B5)

@Composable
fun ClienteHomeScreen(onNavigate: (String) -> Unit = {}) {
    GymScaffold(
        currentSection = "Inicio",
        sections = CLIENT_SECTIONS,
        onNavigate = onNavigate
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                Text(
                    text = "Escanea código QR",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    QrCard()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "QR de acceso · M-1001",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            item {
                SimpleBarChart(
                    title = "Clientes ahora mismo",
                    entries = listOf(
                        BarEntry("6am", 25f), BarEntry("8am", 35f), BarEntry("10am", 30f),
                        BarEntry("12pm", 15f), BarEntry("2pm", 27f), BarEntry("4pm", 42f),
                        BarEntry("6pm", 47f), BarEntry("8pm", 55f), BarEntry("10pm", 63f),
                        BarEntry("12am", 30f)
                    ),
                    barColor = GymColors.Gold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "Horario del gimnasio: 06:00 - 22:00",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
@Composable
private fun QrCard() {
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .drawBehind { drawCornerBrackets() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = "QR de acceso",
                tint = Color.Black,
                modifier = Modifier.size(136.dp)
            )
        }
    }
}

private fun DrawScope.drawCornerBrackets() {
    val i = 14.dp.toPx()
    val len = 34.dp.toPx()
    val r = 14.dp.toPx()
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)

    val corners = listOf(
        Path().apply {
            moveTo(i, i + len); lineTo(i, i + r)
            quadraticBezierTo(i, i, i + r, i); lineTo(i + len, i)
        },
        Path().apply {
            moveTo(w - i - len, i); lineTo(w - i - r, i)
            quadraticBezierTo(w - i, i, w - i, i + r); lineTo(w - i, i + len)
        },
        Path().apply {
            moveTo(i, h - i - len); lineTo(i, h - i - r)
            quadraticBezierTo(i, h - i, i + r, h - i); lineTo(i + len, h - i)
        },
        Path().apply {
            moveTo(w - i - len, h - i); lineTo(w - i - r, h - i)
            quadraticBezierTo(w - i, h - i, w - i, h - i - r); lineTo(w - i, h - i - len)
        }
    )
    corners.forEach { drawPath(it, color = Purple, style = stroke) }
}