package com.example.gymcontrol.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.gymcontrol.R

data class GroupedBarEntry(val label: String, val activos: Float, val inactivos: Float)

// Versiones más llamativas de tu verde y tu rojo, para la barra seleccionada
private val VerdeBrillante = Color(0xFF3DFF7F)
private val RojoBrillante = Color(0xFFFF4D4D)

private fun textoValor(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)

@Composable
fun GroupedBarChart(
    title: String,
    entries: List<GroupedBarEntry>,
    modifier: Modifier = Modifier,
    chartHeight: Int = 180
) {
    val borderBrush = Brush.linearGradient(colors = listOf(ClienteTema.Borde, ClienteTema.AzulSel))
    val maxValue = entries.maxOfOrNull { maxOf(it.activos, it.inactivos) } ?: 1f
    val niceMax = niceCeil(maxValue)
    val labels = axisLabels(maxValue)

    // (índice del grupo, 0 = activos / 1 = inactivos)
    var seleccionado by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Column(
        modifier = modifier
            .background(ClienteTema.Beige, RoundedCornerShape(16.dp))
            .border(1.5.dp, borderBrush, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = ClienteTema.Texto,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(ClienteTema.Verde, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.chart_active), color = ClienteTema.TextoSec, fontSize = 11.sp)
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(ClienteTema.Rojo, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.chart_inactive), color = ClienteTema.TextoSec, fontSize = 11.sp)
                }
            }
        }

        // Un poco más de espacio arriba para que el número no tape el título
        Spacer(Modifier.height(28.dp))

        Row {
            Column(
                modifier = Modifier
                    .width(30.dp)
                    .height(chartHeight.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(text = label, color = ClienteTema.TextoSec, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        entries.forEachIndexed { index, entry ->
                            // (valor, color normal, color brillante al seleccionar)
                            val series = listOf(
                                Triple(entry.activos, ClienteTema.Verde, VerdeBrillante),
                                Triple(entry.inactivos, ClienteTema.Rojo, RojoBrillante)
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    // el grupo seleccionado se dibuja por encima de los demás
                                    .zIndex(if (seleccionado?.first == index) 1f else 0f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                series.forEachIndexed { serie, (valor, color, colorActivo) ->
                                    val fraction = (valor / niceMax).coerceIn(0.01f, 1f)
                                    val activa = seleccionado == (index to serie)

                                    if (serie == 1) Spacer(Modifier.width(3.dp))

                                    Box(
                                        modifier = Modifier
                                            .width(8.dp)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(fraction)
                                                .clickable {
                                                    seleccionado = if (activa) null else (index to serie)
                                                }
                                        ) {
                                            drawRoundRect(
                                                color = if (activa) colorActivo else color,
                                                cornerRadius = CornerRadius(4f, 4f)
                                            )
                                        }

                                        if (activa) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .offset(y = -(chartHeight.dp * fraction) - 3.dp)
                                                    .wrapContentWidth(unbounded = true)
                                                    .background(colorActivo, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = textoValor(valor),
                                                    color = Color(0xFF1A1A1A),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
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
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(ClienteTema.AzulSel)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(ClienteTema.AzulSel)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    entries.forEach { entry ->
                        Text(
                            text = entry.label,
                            color = ClienteTema.TextoSec,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}