package com.example.gymcontrol.ui.encargado.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.ClientesMesRow
import com.example.gymcontrol.data.remote.IngresoDiaRow
import com.example.gymcontrol.data.remote.AccesoFacialRepository
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

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Pagos", "Gastos", "Reportes")

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit = {}) {
    var ocupacion by remember { mutableStateOf<List<OcupacionRow>>(emptyList()) }
    var resumen by remember { mutableStateOf<ResumenRow?>(null) }
    var clientesMes by remember { mutableStateOf<List<ClientesMesRow>>(emptyList()) }
    var ingresosDia by remember { mutableStateOf<List<IngresoDiaRow>>(emptyList()) }
    var dentro by remember { mutableStateOf<Int?>(null) }

    // Personas dentro ahora (entradas − salidas de hoy), cada 5 segundos
    LaunchedEffect(Unit) {
        while (true) {
            try { dentro = AccesoFacialRepository.ocupacionActual() } catch (_: Exception) { }
            delay(5_000)
        }
    }

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
            try {
                clientesMes = ResumenRepository.clientesPorMes()
            } catch (e: Exception) {
                // si falla, se conserva lo último que se cargó
            }
            try {
                ingresosDia = ResumenRepository.ingresosSemana()
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
    val pico = datos.maxByOrNull { it.second }?.takeIf { it.second > 0 }

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
                    title = stringResource(R.string.encargado_stat_people_inside_now),
                    value = dentro?.toString() ?: "—",
                    valueColor = ClienteTema.AzulSel,
                    iconType = StatIconType.PERSON,
                    iconColor = ClienteTema.AzulSel,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                StatCard(
                    title = stringResource(R.string.encargado_stat_active_clients),
                    value = "$activos",
                    valueColor = ClienteTema.Verde,
                    iconType = StatIconType.PERSON,
                    iconColor = ClienteTema.Verde,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                StatCard(
                    title = stringResource(R.string.encargado_stat_inactive_clients),
                    value = "$inactivos",
                    valueColor = ClienteTema.Rojo,
                    iconType = StatIconType.PERSON,
                    iconColor = ClienteTema.Rojo,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                StatCard(
                    title = stringResource(R.string.encargado_stat_month_income),
                    value = ingresosTexto,
                    valueColor = ClienteTema.AzulSel,
                    iconType = StatIconType.MONEY,
                    iconColor = ClienteTema.AzulSel,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SimpleBarChart(
                    title = stringResource(R.string.encargado_chart_hourly_occupancy),
                    entries = datos.map { BarEntry(it.first.take(2), it.second.toFloat()) },
                    barColor = ClienteTema.AzulSel,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (pico != null) {
                item {
                    Text(
                        text = stringResource(
                            R.string.peak_hour_today,
                            pico.first,
                            pico.second,
                            datos.sumOf { it.second }
                        ),
                        color = ClienteTema.TextoSec,
                        fontSize = 13.sp
                    )
                }
            }

            item {
                GroupedBarChart(
                    title = stringResource(R.string.encargado_chart_clients_status),
                    entries = clientesMes.map {
                        GroupedBarEntry(
                            mesCortoStr(it.periodo),
                            it.activos.toFloat(),
                            it.inactivos.toFloat()
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SimpleBarChart(
                    title = stringResource(R.string.encargado_chart_income_per_day),
                    // El domingo solo aparece si ese día hubo cobros.
                    // El total llega 100 veces más grande, por eso se divide entre 100.
                    entries = ingresosDia
                        .filter { it.diaSemana != 7 || it.total > 0.0 }
                        .map {
                            BarEntry(
                                diaCortoStr(it.diaSemana),
                                (it.total / 100.0).toFloat()
                            )
                        },
                    barColor = ClienteTema.AzulSel,
                    // Eje fijo de 5 en 5: 0, 5, 10... hasta 30 (solo crece si un dato lo supera)
                    yStep = 5f,
                    yMax = 30f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun mesCortoStr(periodo: String): String {
    val month = periodo.split("-").getOrNull(1)?.toIntOrNull() ?: 1
    return when (month) {
        1 -> stringResource(R.string.month_jan)
        2 -> stringResource(R.string.month_feb)
        3 -> stringResource(R.string.month_mar)
        4 -> stringResource(R.string.month_apr)
        5 -> stringResource(R.string.month_may)
        6 -> stringResource(R.string.month_jun)
        7 -> stringResource(R.string.month_jul)
        8 -> stringResource(R.string.month_aug)
        9 -> stringResource(R.string.month_sep)
        10 -> stringResource(R.string.month_oct)
        11 -> stringResource(R.string.month_nov)
        12 -> stringResource(R.string.month_dec)
        else -> periodo
    }
}

@Composable
private fun diaCortoStr(diaSemana: Int): String {
    return when (diaSemana) {
        1 -> stringResource(R.string.day_mon)
        2 -> stringResource(R.string.day_tue)
        3 -> stringResource(R.string.day_wed)
        4 -> stringResource(R.string.day_thu)
        5 -> stringResource(R.string.day_fri)
        6 -> stringResource(R.string.day_sat)
        7 -> stringResource(R.string.day_sun)
        else -> ""
    }
}