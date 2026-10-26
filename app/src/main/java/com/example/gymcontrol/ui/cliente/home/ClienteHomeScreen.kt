package com.example.gymcontrol.ui.cliente.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.ui.components.ClienteScaffoldClaro
import com.example.gymcontrol.ui.components.ClienteTema
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Calendar

@Composable
fun ClienteHomeScreen(onNavigate: (String) -> Unit = {}) {
    val membership = "M-1001" // TODO: tomar la membresía del cliente en sesión
    val userName = "Ana López" // TODO: tomar el nombre del cliente en sesión

    ClienteScaffoldClaro(
        currentSection = "Inicio",
        onNavigate = onNavigate,
        logoRes = R.drawable.prueba
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item { WelcomeMessage(name = userName) }

            item {
                Text(
                    text = "Escanea código QR",
                    color = ClienteTema.Texto,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    QrCard(content = "AXOLOTL-GYM|ACCESO|$membership")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "QR de acceso · $membership",
                        color = ClienteTema.TextoSec,
                        fontSize = 13.sp
                    )
                }
            }

            item {
                BarrasClaro(
                    titulo = "Clientes ahorita",
                    datos = listOf(
                        "6am" to 25f, "8am" to 35f, "10am" to 30f, "12pm" to 15f,
                        "2pm" to 27f, "4pm" to 42f, "6pm" to 47f, "8pm" to 55f,
                        "10pm" to 63f, "12am" to 30f
                    )
                )
            }

            item {
                Text(
                    text = "Horario del gimnasio: 06:00 - 22:00",
                    color = ClienteTema.TextoSec,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun WelcomeMessage(name: String) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }
    val firstName = name.trim().split(" ").firstOrNull().orEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$greeting, $firstName",
            color = ClienteTema.Texto,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Qué bueno verte por aquí. ¡A darle con todo hoy!",
            color = ClienteTema.TextoSec,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun BarrasClaro(titulo: String, datos: List<Pair<String, Float>>) {
    val max = 100f
    val altoMax = 150.dp
    val marcas = listOf(0, 20, 40, 60, 80, 100)
    val lineaColor = Color(0xFFCCCCCC)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = titulo,
            color = ClienteTema.Texto,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(altoMax)
            ) {
                marcas.forEach { m ->
                    val fraccion = m / max
                    Text(
                        text = m.toString(),
                        color = ClienteTema.TextoSec,
                        fontSize = 10.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 6.dp)
                            .offset(y = altoMax * (1f - fraccion) - 7.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(altoMax)
                        .drawBehind {
                            marcas.forEach { m ->
                                val y = size.height * (1f - m / max)
                                drawLine(
                                    color = lineaColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            drawLine(
                                color = Color(0xFF888888),
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        datos.forEach { (_, valor) ->
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(altoMax * (valor / max))
                                    .background(
                                        ClienteTema.Barras,
                                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    datos.forEach { (etiqueta, _) ->
                        Box(modifier = Modifier.width(18.dp), contentAlignment = Alignment.Center) {
                            Text(
                                etiqueta,
                                color = ClienteTema.TextoSec,
                                fontSize = 8.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCard(content: String) {
    Box(
        modifier = Modifier
            .size(250.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ClienteTema.Beige)
            .drawBehind { drawCornerBrackets() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(184.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            QrCode(content = content, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun QrCode(content: String, modifier: Modifier = Modifier) {
    val matrix = remember(content) {
        val hints = mapOf(
            EncodeHintType.MARGIN to 0,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
    }

    Canvas(modifier = modifier) {
        val cell = size.width / matrix.width
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                if (matrix.get(x, y)) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x * cell, y * cell),
                        size = Size(cell + 0.5f, cell + 0.5f)
                    )
                }
            }
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
    corners.forEach { drawPath(it, color = ClienteTema.Borde, style = stroke) }
}