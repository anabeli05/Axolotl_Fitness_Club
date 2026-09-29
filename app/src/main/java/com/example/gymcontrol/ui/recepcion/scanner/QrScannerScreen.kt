package com.example.gymcontrol.ui.recepcion.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun QrScannerScreen() {
    ScreenContainer("Escanear acceso") {
        Card(Modifier.fillMaxWidth().height(360.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QrCodeScanner, null, Modifier.size(120.dp))
                    Text("Vista para cámara / lector QR")
                    Text("Conecta CameraX o ML Kit aquí", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
