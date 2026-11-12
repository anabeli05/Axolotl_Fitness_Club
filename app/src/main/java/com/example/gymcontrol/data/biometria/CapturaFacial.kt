package com.example.gymcontrol.data.biometria

import android.graphics.Bitmap
import androidx.annotation.StringRes
import com.example.gymcontrol.R
import com.google.mlkit.vision.face.Face
import kotlin.math.abs

/** Resultado de procesar un cuadro de la cámara. */
sealed interface PasoFacial {
    /** Todavía no hay rostro listo: texto para orientar al cliente y avance (0..1). */
    data class Indicacion(@StringRes val textRes: Int, val progreso: Float = 0f) : PasoFacial
    /** Rostro capturado: vector listo para enviar a la base de datos. */
    class Listo(val vector: FloatArray) : PasoFacial
}

/**
 * Elige SIEMPRE el rostro más cercano a la cámara (el de mayor tamaño) e ignora a quienes
 * están detrás. Junta varias muestras seguidas del mismo rostro antes de dar el vector.
 * Se usa solo desde el hilo de la cámara.
 *
 * @param soloElMasCercano true en el escáner de entrada: nunca se detiene por haber más
 *        personas; escanea al más cercano y, si ya empezó con alguien, lo termina aunque
 *        otro se acerque casi igual (solo cambia si el otro está claramente más cerca).
 *        false en el registro de rostros: si hay dos personas a la misma distancia se
 *        pide que quede una sola, para no registrar la cara equivocada.
 */
class CapturaFacial(
    private val reconocedor: ReconocedorFacial,
    private val muestrasNecesarias: Int,
    private val soloElMasCercano: Boolean = false
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
        val cara = if (soloElMasCercano) elegirMasCercano(rostros) else masCercano(rostros)
        if (cara == null) {
            rostroProcesado = null // ya se retiró
            reiniciar()
            return PasoFacial.Indicacion(R.string.facial_prompt_position_camera)
        }

        val id = cara.trackingId
        if (id != null && id == rostroProcesado) {
            return PasoFacial.Indicacion(R.string.facial_prompt_ready_retire, 1f)
        }

        // Registro: dos personas casi a la misma distancia, no se adivina quién es.
        // En la entrada no se detiene: los de atrás simplemente se ignoran.
        if (!soloElMasCercano) {
            val segunda = rostros.filter { it !== cara }.maxOfOrNull { area(it) } ?: 0
            if (segunda > area(cara) * 0.80f) {
                reiniciar()
                return PasoFacial.Indicacion(R.string.facial_prompt_single_person)
            }
        }

        val caja = cara.boundingBox
        if (caja.width() < frame.width * TAMANO_MINIMO) {
            reiniciar()
            return PasoFacial.Indicacion(R.string.facial_prompt_come_closer)
        }
        if (caja.left < 0 || caja.top < 0 || caja.right > frame.width || caja.bottom > frame.height) {
            reiniciar()
            return PasoFacial.Indicacion(R.string.facial_prompt_center_face)
        }
        if (abs(cara.headEulerAngleY) > GIRO_MAXIMO || abs(cara.headEulerAngleZ) > GIRO_MAXIMO ||
            abs(cara.headEulerAngleX) > GIRO_MAXIMO
        ) {
            muestras.clear()
            return PasoFacial.Indicacion(R.string.facial_prompt_look_camera)
        }

        // Cambió la persona: se empieza de nuevo
        if (id != rostroActual) {
            muestras.clear()
            rostroActual = id
        }

        muestras += try {
            reconocedor.vector(frame, caja)
        } catch (e: Exception) {
            return PasoFacial.Indicacion(R.string.facial_prompt_center_face)
        }

        if (muestras.size < muestrasNecesarias) {
            return PasoFacial.Indicacion(R.string.facial_prompt_hold_still, muestras.size.toFloat() / muestrasNecesarias)
        }

        val vector = ReconocedorFacial.promedio(muestras)
        // Si alguna muestra salió muy distinta (movimiento, otra persona) se repite
        val consistente = muestras.all { ReconocedorFacial.similitud(it, vector) >= CONSISTENCIA }
        muestras.clear()
        if (!consistente) return PasoFacial.Indicacion(R.string.facial_prompt_hold_still, 0f)
        return PasoFacial.Listo(vector)
    }

    /**
     * El más cercano, pero si ya se estaba escaneando a alguien y sigue casi igual de cerca
     * se continúa con él; así no se reinicia cuando dos personas están a distancias parecidas.
     */
    private fun elegirMasCercano(rostros: List<Face>): Face? {
        val mayor = masCercano(rostros) ?: return null
        val actual = rostroActual?.let { id -> rostros.firstOrNull { it.trackingId == id } }
        return if (actual != null && actual !== mayor && area(actual) >= area(mayor) * CONSERVAR_ACTUAL) actual
        else mayor
    }

    companion object {
        /** El que ya se está escaneando se conserva mientras tenga al menos este tamaño del más grande. */
        private const val CONSERVAR_ACTUAL = 0.85f

        /** El rostro debe ocupar al menos este ancho del cuadro (lo de atrás se ve más chico). */
        private const val TAMANO_MINIMO = 0.25f
        private const val GIRO_MAXIMO = 18f
        // FaceNet-512 varía un poco más entre cuadros que MobileFaceNet
        private const val CONSISTENCIA = 0.75f

        private fun area(f: Face) = f.boundingBox.width() * f.boundingBox.height()

        /** El rostro más cercano a la cámara es el que se ve más grande. */
        fun masCercano(rostros: List<Face>): Face? = rostros.maxByOrNull { area(it) }
    }
}
