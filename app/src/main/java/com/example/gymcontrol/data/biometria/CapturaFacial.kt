package com.example.gymcontrol.data.biometria

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import kotlin.math.abs

/** Resultado de procesar un cuadro de la cámara. */
sealed interface PasoFacial {
    /** Todavía no hay rostro listo: texto para orientar al cliente y avance (0..1). */
    data class Indicacion(val texto: String, val progreso: Float = 0f) : PasoFacial
    /** Rostro capturado: vector listo para enviar a la base de datos. */
    class Listo(val vector: FloatArray) : PasoFacial
}

/**
 * Elige SIEMPRE el rostro más cercano a la cámara (el de mayor tamaño) e ignora a quienes
 * están detrás. Junta varias muestras seguidas del mismo rostro antes de dar el vector.
 * Se usa solo desde el hilo de la cámara.
 */
class CapturaFacial(
    private val reconocedor: ReconocedorFacial,
    private val muestrasNecesarias: Int
) {
    private var rostroActual: Int? = null
    private val muestras = mutableListOf<FloatArray>()

    // Rostro que ya se procesó: no se vuelve a enviar mientras siga frente a la cámara
    private var rostroProcesado: Int? = null

    fun reiniciar() {
        rostroActual = null
        muestras.clear()
    }

    /** Marca el rostro actual como ya atendido (evita registros duplicados). */
    fun marcarProcesado() {
        rostroProcesado = rostroActual
        reiniciar()
    }

    /** Permite volver a capturar al mismo rostro (p. ej. si hubo un error de red). */
    fun olvidarProcesado() {
        rostroProcesado = null
    }

    fun procesar(frame: Bitmap, rostros: List<Face>): PasoFacial {
        val cara = masCercano(rostros)
        if (cara == null) {
            rostroProcesado = null // ya se retiró
            reiniciar()
            return PasoFacial.Indicacion("Colócate frente a la cámara")
        }

        val id = cara.trackingId
        if (id != null && id == rostroProcesado) {
            return PasoFacial.Indicacion("Listo. Puedes retirarte de la cámara", 1f)
        }

        // Dos personas casi a la misma distancia: no se adivina quién es
        val segunda = rostros.filter { it !== cara }.maxOfOrNull { area(it) } ?: 0
        if (segunda > area(cara) * 0.80f) {
            reiniciar()
            return PasoFacial.Indicacion("Solo una persona frente a la cámara")
        }

        val caja = cara.boundingBox
        if (caja.width() < frame.width * TAMANO_MINIMO) {
            reiniciar()
            return PasoFacial.Indicacion("Acércate a la cámara")
        }
        if (caja.left < 0 || caja.top < 0 || caja.right > frame.width || caja.bottom > frame.height) {
            reiniciar()
            return PasoFacial.Indicacion("Centra tu rostro en el recuadro")
        }
        if (abs(cara.headEulerAngleY) > GIRO_MAXIMO || abs(cara.headEulerAngleZ) > GIRO_MAXIMO ||
            abs(cara.headEulerAngleX) > GIRO_MAXIMO
        ) {
            muestras.clear()
            return PasoFacial.Indicacion("Mira de frente a la cámara")
        }

        // Cambió la persona: se empieza de nuevo
        if (id != rostroActual) {
            muestras.clear()
            rostroActual = id
        }

        muestras += try {
            reconocedor.vector(frame, caja)
        } catch (e: Exception) {
            return PasoFacial.Indicacion("Centra tu rostro en el recuadro")
        }

        if (muestras.size < muestrasNecesarias) {
            return PasoFacial.Indicacion("No te muevas…", muestras.size.toFloat() / muestrasNecesarias)
        }

        val vector = ReconocedorFacial.promedio(muestras)
        // Si alguna muestra salió muy distinta (movimiento, otra persona) se repite
        val consistente = muestras.all { ReconocedorFacial.similitud(it, vector) >= CONSISTENCIA }
        muestras.clear()
        if (!consistente) return PasoFacial.Indicacion("No te muevas…", 0f)
        return PasoFacial.Listo(vector)
    }

    companion object {
        /** El rostro debe ocupar al menos este ancho del cuadro (lo de atrás se ve más chico). */
        private const val TAMANO_MINIMO = 0.25f
        private const val GIRO_MAXIMO = 18f
        private const val CONSISTENCIA = 0.80f

        private fun area(f: Face) = f.boundingBox.width() * f.boundingBox.height()

        /** El rostro más cercano a la cámara es el que se ve más grande. */
        fun masCercano(rostros: List<Face>): Face? = rostros.maxByOrNull { area(it) }
    }
}
