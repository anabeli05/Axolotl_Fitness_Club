package com.example.gymcontrol.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GroupedBarEntry(val label: String, val activos: Float, val inactivos: Float)

@Composable
fun GroupedBarChart(
    title: String,
    entries: List<GroupedBarEntry>,
    modifier: Modifier = Modifier,
    chartHeight: Int = 180
) {
    val borderBrush = Brush.linearGradient(colors = listOf(ClienteTema.Borde, ClienteTema.AzulSel))
    val maxValue = entries.maxOf { maxOf(it.activos, it.inactivos) }
    val niceMax = niceCeil(maxValue)
    val labels = axisLabels(maxValue)

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
                    Text("Activos", color = ClienteTema.TextoSec, fontSize = 11.sp)
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(ClienteTema.Rojo, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text("Inactivos", color = ClienteTema.TextoSec, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

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
                        entries.forEach { entry ->
                            val fractionA = (entry.activos / niceMax).coerceIn(0.01f, 1f)
                            val fractionB = (entry.inactivos / niceMax).coerceIn(0.01f, 1f)
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .fillMaxHeight(fractionA)
                                ) {
                                    drawRoundRect(color = ClienteTema.Verde, cornerRadius = CornerRadius(4f, 4f))
                                }
                                Spacer(Modifier.width(3.dp))
                                Canvas(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .fillMaxHeight(fractionB)
                                ) {
                                    drawRoundRect(color = ClienteTema.Rojo, cornerRadius = CornerRadius(4f, 4f))
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