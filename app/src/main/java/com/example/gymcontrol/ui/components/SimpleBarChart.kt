package com.example.gymcontrol.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.util.Locale

data class BarEntry(val label: String, val value: Float)

private val ColorDorado = Color(0xFFF2D675)

private fun textoValorDefault(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else "%.2f".format(Locale.US, v)

@Composable
fun SimpleBarChart(
    title: String,
    entries: List<BarEntry>,
    barColor: Color = ClienteTema.AzulSel,
    barColors: List<Color>? = null,
    modifier: Modifier = Modifier,
    chartHeight: Int = 180,
    valueFormatter: (Float) -> String = ::textoValorDefault
) {
    val borderBrush = Brush.linearGradient(colors = listOf(ClienteTema.Borde, ClienteTema.AzulSel))
    val maxValue = entries.maxOfOrNull { it.value } ?: 1f
    val niceMax = niceCeil(maxValue)
    val labels = axisLabels(maxValue)

    // Índice de la barra seleccionada (null = ninguna)
    var seleccionado by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .background(ClienteTema.Beige, RoundedCornerShape(16.dp))
            .border(1.5.dp, borderBrush, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = ClienteTema.Texto,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
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
                            val fraction = (entry.value / niceMax).coerceIn(0.01f, 1f)
                            val activa = seleccionado == index

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    // la barra seleccionada se dibuja por encima de las demás
                                    .zIndex(if (activa) 1f else 0f)
                                    // toda la columna es tocable, así es más fácil pegarle
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        seleccionado = if (activa) null else index
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                        .fillMaxHeight(fraction)
                                ) {
                                    drawRoundRect(
                                        color = if (activa) ColorDorado
                                        else barColors?.getOrNull(index) ?: barColor,
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )
                                }

                                if (activa) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .offset(y = -(chartHeight.dp * fraction) - 3.dp)
                                            .wrapContentWidth(unbounded = true)
                                            .background(ColorDorado, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = valueFormatter(entry.value),
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