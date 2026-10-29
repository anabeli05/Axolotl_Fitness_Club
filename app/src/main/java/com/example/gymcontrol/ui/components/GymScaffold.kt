package com.example.gymcontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.data.model.UserRole
import kotlinx.coroutines.launch

// Borde del cuadro "¿Seguro que quieres salir?": degradado solo en tonos dorados
private val LogoutBorder = Brush.linearGradient(
    listOf(ClienteTema.Borde, ClienteTema.AzulSel)
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
            containerColor = ClienteTema.Fondo,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = com.example.gymcontrol.R.drawable.logo_final),
                                contentDescription = "Logo Axolotl",
                                modifier = Modifier
                                    .size(30.dp)
                                    .graphicsLayer(scaleX = 1.4f, scaleY = 1.4f)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(currentSection, color = ClienteTema.Texto, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        if (rolConSidebar != null) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "Menú", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ClienteTema.Navy)
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding)) { content() }
        }
    }

    if (rolConSidebar == null) {
        scaffold()
    } else {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ModalDrawerSheet(
                            modifier = Modifier.width(290.dp),
                            drawerContainerColor = ClienteTema.Navy,
                            drawerShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
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
                    }
                },
                content = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        scaffold()
                    }
                }
            )
        }
    }

    if (confirmarLogout) {
        Dialog(onDismissRequest = { confirmarLogout = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClienteTema.Beige, RoundedCornerShape(20.dp))
                    .border(1.5.dp, LogoutBorder, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = ClienteTema.AzulSel
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Cerrar sesión",
                    color = ClienteTema.Texto,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "¿Seguro que quieres salir?",
                    color = ClienteTema.TextoSec,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))

                // Los dos botones miden lo mismo y quedan centrados y a la misma altura
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { confirmarLogout = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ClienteTema.Borde),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                    ) { Text("Cancelar", fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { confirmarLogout = false; onLogout() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClienteTema.Rojo,
                            contentColor = Color.White
                        )
                    ) { Text("Salir", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}