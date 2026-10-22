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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Paleta del nuevo diseño del cliente. */
object ClienteTema {
    val Fondo = Color(0xFFBDB9B9)
    val Navy = Color(0xFF020A3A)
    val Beige = Color(0xFFE9E5D3)
    val Azul = Color(0xFF4F7BEA)
    val AzulClaro = Color(0xFF8DA6EA)
    val AzulSel = Color(0xFF6C8FF0)
    val Barras = Color(0xFF0A1B9A)
    val Texto = Color(0xFF111111)
    val TextoSec = Color(0xFF555555)
    val Borde = Color(0xFF333333)
    val Verde = Color(0xFF1B6B1B)
    val Rojo = Color(0xFFC62828)
}

private val TABS_CLARO = listOf(
    ScaffoldTab("Inicio", Icons.Filled.Home),
    ScaffoldTab("Asesorías", Icons.Filled.FitnessCenter),
    ScaffoldTab("Perfil", Icons.Filled.Person)
)

private val LogoutBorderClaro = Brush.linearGradient(
    listOf(ClienteTema.AzulSel, ClienteTema.Borde, ClienteTema.Rojo)
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
        // Encabezado recto y compacto con botón de cerrar sesión
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
                modifier = Modifier.height(56.dp)
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

        // Barra inferior recta y baja
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
                    tint = ClienteTema.Navy
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