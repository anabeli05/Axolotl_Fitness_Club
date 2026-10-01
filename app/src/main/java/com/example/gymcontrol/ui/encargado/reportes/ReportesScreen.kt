package com.example.gymcontrol.ui.encargado.reportes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")

@Composable
fun ReportesScreen(onNavigate: (String) -> Unit = {}) {
    val borderBrush = Brush.linearGradient(colors = listOf(GymColors.Purple, GymColors.Gold))

    GymScaffold(
        currentSection = "Reportes",
        sections = ADMIN_SECTIONS,
        onNavigate = onNavigate
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Reporte mensual",
                color = GymColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GymColors.Surface, RoundedCornerShape(16.dp))
                    .border(1.5.dp, borderBrush, RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier
                            .background(GymColors.Background, RoundedCornerShape(10.dp))
                            .padding(vertical = 4.dp)
                    ) {
                        HeaderCell("Periodo", 130)
                        HeaderCell("Servicios", 110)
                        HeaderCell("Asesorías", 110)
                        HeaderCell("Gastos", 110)
                        HeaderCell("Total", 110)
                    }

                    GymApp.repository.reports().forEachIndexed { index, report ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index % 2 == 0) GymColors.Surface else GymColors.Background.copy(alpha = 0.4f)
                                )
                        ) {
                            BodyCell(report.period, 130)
                            BodyCell("$${report.services}", 110)
                            BodyCell("$${report.advisories}", 110)
                            BodyCell("$${report.expenses}", 110)
                            BodyCell("$${report.total}", 110, highlight = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Int) {
    Text(
        text = text,
        color = GymColors.Gold,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .width(width.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    )
}

@Composable
private fun BodyCell(text: String, width: Int, highlight: Boolean = false) {
    Text(
        text = text,
        color = if (highlight) GymColors.Gold else GymColors.TextPrimary,
        fontSize = 13.sp,
        fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .width(width.dp)
            .padding(horizontal = 10.dp, vertical = 12.dp)
    )
}