package com.example.gymcontrol.data.biometria

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Foto de perfil (clientes y personal): un cuadrado alrededor de la cara,
 * guardado como JPEG en base64.
 *
 * Lo ideal es [desdeFotoAltaCalidad] (foto a resolución completa de la cámara).
 * [desdeCuadro] usa el cuadro del análisis (~640×480) y solo es el respaldo,
 * porque se ve borroso.
 */
object FotoPerfil {
    private const val TAM = 512        // px del lado del cuadrado final
    private const val MARGEN = 0.6f    // espacio alrededor de la cara (cabello, barbilla)
    private const val CALIDAD = 90
    private const val LADO_DETECCION = 1600  // la foto completa se reduce a esto antes de buscar la cara

    /** Recorta la cara de una foto a resolución completa. null si no encuentra un rostro. */
    suspend fun desdeFotoAltaCalidad(foto: Bitmap): String? = withContext(Dispatchers.Default) {
        // Fotos de 12 MP o más: se reducen para no gastar memoria (sigue siendo mucho más de 512 px)
        val escala = minOf(1f, LADO_DETECCION.toFloat() / maxOf(foto.width, foto.height))
        val base = if (escala < 1f)
            Bitmap.createScaledBitmap(foto, (foto.width * escala).toInt(), (foto.height * escala).toInt(), true)
        else foto

        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setMinFaceSize(0.15f)
                .build()
        )
        try {
            val rostros = Tasks.await(detector.process(InputImage.fromBitmap(base, 0)))
            val cara = rostros.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                ?: return@withContext null
            desdeCuadro(base, cara.boundingBox)
        } catch (e: Exception) {
            null
        } finally {
            detector.close()
        }
    }

    /** Recorta un cuadrado alrededor de [caja] y lo devuelve en base64. */
    fun desdeCuadro(frame: Bitmap, caja: Rect): String {
        val lado = (maxOf(caja.width(), caja.height()) * (1 + MARGEN)).toInt()
            .coerceAtMost(minOf(frame.width, frame.height))
        val left = (caja.centerX() - lado / 2).coerceIn(0, frame.width - lado)
        // Un poco más arriba del centro para que entre el cabello
        val top = (caja.centerY() - lado / 2 - caja.height() / 10).coerceIn(0, frame.height - lado)

        val recorte = Bitmap.createBitmap(frame, left, top, lado, lado)
        // Solo se reduce; si el recorte es más chico que TAM no se agranda (agrandar es lo que emborrona)
        val final = if (lado > TAM) Bitmap.createScaledBitmap(recorte, TAM, TAM, true) else recorte
        val salida = ByteArrayOutputStream()
        final.compress(Bitmap.CompressFormat.JPEG, CALIDAD, salida)
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
