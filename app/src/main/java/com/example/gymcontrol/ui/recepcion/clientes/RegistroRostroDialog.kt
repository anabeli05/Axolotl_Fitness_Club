package com.example.gymcontrol.ui.recepcion.clientes

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.R
import com.example.gymcontrol.data.biometria.CapturaFacial
import com.example.gymcontrol.data.biometria.PasoFacial
import com.example.gymcontrol.data.biometria.ReconocedorFacial
import com.example.gymcontrol.data.remote.AccesoFacialRepository
import com.example.gymcontrol.data.remote.ClienteRow
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.recepcion.scanner.FaceCameraPreview
import com.example.gymcontrol.ui.recepcion.scanner.ScannerCorners
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private val DialogBorder = Brush.linearGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel))

/**
 * Registra (o actualiza) el rostro del cliente. Toma 5 muestras del rostro más cercano,
 * las promedia y guarda solo el vector en la base de datos (sin fotos).
 */
@Composable
fun RegistroRostroDialog(
    cliente: ClienteRow,
    yaTieneRostro: Boolean,
    onDismiss: () -> Unit,
    onRegistrado: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val main = remember { Handler(Looper.getMainLooper()) }

    val reconocedor = remember { runCatching { ReconocedorFacial(context) }.getOrNull() }
    val captura = remember(reconocedor) { reconocedor?.let { CapturaFacial(it, muestrasNecesarias = 5) } }
    val pausa = remember { AtomicBoolean(false) }
    DisposableEffect(reconocedor) { onDispose { reconocedor?.close() } }

    val promptAskClient = stringResource(R.string.facial_prompt_ask_client)
    val promptSaving = stringResource(R.string.facial_prompt_saving)
    val registeredSuccess = stringResource(R.string.facial_registered_success, cliente.nombre)

    var indicacion by remember { mutableStateOf(promptAskClient) }
    var progreso by remember { mutableStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var listo by remember { mutableStateOf(false) }

    fun guardar(vector: FloatArray) {
        scope.launch {
            try {
                AccesoFacialRepository.registrarRostro(cliente.id, vector)
                listo = true
                indicacion = registeredSuccess
                delay(1_500)
                onRegistrado()
            } catch (e: Exception) {
                error = e.message?.substringBefore("\nCode:")?.substringAfterLast("message: ")
                    ?: "No se pudo guardar el rostro"
                captura?.olvidarProcesado()
                delay(2_500)
                progreso = 0f
                pausa.set(false)
            }
        }
    }

    val onFrame: (Bitmap, List<Face>) -> Unit = onFrame@{ frame, rostros ->
        val c = captura ?: return@onFrame
        if (pausa.get()) return@onFrame
        when (val paso = c.procesar(frame, rostros)) {
            is PasoFacial.Indicacion -> main.post {
                indicacion = context.getString(paso.textRes)
                progreso = paso.progreso
            }
            is PasoFacial.Listo -> {
                pausa.set(true)
                c.marcarProcesado()
                main.post {
                    error = null
                    progreso = 1f
                    indicacion = promptSaving
                    guardar(paso.vector)
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(ClienteTema.Beige)
                .border(BorderStroke(1.5.dp, DialogBorder), shape)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (yaTieneRostro) "Actualizar rostro" else "Registrar rostro",
                color = ClienteTema.Texto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${cliente.nombre} · ${cliente.numeroMembresia.ifBlank { "—" }}",
                color = ClienteTema.TextoSec,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (captura != null && !listo) {
                    FaceCameraPreview(modifier = Modifier.fillMaxSize(), onFrame = onFrame)
                }
                ScannerCorners(Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(12.dp))
            Text(
                if (captura == null) "No se pudo cargar el modelo facial" else indicacion,
                color = when {
                    captura == null -> ClienteTema.Rojo
                    listo -> ClienteTema.Verde
                    else -> ClienteTema.TextoSec
                },
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progreso },
                color = ClienteTema.AzulSel,
                trackColor = ClienteTema.Borde.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = ClienteTema.Rojo, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ClienteTema.Borde),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
            ) { Text(if (listo) "Cerrar" else "Cancelar") }
        }
    }
}
