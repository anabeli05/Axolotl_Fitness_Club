package com.example.gymcontrol.ui.components

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.R
import com.example.gymcontrol.data.biometria.CapturaFacial
import com.example.gymcontrol.data.biometria.FotoPerfil
import com.example.gymcontrol.ui.recepcion.scanner.CamaraFoto
import com.example.gymcontrol.ui.recepcion.scanner.FaceCameraPreview
import com.example.gymcontrol.ui.recepcion.scanner.ScannerCorners
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.launch

/**
 * Toma la foto de perfil de un miembro del personal: cámara → "Tomar foto" → vista previa
 * → Guardar o Repetir. La foto es a resolución completa y se recorta alrededor de la cara.
 * [onGuardar] recibe la foto en base64 y debe lanzar una excepción si no se pudo guardar.
 */
@Composable
fun TomarFotoDialog(
    nombre: String,
    onDismiss: () -> Unit,
    onGuardar: suspend (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val main = remember { Handler(Looper.getMainLooper()) }
    val camaraFoto = remember { CamaraFoto() }

    var hayRostro by remember { mutableStateOf(false) }
    var trabajando by remember { mutableStateOf(false) }
    var foto by remember { mutableStateOf<String?>(null) }  // base64 lista para guardar
    var error by remember { mutableStateOf<String?>(null) }

    val msgNoFace = stringResource(R.string.foto_no_face)
    val msgSaveError = stringResource(R.string.foto_save_error)

    // Respaldo por si la foto a resolución completa no está disponible en el equipo
    var ultimoCuadro by remember { mutableStateOf<Pair<Bitmap, Face>?>(null) }
    val onFrame: (Bitmap, List<Face>) -> Unit = { frame, rostros ->
        val cara = CapturaFacial.masCercano(rostros)
        main.post {
            hayRostro = cara != null
            ultimoCuadro = cara?.let { frame to it }
        }
    }

    fun tomar() {
        trabajando = true
        error = null
        scope.launch {
            val nueva = camaraFoto.tomar()?.let { FotoPerfil.desdeFotoAltaCalidad(it) }
                ?: ultimoCuadro?.let { (frame, cara) ->
                    runCatching { FotoPerfil.desdeCuadro(frame, cara.boundingBox) }.getOrNull()
                }
            if (nueva == null) error = msgNoFace else foto = nueva
            trabajando = false
        }
    }

    fun guardar(base64: String) {
        trabajando = true
        error = null
        scope.launch {
            try {
                onGuardar(base64)
            } catch (e: Exception) {
                error = e.message?.substringBefore("\nCode:")?.substringAfterLast("message: ") ?: msgSaveError
                trabajando = false
            }
        }
    }

    Dialog(onDismissRequest = { if (!trabajando) onDismiss() }) {
        val forma = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(forma)
                .background(ClienteTema.Beige)
                .border(1.5.dp, ClienteTema.Borde, forma)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.perfil_photo_dialog_title),
                color = ClienteTema.Texto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(nombre, color = ClienteTema.TextoSec, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val vista = foto?.let { FotoPerfil.aImagen(it) }
                if (vista != null) {
                    Image(
                        bitmap = vista,
                        contentDescription = stringResource(R.string.perfil_photo_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    FaceCameraPreview(modifier = Modifier.fillMaxSize(), camaraFoto = camaraFoto, onFrame = onFrame)
                    ScannerCorners(Modifier.fillMaxSize())
                }
                if (trabajando) CircularProgressIndicator(color = ClienteTema.AzulSel)
            }

            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    error != null -> error!!
                    foto != null -> stringResource(R.string.foto_preview_hint)
                    hayRostro -> stringResource(R.string.foto_ready_hint)
                    else -> stringResource(R.string.foto_face_hint)
                },
                color = if (error != null) ClienteTema.Rojo else ClienteTema.TextoSec,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val lista = foto
                OutlinedButton(
                    onClick = { if (lista != null) foto = null else onDismiss() },
                    enabled = !trabajando,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClienteTema.Borde),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                ) { Text(stringResource(if (lista != null) R.string.foto_retake else R.string.cancel)) }
                Button(
                    onClick = { if (lista != null) guardar(lista) else tomar() },
                    enabled = !trabajando && (lista != null || hayRostro),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Azul, contentColor = Color.White)
                ) { Text(stringResource(if (lista != null) R.string.save else R.string.perfil_photo_take)) }
            }
        }
    }
}
