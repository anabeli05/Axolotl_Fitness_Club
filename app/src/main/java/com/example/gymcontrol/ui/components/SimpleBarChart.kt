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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

data class BarEntry(val label: String, val value: Float)

// Barra/etiqueta seleccionada: dorado claro en oscuro, un dorado más profundo en claro
private val ColorDorado
    get() = if (ClienteTema.oscuro) Color(0xFFF2D675) else Color(0xFFD4A82A)

private fun textoValorDefault(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else "%.2f".format(Locale.US, v)

/**
 * Paso del eje Y a partir de un paso mínimo deseado (ej. 5).
 * Si con ese paso saldrían demasiadas líneas, sube al siguiente paso "redondo"
 * (5, 10, 20, 25, 50, 100, 200, 250, 500...) hasta que queden [maxLineas] o menos.
 */
private fun pasoEje(maxValue: Float, pasoMinimo: Float, maxLineas: Int = 5): Float {
    val base = floatArrayOf(1f, 2f, 2.5f, 5f)
    for (n in -2..8) {
        val escala = 10f.pow(n)
        for (b in base) {
            val paso = b * escala
            if (paso >= pasoMinimo && ceil(maxValue / paso) <= maxLineas) return paso
        }
    }
    return pasoMinimo
}

@Composable
fun SimpleBarChart(
    title: String,
    entries: List<BarEntry>,
    barColor: Color = ClienteTema.AzulSel,
    barColors: List<Color>? = null,
    modifier: Modifier = Modifier,
    chartHeight: Int = 180,
    // Si se indica (ej. 5f), el eje Y sube de ese paso en ese paso (se ajusta solo si hay demasiadas líneas).
    // Si es null, se usa la escala automática de siempre.
    yStep: Float? = null,
    // Si se indica junto con yStep (ej. 35f), el eje queda FIJO: 0, 5, 10... hasta ese valor,
    // sin cambiar el paso. Solo crece si algún dato lo supera.
    yMax: Float? = null,
    valueFormatter: (Float) -> String = ::textoValorDefault
) {
    val borderBrush = Brush.linearGradient(colors = listOf(ClienteTema.Borde, ClienteTema.AzulSel))
    val maxValue = entries.maxOfOrNull { it.value } ?: 1f

    // Con yMax el paso no se mueve; sin yMax se ajusta solo si hay demasiadas líneas
    val paso = yStep?.let { if (yMax != null) it else pasoEje(maxValue, it) }
    val niceMax: Float
    val labels: List<String>
    if (paso != null) {
        val tope = max(maxValue, yMax ?: 0f)
        val lineas = ceil(tope / paso).toInt().coerceAtLeast(1)
        niceMax = paso * lineas
        // De arriba hacia abajo, igual que el eje automático
        labels = (lineas downTo 0).map { textoValorDefault(it * paso) }
    } else {
        niceMax = niceCeil(maxValue)
        labels = axisLabels(maxValue)
    }

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
            // Cada etiqueta se coloca a la altura exacta de su valor (centrada en su línea),
            // así coincide con las barras aunque cambie el tamaño de la fuente.
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(chartHeight.dp)
            ) {
                val tramos = (labels.size - 1).coerceAtLeast(1)
                labels.forEachIndexed { i, label ->
                    // i = 0 es la etiqueta de arriba (valor máximo); la última es el 0
                    val y = chartHeight.dp * (i.toFloat() / tramos)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = y)
                            .height(0.dp)
                    ) {
                        Text(
                            text = label,
                            color = ClienteTema.TextoSec,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.wrapContentHeight(
                                align = Alignment.CenterVertically,
                                unbounded = true
                            )
                        )
                    }
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