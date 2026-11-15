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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.R

private val LogoutBorder = Brush.linearGradient(
    listOf(ClienteTema.Azul, ClienteTema.AzulSel)
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
                .background(ClienteTema.Beige, GymCardShape)
                .border(1.dp, LogoutBorder, GymCardShape)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = ClienteTema.AzulSel,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.logout),
                color = ClienteTema.Texto,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.logout_confirmation),
                color = ClienteTema.TextoSec,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onDismiss,
                    shape = GymButtonShape,
                    modifier = Modifier.defaultMinSize(minHeight = GymButtonMinHeight),
                    contentPadding = GymButtonPadding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.Rojo,
                        contentColor = Color.White
                    )
                ) { GymButtonText(stringResource(R.string.cancel)) }
                Button(
                    onClick = onConfirm,
                    shape = GymButtonShape,
                    modifier = Modifier.defaultMinSize(minHeight = GymButtonMinHeight),
                    contentPadding = GymButtonPadding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.Verde,
                        contentColor = Color.White
                    )
                ) { GymButtonText(stringResource(R.string.exit)) }
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
            contentDescription = stringResource(R.string.logout),
            tint = Color.White,
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
