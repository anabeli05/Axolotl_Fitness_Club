package com.example.gymcontrol.ui.cliente

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymcontrol.navigation.Routes
import com.example.gymcontrol.ui.cliente.asesorias.AsesoriasScreen
import com.example.gymcontrol.ui.cliente.home.ClienteHomeScreen
import com.example.gymcontrol.ui.cliente.perfil.ClientePerfilScreen
import com.example.gymcontrol.ui.theme.GymColors
import androidx.compose.runtime.getValue

private data class ClienteTabItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val clienteTabs = listOf(
    ClienteTabItem(Routes.CLIENT_HOME, "Inicio", Icons.Filled.Home),
    ClienteTabItem(Routes.CLIENT_ADVISORIES, "Asesorias", Icons.Filled.FitnessCenter),
    ClienteTabItem(Routes.CLIENT_PROFILE, "Perfil", Icons.Filled.Person)
)

@Composable
fun ClienteRootScreen() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = GymColors.Background,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            NavigationBar(containerColor = GymColors.Surface) {
                clienteTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            // Esto es lo que evita el "sobrepuesto": en vez de apilar
                            // una pantalla nueva encima, limpia hasta la raíz y
                            // restaura el estado de la pestaña si ya se había visitado.
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GymColors.Gold,
                            selectedTextColor = GymColors.Gold,
                            unselectedIconColor = GymColors.TextSecondary,
                            unselectedTextColor = GymColors.TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = Routes.CLIENT_HOME
            ) {
                composable(Routes.CLIENT_HOME) { ClienteHomeScreen() }
                composable(Routes.CLIENT_ADVISORIES) { AsesoriasScreen() }
                composable(Routes.CLIENT_PROFILE) { ClientePerfilScreen() }
            }
        }
    }
}