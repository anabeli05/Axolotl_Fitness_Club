package com.example.gymcontrol.ui.encargado.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.data.model.Status
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.components.BarEntry
import com.example.gymcontrol.ui.components.GroupedBarChart
import com.example.gymcontrol.ui.components.GroupedBarEntry
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.SimpleBarChart
import com.example.gymcontrol.ui.components.StatCard
import com.example.gymcontrol.ui.components.StatIconType
import com.example.gymcontrol.ui.theme.GymColors

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit = {}) {
    val users = GymApp.repository.users()
    val active = users.count { it.role == UserRole.CLIENTE && it.status == Status.ACTIVO }
    val inactive = users.count { it.role == UserRole.CLIENTE && it.status == Status.INACTIVO }

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
                    value = "$active",
                    valueColor = GymColors.Green,
                    iconType = StatIconType.PERSON,
                    iconColor = GymColors.Green,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                StatCard(
                    title = "Clientes Inactivos",
                    value = "$inactive",
                    valueColor = GymColors.Red,
                    iconType = StatIconType.PERSON,
                    iconColor = GymColors.Red,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                StatCard(
                    title = "Ingresos del mes",
                    value = "$58,400",
                    valueColor = GymColors.Gold,
                    iconType = StatIconType.MONEY,
                    iconColor = GymColors.Gold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SimpleBarChart(
                    title = "Clientes en Tiempo Real",
                    entries = listOf(
                        BarEntry("6am", 25f), BarEntry("8am", 35f), BarEntry("10am", 30f),
                        BarEntry("12pm", 15f), BarEntry("2pm", 27f), BarEntry("4pm", 42f),
                        BarEntry("6pm", 47f), BarEntry("8pm", 55f), BarEntry("10pm", 63f),
                        BarEntry("12am", 30f)
                    ),
                    barColor = GymColors.Gold,
                    modifier = Modifier.fillMaxWidth()
                )
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
                    barColor = GymColors.Gold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}