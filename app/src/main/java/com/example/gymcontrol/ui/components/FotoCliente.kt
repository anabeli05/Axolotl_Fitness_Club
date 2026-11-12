package com.example.gymcontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.R
import com.example.gymcontrol.data.biometria.FotoPerfil
import com.example.gymcontrol.data.remote.AccesoFacialRepository
import com.example.gymcontrol.data.remote.PersonalRepository

/**
 * Fotos de perfil ya descargadas en esta sesión (id → foto, o null si no tiene),
 * para no pedir dos veces la misma foto.
 */
open class CacheFotos(private val descargar: suspend (Long) -> String?) {
    private val cache = mutableMapOf<Long, ImageBitmap?>()

    fun enCache(clienteId: Long): Boolean = synchronized(cache) { cache.containsKey(clienteId) }

    fun guardada(clienteId: Long): ImageBitmap? = synchronized(cache) { cache[clienteId] }

    /** Descarga la foto si no está en caché. Si falla la red no se guarda, para reintentar después. */
    suspend fun obtener(clienteId: Long): ImageBitmap? {
        if (enCache(clienteId)) return guardada(clienteId)
        val img = FotoPerfil.aImagen(descargar(clienteId))
        synchronized(cache) { cache[clienteId] = img }
        return img
    }

    /** Se llama al tomar una foto nueva, para que se vuelva a descargar. */
    fun olvidar(clienteId: Long) = synchronized(cache) { cache.remove(clienteId); Unit }
}

/** Clientes: listas de Recepción y Encargado y el acceso facial (clientes.foto_perfil). */
object FotosClientes : CacheFotos({ AccesoFacialRepository.foto(it) })

/** Personal (Recepción, Instructor, Encargado): lista de Usuarios (usuarios.foto_perfil). */
object FotosPersonal : CacheFotos({ PersonalRepository.foto(it) })

/** Círculo con la foto de perfil (cliente o personal, según [cache]), o el ícono de persona. */
@Composable
fun FotoClienteAvatar(
    clienteId: Long,
    size: Dp,
    modifier: Modifier = Modifier,
    borderColor: Color = ClienteTema.Borde,
    borderWidth: Dp = 1.dp,
    // Cambia este valor para volver a buscar la foto (p. ej. al registrar el rostro)
    recarga: Any? = null,
    cache: CacheFotos = FotosClientes,
    onClick: ((ImageBitmap?) -> Unit)? = null
) {
    var foto by remember(clienteId, recarga) { mutableStateOf(cache.guardada(clienteId)) }
    LaunchedEffect(clienteId, recarga) {
        if (!cache.enCache(clienteId)) {
            foto = try { cache.obtener(clienteId) } catch (_: Exception) { null }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .background(ClienteTema.Navy, CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick(foto) } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val actual = foto
        if (actual != null) {
            Image(
                bitmap = actual,
                contentDescription = stringResource(R.string.perfil_photo_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = ClienteTema.TextoSec,
                modifier = Modifier.size(size * 0.65f)
            )
        }
    }
}

/**
 * Foto de perfil en grande. [accion] es un botón extra opcional
 * (p. ej. "Actualizar rostro" en Recepción).
 */
@Composable
fun FotoClienteDialog(
    nombre: String,
    foto: ImageBitmap,
    onDismiss: () -> Unit,
    accion: Pair<String, () -> Unit>? = null
) {
    Dialog(onDismissRequest = onDismiss) {
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
                nombre,
                color = ClienteTema.Texto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Image(
                bitmap = foto,
                contentDescription = stringResource(R.string.perfil_photo_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClienteTema.Borde),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                ) { Text(stringResource(R.string.foto_cliente_close)) }
                accion?.let { (texto, alPulsar) ->
                    Button(
                        onClick = alPulsar,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Azul, contentColor = Color.White)
                    ) { Text(texto) }
                }
            }
        }
    }
}
