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
import com.example.gymcontrol.ui.components.LocalOnLogout
import com.example.gymcontrol.ui.components.SesionActual
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

    // Cerrar sesión: limpia el rol y regresa al login borrando todo el back stack
    val logout: () -> Unit = {
        SesionActual.cerrar()
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    CompositionLocalProvider(LocalOnLogout provides logout) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GymColors.Background)
        ) {
            NavHost(navController = navController, startDestination = Routes.LOGIN) {
                composable(Routes.LOGIN) {
                    LoginScreen { role ->
                        SesionActual.iniciar(role)
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

                // Cliente: la barra inferior cambia entre Inicio, Asesorías y Perfil
                composable(Routes.CLIENT_HOME) {
                    ClienteHomeScreen(onNavigate = { section -> navigateClient(navController, section) })
                }
                composable(Routes.CLIENT_ADVISORIES) {
                    AsesoriasScreen(onNavigate = { section -> navigateClient(navController, section) })
                }
                composable(Routes.CLIENT_PROFILE) {
                    ClientePerfilScreen(onNavigate = { section -> navigateClient(navController, section) })
                }

                // Recepción (con sidebar)
                composable(Routes.RECEPTION_SCANNER) {
                    QrScannerScreen(onNavigate = { section -> navigateReception(navController, section) })
                }
                composable(Routes.RECEPTION_CLIENTS) {
                    ClientesScreen(onNavigate = { section -> navigateReception(navController, section) })
                }
                composable(Routes.RECEPTION_REQUESTS) {
                    SolicitudesScreen(onNavigate = { section -> navigateReception(navController, section) })
                }

                // Instructor: la barra inferior cambia entre Mis clientes y Perfil
                composable(Routes.INSTRUCTOR_CLIENTS) {
                    MisClientesScreen(onNavigate = { section -> navigateInstructor(navController, section) })
                }
                composable(Routes.INSTRUCTOR_PROFILE) {
                    InstructorPerfilScreen(onNavigate = { section -> navigateInstructor(navController, section) })
                }

                // Encargado (con sidebar)
                composable(Routes.ADMIN_DASHBOARD) {
                    DashboardScreen(onNavigate = { section -> navigateAdmin(navController, section) })
                }
                composable(Routes.ADMIN_CLIENTS) {
                    ClientesScreen(onNavigate = { section -> navigateAdmin(navController, section) })
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
                composable(Routes.ADMIN_SERVICES) {
                    ServiciosScreen(onNavigate = { section -> navigateAdmin(navController, section) })
                }
                composable(Routes.ADMIN_EXPENSES) {
                    GastosScreen(onNavigate = { section -> navigateAdmin(navController, section) })
                }
                composable(Routes.ADMIN_REPORTS) {
                    ReportesScreen(onNavigate = { section -> navigateAdmin(navController, section) })
                }
            }

            // Menú dev movido al centro-derecha para no tapar la hamburguesa del sidebar
            //Box(
            //   modifier = Modifier
            //       .align(Alignment.CenterEnd)
            //       .padding(end = 4.dp)
            //) {
            //   RoleDevMenu(navController)
            //}
        }
    }
}

// Barra inferior del cliente
// popUpTo + saveState + restoreState evita que se apilen instancias repetidas
// de la misma pestaña (la causa de que las vistas se vieran sobrepuestas).
private fun navigateClient(navController: NavHostController, section: String) {
    val route = when (section) {
        "Inicio" -> Routes.CLIENT_HOME
        "Asesorías" -> Routes.CLIENT_ADVISORIES
        "Perfil" -> Routes.CLIENT_PROFILE
        else -> null
    }
    if (route != null) {
        navController.navigate(route) {
            popUpTo(Routes.CLIENT_HOME) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

// Sidebar de recepción
private fun navigateReception(navController: NavHostController, section: String) {
    val route = when (section) {
        "Home" -> Routes.RECEPTION_SCANNER
        "Clientes" -> Routes.RECEPTION_CLIENTS
        "Solicitudes" -> Routes.RECEPTION_REQUESTS
        else -> null
    }
    if (route != null) {
        navController.navigate(route) {
            popUpTo(Routes.RECEPTION_SCANNER) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

// Barra inferior del instructor
private fun navigateInstructor(navController: NavHostController, section: String) {
    val route = when (section) {
        "Mis clientes" -> Routes.INSTRUCTOR_CLIENTS
        "Perfil" -> Routes.INSTRUCTOR_PROFILE
        else -> null
    }
    if (route != null) {
        navController.navigate(route) {
            popUpTo(Routes.INSTRUCTOR_CLIENTS) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

// Sidebar del encargado
private fun navigateAdmin(navController: NavHostController, section: String) {
    val route = when (section) {
        "Dashboard" -> Routes.ADMIN_DASHBOARD
        "Clientes" -> Routes.ADMIN_CLIENTS
        "Personal", "Usuarios" -> Routes.ADMIN_USERS
        "Servicios" -> Routes.ADMIN_SERVICES
        "Gastos" -> Routes.ADMIN_EXPENSES
        "Reportes" -> Routes.ADMIN_REPORTS
        else -> null
    }
    if (route != null) {
        navController.navigate(route) {
            popUpTo(Routes.ADMIN_DASHBOARD) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
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

        // Cada entrada fija el rol para que el sidebar se comporte como con login real
        listOf(
            Triple("Cliente · Inicio", Routes.CLIENT_HOME, UserRole.CLIENTE),
            Triple("Cliente · Asesorías", Routes.CLIENT_ADVISORIES, UserRole.CLIENTE),
            Triple("Cliente · Perfil", Routes.CLIENT_PROFILE, UserRole.CLIENTE),
            Triple("Recepción · QR", Routes.RECEPTION_SCANNER, UserRole.RECEPCION),
            Triple("Recepción · Clientes", Routes.RECEPTION_CLIENTS, UserRole.RECEPCION),
            Triple("Recepción · Solicitudes", Routes.RECEPTION_REQUESTS, UserRole.RECEPCION),
            Triple("Instructor · Clientes", Routes.INSTRUCTOR_CLIENTS, UserRole.INSTRUCTOR),
            Triple("Instructor · Perfil", Routes.INSTRUCTOR_PROFILE, UserRole.INSTRUCTOR),
            Triple("Dueño · Dashboard", Routes.ADMIN_DASHBOARD, UserRole.ENCARGADO),
            Triple("Dueño · Clientes", Routes.ADMIN_CLIENTS, UserRole.ENCARGADO),
            Triple("Dueño · Usuarios", Routes.ADMIN_USERS, UserRole.ENCARGADO),
            Triple("Dueño · Servicios", Routes.ADMIN_SERVICES, UserRole.ENCARGADO),
            Triple("Dueño · Gastos", Routes.ADMIN_EXPENSES, UserRole.ENCARGADO),
            Triple("Dueño · Reportes", Routes.ADMIN_REPORTS, UserRole.ENCARGADO)
        ).forEach { (label, route, role) ->
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
                    SesionActual.iniciar(role)
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