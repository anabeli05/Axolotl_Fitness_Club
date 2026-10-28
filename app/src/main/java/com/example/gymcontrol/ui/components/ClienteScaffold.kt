package com.example.gymcontrol.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter // requiere material-icons-extended
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.ui.theme.GymColors

data class ScaffoldTab(val label: String, val icon: ImageVector)

private val CLIENTE_TABS = listOf(
    ScaffoldTab("Inicio", Icons.Filled.Home),
    ScaffoldTab("Asesorías", Icons.Filled.FitnessCenter),
    ScaffoldTab("Perfil", Icons.Filled.Person)
)

// Cambia este color por el fondo que uses en el resto de la app
private val PageBackground = Color.Black

@Composable
fun ClienteScaffold(
    currentSection: String,
    onNavigate: (String) -> Unit,
    @DrawableRes logoRes: Int,
    tabs: List<ScaffoldTab> = CLIENTE_TABS,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        // Encabezado: logo centrado + botón de cerrar sesión a la derecha + línea dorada
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(logoRes),
                    contentDescription = "Axolotl Fitness Club",
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .height(95.dp)
                )
                BotonCerrarSesion(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GymColors.Gold.copy(alpha = 0.7f))
            )
        }

        // Contenido de la pantalla
        Box(modifier = Modifier.weight(1f)) {
            content()
        }

        // Barra de navegación inferior
        HorizontalDividerLine()
        NavigationBar(
            containerColor = PageBackground,
            tonalElevation = 0.dp
        ) {
            tabs.forEach { tab ->
                val selected = tab.label == currentSection
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(tab.label) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label
                        )
                    },
                    label = { Text(tab.label, fontSize = 13.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GymColors.Purple,
                        selectedTextColor = GymColors.Purple,
                        unselectedIconColor = GymColors.TextSecondary,
                        unselectedTextColor = GymColors.TextSecondary,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
private fun HorizontalDividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GymColors.Border)
    )
}