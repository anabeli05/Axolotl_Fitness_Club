package com.example.gymcontrol.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.auth.LoginScreen
import com.example.gymcontrol.ui.cliente.asesorias.AsesoriasScreen
import com.example.gymcontrol.ui.cliente.home.ClienteHomeScreen
import com.example.gymcontrol.ui.cliente.perfil.ClientePerfilScreen
import com.example.gymcontrol.ui.encargado.dashboard.DashboardScreen
import com.example.gymcontrol.ui.encargado.gastos.GastosScreen
import com.example.gymcontrol.ui.encargado.reportes.ReportesScreen
import com.example.gymcontrol.ui.encargado.servicios.ServiciosScreen
import com.example.gymcontrol.ui.encargado.usuarios.NuevoUsuarioScreen
import com.example.gymcontrol.ui.encargado.usuarios.UsuariosScreen
import com.example.gymcontrol.ui.instructor.clientes.MisClientesScreen
import com.example.gymcontrol.ui.instructor.perfil.InstructorPerfilScreen
import com.example.gymcontrol.ui.recepcion.clientes.ClientesScreen
import com.example.gymcontrol.ui.recepcion.clientes.NuevoClienteScreen
import com.example.gymcontrol.ui.recepcion.scanner.QrScannerScreen
import com.example.gymcontrol.ui.recepcion.solicitudes.SolicitudesScreen

@Composable
fun GymNavGraph() {
    val navController = rememberNavController()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(navController = navController, startDestination = Routes.LOGIN) {
            composable(Routes.LOGIN) {
                LoginScreen { role ->
                    val target = when (role) {
                        UserRole.CLIENTE -> Routes.CLIENT_HOME
                        UserRole.RECEPCION -> Routes.RECEPTION_SCANNER
                        UserRole.INSTRUCTOR -> Routes.INSTRUCTOR_CLIENTS
                        UserRole.ENCARGADO -> Routes.ADMIN_DASHBOARD
                    }
                    navController.navigate(target) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            }

            composable(Routes.CLIENT_HOME) { ClienteHomeScreen() }
            composable(Routes.CLIENT_ADVISORIES) { AsesoriasScreen() }
            composable(Routes.CLIENT_PROFILE) { ClientePerfilScreen() }

            composable(Routes.RECEPTION_SCANNER) { QrScannerScreen() }
            composable(Routes.RECEPTION_CLIENTS) {
                ClientesScreen { navController.navigate(Routes.RECEPTION_NEW_CLIENT) }
            }
            composable(Routes.RECEPTION_NEW_CLIENT) {
                NuevoClienteScreen { navController.popBackStack() }
            }
            composable(Routes.RECEPTION_REQUESTS) { SolicitudesScreen() }

            composable(Routes.INSTRUCTOR_CLIENTS) { MisClientesScreen() }
            composable(Routes.INSTRUCTOR_PROFILE) { InstructorPerfilScreen() }

            composable(Routes.ADMIN_DASHBOARD) {
                DashboardScreen(onNavigate = { section -> navigateAdmin(navController, section) })
            }
            composable(Routes.ADMIN_USERS) {
                UsuariosScreen(
                    onNewUser = { navController.navigate(Routes.ADMIN_NEW_USER) },
                    onNavigate = { section -> navigateAdmin(navController, section) }
                )
            }
            composable(Routes.ADMIN_NEW_USER) {
                NuevoUsuarioScreen { navController.popBackStack() }
            }
            composable(Routes.ADMIN_SERVICES) { ServiciosScreen() }
            composable(Routes.ADMIN_EXPENSES) { GastosScreen() }
            composable(Routes.ADMIN_REPORTS) { ReportesScreen() }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            RoleDevMenu(navController)
        }
    }
}

// Convierte el nombre de la sección del menú del encargado en su ruta
private fun navigateAdmin(navController: NavHostController, section: String) {
    val route = when (section) {
        "Dashboard" -> Routes.ADMIN_DASHBOARD
        "Personal" -> Routes.ADMIN_USERS
        "Servicios" -> Routes.ADMIN_SERVICES
        "Gastos" -> Routes.ADMIN_EXPENSES
        "Reportes" -> Routes.ADMIN_REPORTS
        else -> null // "Clientes" aún no tiene ruta de encargado
    }
    if (route != null) {
        navController.navigate(route) { launchSingleTop = true }
    }
}

@Composable
private fun RoleDevMenu(navController: NavHostController) {
    var expanded by remember { mutableStateOf(false) }

    FloatingActionButton(
        onClick = { expanded = true }
    ) { Text("☰") }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        listOf(
            "Cliente · Inicio" to Routes.CLIENT_HOME,
            "Cliente · Asesorías" to Routes.CLIENT_ADVISORIES,
            "Cliente · Perfil" to Routes.CLIENT_PROFILE,
            "Recepción · QR" to Routes.RECEPTION_SCANNER,
            "Recepción · Clientes" to Routes.RECEPTION_CLIENTS,
            "Recepción · Solicitudes" to Routes.RECEPTION_REQUESTS,
            "Instructor · Clientes" to Routes.INSTRUCTOR_CLIENTS,
            "Instructor · Perfil" to Routes.INSTRUCTOR_PROFILE,
            "Dueño · Dashboard" to Routes.ADMIN_DASHBOARD,
            "Dueño · Usuarios" to Routes.ADMIN_USERS,
            "Dueño · Servicios" to Routes.ADMIN_SERVICES,
            "Dueño · Gastos" to Routes.ADMIN_EXPENSES,
            "Dueño · Reportes" to Routes.ADMIN_REPORTS
        ).forEach { (label, route) ->
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    expanded = false
                    navController.navigate(route)
                }
            )
        }
    }
}