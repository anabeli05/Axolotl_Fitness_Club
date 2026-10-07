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
import androidx.compose.runtime.*
import com.example.gymcontrol.data.remote.ReporteRow
import com.example.gymcontrol.data.remote.ReportesRepository
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")

@Composable
fun ReportesScreen(onNavigate: (String) -> Unit = {}) {
    val borderBrush = Brush.linearGradient(colors = listOf(GymColors.Purple, GymColors.Gold))

    var reportes by remember { mutableStateOf<List<ReporteRow>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            reportes = ReportesRepository.listar()
            errorMsg = null
        } catch (e: Exception) {
            errorMsg = "No se pudo cargar: ${e.message}"
        }
        cargando = false
    }

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

            if (cargando) {
                Text("Cargando...", color = GymColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
            }
            errorMsg?.let { msg ->
                Text(msg, color = GymColors.Red)
                Spacer(Modifier.height(8.dp))
            }

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

                    reportes.forEachIndexed { index, r ->
                        val total = r.servicios + r.asesorias - r.gastos
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index % 2 == 0) GymColors.Surface else GymColors.Background.copy(alpha = 0.4f)
                                )
                        ) {
                            BodyCell(ReportesRepository.nombrePeriodo(r.periodo), 130)
                            BodyCell("$${r.servicios}", 110)
                            BodyCell("$${r.asesorias}", 110)
                            BodyCell("$${r.gastos}", 110)
                            BodyCell("$$total", 110, highlight = true)
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