package com.example.gymcontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.ui.theme.GymColors

private val LogoutBorder = Brush.linearGradient(
    listOf(GymColors.Purple, GymColors.Gold, GymColors.Red)
)

@Composable
fun LogoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GymColors.Surface, RoundedCornerShape(20.dp))
                .border(1.5.dp, LogoutBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = GymColors.Gold,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Cerrar sesión",
                color = GymColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "¿Seguro que quieres salir?",
                color = GymColors.TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GymColors.Purple),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.TextPrimary)
                ) { Text("Cancelar") }
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymColors.Red,
                        contentColor = Color.White
                    )
                ) { Text("Salir", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun BotonCerrarSesion(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = LocalOnLogout.current
) {
    var mostrar by remember { mutableStateOf(false) }

    IconButton(onClick = { mostrar = true }, modifier = modifier) {
        Icon(
            Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = "Cerrar sesión",
            tint = GymColors.Red,
            modifier = Modifier.size(26.dp)
        )
    }

    if (mostrar) {
        LogoutDialog(
            onConfirm = { mostrar = false; onLogout() },
            onDismiss = { mostrar = false }
        )
    }
}