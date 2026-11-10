package com.example.gymcontrol.ui.encargado.reportes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.ReporteRow
import com.example.gymcontrol.data.remote.ReportesRepository
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.ClienteTema

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Pagos", "Gastos", "Reportes")

data class ReporteMensualItem(
    val periodo: String,
    val servicios: Double,
    val asesorias: Double,
    val gastos: Double,
    val total: Double
)

@Composable
fun ReportesScreen(onNavigate: (String) -> Unit = {}) {
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
                text = stringResource(R.string.title_monthly_report),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ClienteTema.Texto,
                fontSize = 22.sp
            )

            Spacer(Modifier.height(16.dp))

            if (cargando) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.common_loading), color = ClienteTema.TextoSec)
                }
                Spacer(Modifier.height(8.dp))
            }
            errorMsg?.let { msg ->
                Text(msg, color = ClienteTema.Rojo)
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(reportes) { r ->
                    val total = r.servicios + r.asesorias - r.gastos
                    MonthlyReportCard(
                        item = ReporteMensualItem(
                            periodo = ReportesRepository.nombrePeriodo(r.periodo),
                            servicios = r.servicios,
                            asesorias = r.asesorias,
                            gastos = r.gastos,
                            total = total
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyReportCard(item: ReporteMensualItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
        border = BorderStroke(1.5.dp, ClienteTema.Borde),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.periodo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ClienteTema.Texto
                )
                val totalColor = if (item.total >= 0) ClienteTema.Verde else ClienteTema.Rojo
                Text(
                    text = stringResource(R.string.label_total_format, item.total),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = totalColor
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = ClienteTema.Borde.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReportDetailItem(label = stringResource(R.string.label_services), value = item.servicios)
                ReportDetailItem(label = stringResource(R.string.label_advisories), value = item.asesorias)
                ReportDetailItem(label = stringResource(R.string.label_expenses), value = item.gastos)
            }
        }
    }
}

@Composable
fun ReportDetailItem(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = ClienteTema.TextoSec)
        Spacer(Modifier.height(2.dp))
        Text(text = "$${value}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ClienteTema.Texto)
    }
}
