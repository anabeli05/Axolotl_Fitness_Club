package com.example.gymcontrol.ui.encargado.reportes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun ReportesScreen() {
    ScreenContainer("Reporte mensual") {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row {
                HeaderCell("Periodo", 130)
                HeaderCell("Servicios", 110)
                HeaderCell("Asesorías", 110)
                HeaderCell("Gastos", 110)
                HeaderCell("Total", 110)
            }
            GymApp.repository.reports().forEach { report ->
                Row {
                    BodyCell(report.period, 130)
                    BodyCell("$${report.services}", 110)
                    BodyCell("$${report.advisories}", 110)
                    BodyCell("$${report.expenses}", 110)
                    BodyCell("$${report.total}", 110)
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Int) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.width(width.dp)) {
        Text(text, Modifier.padding(10.dp))
    }
}

@Composable
private fun BodyCell(text: String, width: Int) {
    Text(text, Modifier.width(width.dp).padding(10.dp))
}
