package com.example.gymcontrol.ui.encargado.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.data.remote.OcupacionRepository
import com.example.gymcontrol.data.remote.OcupacionRow
import com.example.gymcontrol.data.remote.ResumenRepository
import com.example.gymcontrol.data.remote.ResumenRow
import com.example.gymcontrol.ui.components.BarEntry
import com.example.gymcontrol.ui.components.GroupedBarChart
import com.example.gymcontrol.ui.components.GroupedBarEntry
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.SimpleBarChart
import com.example.gymcontrol.ui.components.StatCard
import com.example.gymcontrol.ui.components.StatIconType
import com.example.gymcontrol.ui.components.ClienteTema
import kotlinx.coroutines.delay
import java.util.Locale

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit = {}) {
    var ocupacion by remember { mutableStateOf<List<OcupacionRow>>(emptyList()) }
    var resumen by remember { mutableStateOf<ResumenRow?>(null) }

    // Se actualiza cada 30 segundos
    LaunchedEffect(Unit) {
        while (true) {
            try {
                ocupacion = OcupacionRepository.hoy()
            } catch (e: Exception) {
                // si falla, se conserva lo último que se cargó
            }
            try {
                resumen = ResumenRepository.obtener()
            } catch (e: Exception) {
                // si falla, se conserva lo último que se cargó
            }
            delay(30_000)
        }
    }

    val activos = resumen?.activos ?: 0
    val inactivos = resumen?.inactivos ?: 0
    val ingresosTexto = "$" + "%,.2f".format(Locale.US, resumen?.ingresosMes ?: 0.0)

    // Todas las horas del gimnasio (6:00 a 21:00), con 0 donde no hubo entradas
    val conteo = ocupacion.associate { it.hora to it.personas }
    val datos = (6..21).map { h ->
        val hora = "%02d:00".format(h)
        hora to (conteo[hora] ?: 0)
    }
    val maximo = datos.maxOfOrNull { it.second } ?: 0
    val pico = datos.maxByOrNull { it.second }?.takeIf { it.second > 0 }
    val colores = datos.map { (_, n) ->
        when {
            maximo == 0 -> ClienteTema.AzulSel
            n * 3 >= maximo * 2 -> ClienteTema.Rojo
            n * 3 >= maximo -> ClienteTema.AzulSel
            else -> ClienteTema.Verde
        }
    }

    GymScaffold(
        currentSection = "Dashboard",
        sections = ADMIN_SECTIONS,
        onNavigate = onNavigate
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                StatCard(
                    title = "Clientes Activos",
                    value = "$activos",
                    valueColor = ClienteTema.Verde,
                    iconType = StatIconType.PERSON,
                    iconColor = ClienteTema.Verde,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                StatCard(
                    title = "Clientes Inactivos",
                    value = "$inactivos",
                    valueColor = ClienteTema.Rojo,
                    iconType = StatIconType.PERSON,
                    iconColor = ClienteTema.Rojo,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                StatCard(
                    title = "Ingresos del mes",
                    value = ingresosTexto,
                    valueColor = ClienteTema.AzulSel,
                    iconType = StatIconType.MONEY,
                    iconColor = ClienteTema.AzulSel,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SimpleBarChart(
                    title = "Personas por hora (hoy)",
                    entries = datos.map { BarEntry(it.first.take(2), it.second.toFloat()) },
                    barColors = colores,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (pico != null) {
                item {
                    Text(
                        text = "Hora pico: ${pico.first} (${pico.second} personas). Entradas de hoy: ${datos.sumOf { it.second }}",
                        color = ClienteTema.TextoSec,
                        fontSize = 13.sp
                    )
                }
            }

            item {
                GroupedBarChart(
                    title = "Clientes Activos/Inactivos",
                    entries = listOf(
                        GroupedBarEntry("Ene", 830f, 330f),
                        GroupedBarEntry("Feb", 540f, 570f),
                        GroupedBarEntry("Mar", 370f, 160f),
                        GroupedBarEntry("Abr", 290f, 350f),
                        GroupedBarEntry("May", 520f, 400f),
                        GroupedBarEntry("Jun", 660f, 390f),
                        GroupedBarEntry("Jul", 270f, 520f),
                        GroupedBarEntry("Ago", 370f, 240f),
                        GroupedBarEntry("Sep", 420f, 790f),
                        GroupedBarEntry("Oct", 570f, 150f),
                        GroupedBarEntry("Nov", 330f, 60f),
                        GroupedBarEntry("Dic", 150f, 250f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SimpleBarChart(
                    title = "Ingresos por Día",
                    entries = listOf(
                        BarEntry("Lun", 210f), BarEntry("Mar", 270f), BarEntry("Mié", 155f),
                        BarEntry("Jue", 310f), BarEntry("Vie", 185f), BarEntry("Sáb", 345f)
                    ),
                    barColor = ClienteTema.AzulSel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}