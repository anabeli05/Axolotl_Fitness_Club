package com.example.gymcontrol.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.auth.LoginScreen
import com.example.gymcontrol.ui.cliente.asesorias.AsesoriasScreen
import com.example.gymcontrol.ui.cliente.home.ClienteHomeScreen
import com.example.gymcontrol.ui.cliente.perfil.ClientePerfilScreen
import com.example.gymcontrol.ui.encargado.clientes.ClientesScreen as AdminClientesScreen
import com.example.gymcontrol.ui.encargado.dashboard.DashboardScreen
import com.example.gymcontrol.ui.encargado.gastos.GastosScreen
import com.example.gymcontrol.ui.encargado.reportes.ReportesScreen
import com.example.gymcontrol.ui.encargado.servicios.ServiciosScreen
import com.example.gymcontrol.ui.encargado.usuarios.NuevoUsuarioScreen
import com.example.gymcontrol.ui.encargado.usuarios.UsuariosScreen
import com.example.gymcontrol.ui.instructor.clientes.MisClientesScreen
import com.example.gymcontrol.ui.instructor.perfil.InstructorPerfilScreen
import com.example.gymcontrol.ui.recepcion.clientes.ClientesScreen
import com.example.gymcontrol.ui.recepcion.scanner.QrScannerScreen
import com.example.gymcontrol.ui.recepcion.solicitudes.SolicitudesScreen
import com.example.gymcontrol.ui.theme.GymColors

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
                ClientesScreen()
            }
            composable(Routes.RECEPTION_REQUESTS) { SolicitudesScreen() }

            composable(Routes.INSTRUCTOR_CLIENTS) { MisClientesScreen() }
            composable(Routes.INSTRUCTOR_PROFILE) { InstructorPerfilScreen() }

            composable(Routes.ADMIN_DASHBOARD) {
                DashboardScreen(onNavigate = { section -> navigateAdmin(navController, section) })
            }
            composable(Routes.ADMIN_CLIENTS) {
                AdminClientesScreen(onNavigate = { section -> navigateAdmin(navController, section) })
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
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 16.dp)
        ) {
            RoleDevMenu(navController)
        }
    }
}

// Convierte el nombre de la sección del menú del encargado en su ruta
private fun navigateAdmin(navController: NavHostController, section: String) {
    val route = when (section) {
        "Dashboard" -> Routes.ADMIN_DASHBOARD
        "Clientes" -> Routes.ADMIN_CLIENTS
        "Personal" -> Routes.ADMIN_USERS
        "Servicios" -> Routes.ADMIN_SERVICES
        "Gastos" -> Routes.ADMIN_EXPENSES
        "Reportes" -> Routes.ADMIN_REPORTS
        else -> null
    }
    if (route != null) {
        navController.navigate(route) { launchSingleTop = true }
    }
}

@Composable
private fun RoleDevMenu(navController: NavHostController) {
    var expanded by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val borderBrush = Brush.linearGradient(colors = listOf(GymColors.Purple, GymColors.Gold))

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Filled.Menu,
            contentDescription = "Menú dev",
            tint = if (expanded) GymColors.Purple else GymColors.TextPrimary
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
            .background(GymColors.Surface, RoundedCornerShape(14.dp))
            .border(1.5.dp, borderBrush, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = com.example.gymcontrol.R.drawable.logo_axolotl),
                contentDescription = "Logo Axolotl",
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "AXOLOTL",
                    color = GymColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Navegación dev",
                    color = GymColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = GymColors.Border, thickness = 1.dp)

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
            "Dueño · Clientes" to Routes.ADMIN_CLIENTS,
            "Dueño · Usuarios" to Routes.ADMIN_USERS,
            "Dueño · Servicios" to Routes.ADMIN_SERVICES,
            "Dueño · Gastos" to Routes.ADMIN_EXPENSES,
            "Dueño · Reportes" to Routes.ADMIN_REPORTS
        ).forEach { (label, route) ->
            val isSelected = route == currentRoute
            DropdownMenuItem(
                text = {
                    Text(
                        text = label,
                        color = if (isSelected) GymColors.Purple else GymColors.TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    expanded = false
                    navController.navigate(route)
                },
                modifier = Modifier.background(
                    if (isSelected) GymColors.Purple.copy(alpha = 0.12f) else Color.Transparent
                ),
                leadingIcon = if (isSelected) {
                    {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(GymColors.Purple, RoundedCornerShape(2.dp))
                        )
                    }
                } else null
            )
        }
    }
}