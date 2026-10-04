package com.example.gymcontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.theme.GymColors
import kotlinx.coroutines.launch

private val LogoutBorder = Brush.linearGradient(
    listOf(GymColors.Purple, GymColors.Gold, GymColors.Red)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymScaffold(
    currentSection: String,
    sections: List<String> = emptyList(),   // se ignora; evita editar las pantallas
    onNavigate: (String) -> Unit,
    rol: UserRole? = SesionActual.rol,
    onLogout: () -> Unit = LocalOnLogout.current,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var confirmarLogout by remember { mutableStateOf(false) }

    val rolConSidebar = rol?.takeIf { it.tieneSidebar() }

    val scaffold: @Composable () -> Unit = {
        Scaffold(
            containerColor = GymColors.Background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = com.example.gymcontrol.R.drawable.logo_axolotl),
                                contentDescription = "Logo Axolotl",
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(currentSection, color = GymColors.TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        if (rolConSidebar != null) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "Menú", tint = GymColors.TextPrimary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GymColors.Background)
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding)) { content() }
        }
    }

    if (rolConSidebar == null) {
        scaffold()
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(290.dp),
                    drawerContainerColor = GymColors.Surface,
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                ) {
                    GymSidebar(
                        secciones = seccionesPara(rolConSidebar),
                        titulo = rolConSidebar.etiqueta(),
                        currentSection = currentSection,
                        onNavigate = {
                            onNavigate(it)
                            scope.launch { drawerState.close() }
                        },
                        onLogout = {
                            scope.launch { drawerState.close() }
                            confirmarLogout = true
                        }
                    )
                }
            },
            content = scaffold
        )
    }

    if (confirmarLogout) {
        AlertDialog(
            onDismissRequest = { confirmarLogout = false },
            modifier = Modifier.border(1.5.dp, LogoutBorder, RoundedCornerShape(20.dp)),
            containerColor = GymColors.Surface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = GymColors.Gold
                )
            },
            title = {
                Text("Cerrar sesión", color = GymColors.TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("¿Seguro que quieres salir?", color = GymColors.TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = { confirmarLogout = false; onLogout() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymColors.Red,
                        contentColor = Color.White
                    )
                ) { Text("Salir", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { confirmarLogout = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GymColors.Purple),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.TextPrimary)
                ) { Text("Cancelar") }
            }
        )
    }
}