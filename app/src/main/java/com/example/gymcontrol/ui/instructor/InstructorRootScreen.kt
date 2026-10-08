package com.example.gymcontrol.ui.instructor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
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
import com.example.gymcontrol.ui.instructor.clientes.MisClientesScreen
import com.example.gymcontrol.ui.instructor.perfil.InstructorPerfilScreen
import com.example.gymcontrol.ui.theme.GymColors
import androidx.compose.runtime.getValue

private data class InstructorTabItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val instructorTabs = listOf(
    InstructorTabItem(Routes.INSTRUCTOR_CLIENTS, "Clientes", Icons.Filled.Group),
    InstructorTabItem(Routes.INSTRUCTOR_PROFILE, "Perfil", Icons.Filled.Person)
)

@Composable
fun InstructorRootScreen() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = GymColors.Background,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            NavigationBar(containerColor = GymColors.Surface) {
                instructorTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
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
                startDestination = Routes.INSTRUCTOR_CLIENTS
            ) {
                composable(Routes.INSTRUCTOR_CLIENTS) { MisClientesScreen() }
                composable(Routes.INSTRUCTOR_PROFILE) { InstructorPerfilScreen() }
            }
        }
    }
}