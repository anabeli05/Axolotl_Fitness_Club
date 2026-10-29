package com.example.gymcontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.data.model.UserRole

fun seccionesPara(rol: UserRole): List<String> = when (rol) {
    UserRole.RECEPCION -> listOf("Home", "Clientes", "Solicitudes")
    UserRole.ENCARGADO -> listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")
    UserRole.CLIENTE, UserRole.INSTRUCTOR -> emptyList()
}

fun UserRole.tieneSidebar(): Boolean = seccionesPara(this).isNotEmpty()

fun UserRole.etiqueta(): String = when (this) {
    UserRole.RECEPCION -> "Recepción"
    UserRole.ENCARGADO -> "Encargado"
    UserRole.INSTRUCTOR -> "Instructor"
    UserRole.CLIENTE -> "Cliente"
}

private fun iconoPara(seccion: String): ImageVector = when (seccion) {
    "Home", "Dashboard" -> Icons.Filled.Home
    "Clientes" -> Icons.Filled.Face
    "Solicitudes" -> Icons.Filled.Notifications
    "Personal" -> Icons.Filled.Person
    "Servicios" -> Icons.Filled.Build
    "Gastos" -> Icons.Filled.ShoppingCart
    "Reportes" -> Icons.Filled.DateRange
    else -> Icons.Filled.Home
}

private val BorderBrush = Brush.linearGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel))

@Composable
fun GymSidebar(
    secciones: List<String>,
    titulo: String,
    currentSection: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    Row(Modifier.fillMaxSize().background(ClienteTema.Navy)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // ---------- Encabezado ----------
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // El logo ya trae su propio aro dorado, así que se muestra solo (sin círculo extra)
                Image(
                    painter = painterResource(id = com.example.gymcontrol.R.drawable.logo_final),
                    contentDescription = "Logo Axolotl",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(110.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "AXOLOTL",
                    color = ClienteTema.Texto,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .background(ClienteTema.AzulSel.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .border(1.dp, ClienteTema.AzulSel, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(titulo, color = ClienteTema.AzulSel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(Brush.horizontalGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel)))
            )
            Spacer(Modifier.height(16.dp))

            // ---------- Secciones ----------
            secciones.forEach { section ->
                val selected = section == currentSection
                val shape = RoundedCornerShape(12.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(shape)
                        .then(
                            if (selected) Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            ClienteTema.AzulSel.copy(alpha = 0.30f),
                                            ClienteTema.AzulSel.copy(alpha = 0.05f)
                                        )
                                    ),
                                    shape
                                )
                                .border(BorderStroke(1.dp, ClienteTema.AzulSel), shape)
                            else Modifier
                        )
                        .clickable { onNavigate(section) }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        iconoPara(section),
                        contentDescription = null,
                        tint = if (selected) ClienteTema.AzulSel else ClienteTema.TextoSec,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = section,
                        color = if (selected) ClienteTema.Texto else ClienteTema.TextoSec,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ---------- Cerrar sesión ----------
            val rojo = ClienteTema.Rojo
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(rojo.copy(alpha = 0.12f), shape)
                    .border(1.dp, rojo.copy(alpha = 0.7f), shape)
                    .clickable(onClick = onLogout)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = rojo,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text("Cerrar sesión", color = rojo, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Borde derecho degradado
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Brush.verticalGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel)))
        )
    }
}