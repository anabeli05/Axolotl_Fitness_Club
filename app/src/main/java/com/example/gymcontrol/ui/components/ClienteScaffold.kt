package com.example.gymcontrol.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.ui.theme.GymColors

/** Paleta unisex del cliente: grafito + violeta + dorado suave. */
object ClienteTema {
    val Fondo = Color(0xFF222223)
    val Navy = Color(0xFF111111)       // encabezado y barra inferior (grafito)
    val Beige = Color(0xFF3D3D3F)      // tarjetas (gris lavanda muy claro)
    val Azul = Color(0xFF8E723A)       // botones (violeta)
    val AzulClaro = Color(0xFFC1AA60)  // "Pendiente"
    val AzulSel = Color(0xFFAC933D)    // pestaña seleccionada (dorado)
    val Barras = Color(0xFFAF9A54)     // barras de la gráfica
    val Texto = Color(0xFFEEEEF3)
    val TextoSec = Color(0xFFE3E3EC)
    val Borde = Color(0xFF93834C)
    val Verde = Color(0xFF2E8B57)
    val Rojo = Color(0xFFD64545)
}

data class ScaffoldTab(val label: String, val icon: ImageVector)

private val TABS_CLARO = listOf(
    ScaffoldTab("Inicio", Icons.Filled.Home),
    ScaffoldTab("Asesorías", Icons.Filled.FitnessCenter),
    ScaffoldTab("Pagos", Icons.Filled.CreditCard),
    ScaffoldTab("Perfil", Icons.Filled.Person)
)

private val LogoutBorderClaro = Brush.linearGradient(
    listOf(ClienteTema.Azul, ClienteTema.AzulSel)
)

@Composable
fun ClienteScaffoldClaro(
    currentSection: String,
    onNavigate: (String) -> Unit,
    @DrawableRes logoRes: Int,
    onLogout: () -> Unit = LocalOnLogout.current,
    content: @Composable () -> Unit
) {
    var confirmarLogout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClienteTema.Fondo)
    ) {
        // Encabezado recto con botón de cerrar sesión
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Navy)
                .statusBarsPadding()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = "Axolotl Fitness Club",
                modifier = Modifier
                    .height(60.dp)
                    .graphicsLayer(scaleX = 1.6f, scaleY = 1.6f) // baja a 1.4f si se ve cortado o muy grande
            )
            IconButton(
                onClick = { confirmarLogout = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) { content() }

        // Barra inferior recta y baja.
        // El Box pinta el fondo y deja espacio para la barra de botones del sistema
        // (si el teléfono usa gestos, ese espacio es mínimo).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Navy)
                .navigationBarsPadding()
        ) {
            NavigationBar(
                modifier = Modifier.height(64.dp),
                containerColor = ClienteTema.Navy,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                TABS_CLARO.forEach { tab ->
                    NavigationBarItem(
                        selected = tab.label == currentSection,
                        onClick = { onNavigate(tab.label) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ClienteTema.AzulSel,
                            selectedTextColor = ClienteTema.AzulSel,
                            unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }

    // Diálogo de confirmación (estilo claro, texto centrado)
    if (confirmarLogout) {
        Dialog(onDismissRequest = { confirmarLogout = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClienteTema.Beige, RoundedCornerShape(20.dp))
                    .border(1.5.dp, LogoutBorderClaro, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = ClienteTema.Azul
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
                        border = BorderStroke(1.dp, ClienteTema.Azul),
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

// ---------- Scaffold usado por las pantallas del instructor ----------

private val CLIENTE_TABS = listOf(
    ScaffoldTab("Inicio", Icons.Filled.Home),
    ScaffoldTab("Asesorías", Icons.Filled.FitnessCenter),
    ScaffoldTab("Perfil", Icons.Filled.Person)
)

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
                        .padding(vertical = 20.dp)
                        .height(105.dp)
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

        Box(modifier = Modifier.weight(1f)) {
            content()
        }

        HorizontalDividerLine()
        NavigationBar(
            containerColor = PageBackground,
            tonalElevation = 0.dp
        ) {
            tabs.forEach { tab ->
                NavigationBarItem(
                    selected = tab.label == currentSection,
                    onClick = { onNavigate(tab.label) },
                    icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
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