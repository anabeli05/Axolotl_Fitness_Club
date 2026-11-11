package com.example.gymcontrol.data.biometria

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

/**
 * Foto de perfil del cliente: se recorta del cuadro de la cámara al registrar su rostro
 * y se guarda como JPEG pequeño en base64 (clientes.foto_perfil).
 */
object FotoPerfil {
    private const val TAM = 320       // px del lado del cuadrado final
    private const val MARGEN = 0.45f  // espacio alrededor de la cara (cabello, barbilla)
    private const val CALIDAD = 80

    /** Recorta un cuadrado alrededor de la cara y lo devuelve en base64. */
    fun desdeCuadro(frame: Bitmap, caja: Rect): String {
        val lado = (maxOf(caja.width(), caja.height()) * (1 + MARGEN)).toInt()
            .coerceAtMost(minOf(frame.width, frame.height))
        val left = (caja.centerX() - lado / 2).coerceIn(0, frame.width - lado)
        val top = (caja.centerY() - lado / 2).coerceIn(0, frame.height - lado)

        val recorte = Bitmap.createBitmap(frame, left, top, lado, lado)
        val escalado = Bitmap.createScaledBitmap(recorte, TAM, TAM, true)
        val salida = ByteArrayOutputStream()
        escalado.compress(Bitmap.CompressFormat.JPEG, CALIDAD, salida)
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
    }

    /** Convierte la foto guardada en imagen para Compose (null si no hay o está dañada). */
    fun aImagen(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
