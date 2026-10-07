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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.ui.theme.GymColors

data class BarEntry(val label: String, val value: Float)

@Composable
fun SimpleBarChart(
    title: String,
    entries: List<BarEntry>,
    barColor: Color = GymColors.Gold,
    barColors: List<Color>? = null,
    modifier: Modifier = Modifier,
    chartHeight: Int = 180
) {
    val borderBrush = Brush.linearGradient(colors = listOf(GymColors.Purple, GymColors.Gold))
    val maxValue = entries.maxOfOrNull { it.value } ?: 1f
    val niceMax = niceCeil(maxValue)
    val labels = axisLabels(maxValue)

    Column(
        modifier = modifier
            .background(GymColors.Surface, RoundedCornerShape(16.dp))
            .border(1.5.dp, borderBrush, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = GymColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
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
                    Text(text = label, color = GymColors.TextSecondary, fontSize = 10.sp)
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
                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .fillMaxHeight(fraction)
                            ) {
                                drawRoundRect(
                                    color = barColors?.getOrNull(index) ?: barColor,
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(GymColors.Purple)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(GymColors.Purple)
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
                            color = GymColors.TextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}