package com.example.gymcontrol.ui.cliente.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.gymcontrol.ui.components.SesionActual
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.AccesoFacialRepository
import com.example.gymcontrol.data.remote.OcupacionRepository
import com.example.gymcontrol.data.remote.OcupacionRow
import com.example.gymcontrol.ui.components.ClienteScaffoldClaro
import com.example.gymcontrol.ui.components.ClienteTema
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun ClienteHomeScreen(onNavigate: (String) -> Unit = {}) {
    // El nombre del cliente en sesión ya viene del login
    val userName = SesionActual.nombre ?: ""

    // Personas dentro del gimnasio ahora (entradas − salidas de hoy); se actualiza cada 5 s
    var personasDentro by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            try { personasDentro = AccesoFacialRepository.ocupacionActual() } catch (_: Exception) { }
            delay(5_000)
        }
    }

    // Entradas de hoy por hora (vista ocupacion_por_hora); se actualiza cada 30 s
    var ocupacion by remember { mutableStateOf<List<OcupacionRow>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            try { ocupacion = OcupacionRepository.hoy() } catch (_: Exception) { }
            delay(30_000)
        }
    }

    // Bloques de 2 horas de 6am a 12am, con 0 donde no hubo entradas
    val conteo = ocupacion.associate { it.hora to it.personas }
    val datosHora = (6..22 step 2).map { h ->
        val etiqueta = when {
            h < 12 -> "${h}am"
            h == 12 -> "12pm"
            else -> "${h - 12}pm"
        }
        val total = (conteo["%02d:00".format(h)] ?: 0) + (conteo["%02d:00".format(h + 1)] ?: 0)
        etiqueta to total.toFloat()
    }

    ClienteScaffoldClaro(
        currentSection = "Inicio",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_final
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
                    text = stringResource(R.string.cliente_facial_access_title),
                    color = ClienteTema.Texto,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AccesoFacialCard(personasDentro = personasDentro)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.cliente_facial_camera_hint),
                        color = ClienteTema.TextoSec,
                        fontSize = 13.sp
                    )
                }
            }

            item {
                BarrasClaro(
                    titulo = stringResource(R.string.cliente_clients_now_title),
                    datos = datosHora
                )
            }

            item {
                Text(
                    text = stringResource(R.string.cliente_gym_schedule),
                    color = ClienteTema.TextoSec,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun WelcomeMessage(name: String) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> stringResource(R.string.cliente_greeting_morning)
        in 12..18 -> stringResource(R.string.cliente_greeting_afternoon)
        else -> stringResource(R.string.cliente_greeting_evening)
    }
    val firstName = name.trim().split(" ").firstOrNull().orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$greeting, $firstName",
            color = ClienteTema.Texto,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.cliente_welcome_subtitle),
            color = ClienteTema.TextoSec,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BarrasClaro(titulo: String, datos: List<Pair<String, Float>>) {
    // Escala según el valor más alto (múltiplo de 5, mínimo 5)
    val max = ceil(maxOf(datos.maxOfOrNull { it.second } ?: 0f, 5f) / 5f) * 5f
    val altoMax = 150.dp
    val marcas = (0..5).map { (max * it / 5).roundToInt() }
    var seleccionado by remember { mutableStateOf<Int?>(null) }

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
            // Eje Y (números)
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
                // Área de barras (sin líneas horizontales, solo el eje vertical)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(altoMax)
                        .drawBehind {
                            drawLine(
                                color = Color(0xFF888888),
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            // Línea base (eje X)
                            drawLine(
                                color = Color(0xFF888888),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        },
                    verticalAlignment = Alignment.Bottom
                ) {
                    datos.forEachIndexed { index, (_, valor) ->
                        val activo = seleccionado == index
                        val colorBarra by animateColorAsState(
                            targetValue = if (activo) Color(0xFFF2D675) else ClienteTema.Barras,
                            label = "colorBarra"
                        )
                        val anchoBarra by animateDpAsState(
                            targetValue = if (activo) 22.dp else 18.dp,
                            label = "anchoBarra"
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (activo) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color(0xFFF2D675),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = valor.toInt().toString(),
                                        color = Color(0xFF1A1A1A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .width(anchoBarra)
                                    .height(altoMax * (valor / max))
                                    .background(
                                        colorBarra,
                                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                    )
                                    .clickable {
                                        seleccionado = if (activo) null else index
                                    }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Etiquetas de hora: cada una usa el mismo ancho que su barra
                Row(modifier = Modifier.fillMaxWidth()) {
                    datos.forEach { (etiqueta, _) ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = etiqueta,
                                color = ClienteTema.TextoSec,
                                fontSize = 9.sp,
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
private fun AccesoFacialCard(personasDentro: Int?) {
    Box(
        modifier = Modifier
            .size(250.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ClienteTema.Beige)
            .drawBehind { drawCornerBrackets() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Face,
                contentDescription = null,
                tint = ClienteTema.AzulSel,
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = personasDentro?.toString() ?: "—",
                color = ClienteTema.Texto,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (personasDentro == 1) stringResource(R.string.cliente_person_inside_now) else stringResource(R.string.cliente_people_inside_now),
                color = ClienteTema.TextoSec,
                fontSize = 13.sp
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
    corners.forEach { drawPath(it, color = ClienteTema.Borde, style = stroke) }
}