package com.example.gymcontrol.data.biometria

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Convierte un rostro en un vector de 512 números con FaceNet (assets/facenet_512.tflite).
 * Dos vectores de la misma persona dan una similitud alta (producto punto cercano a 1).
 * No se guarda ninguna foto: solo viaja el vector.
 */
class ReconocedorFacial(context: Context) : Closeable {

    private val interpreter: Interpreter
    private val entrada = ByteBuffer.allocateDirect(1 * TAM * TAM * 3 * 4).order(ByteOrder.nativeOrder())
    private val pixeles = IntArray(TAM * TAM)
    private var cerrado = false

    init {
        // Se copia a un buffer directo: funciona aunque el asset esté comprimido en el APK
        val bytes = context.assets.open(MODELO).use { it.readBytes() }
        val modelo = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        modelo.put(bytes)
        modelo.rewind()
        interpreter = Interpreter(modelo, Interpreter.Options().setNumThreads(4))
    }

    /** Vector normalizado (largo 1) del rostro que está en [caja] dentro de [frame]. */
    @Synchronized
    fun vector(frame: Bitmap, caja: Rect): FloatArray {
        check(!cerrado) { "Reconocedor cerrado" }
        val r = Rect(caja)
        if (!r.intersect(0, 0, frame.width, frame.height) || r.width() < 8 || r.height() < 8) {
            throw IllegalArgumentException("Rostro fuera de la imagen")
        }
        val recorte = Bitmap.createBitmap(frame, r.left, r.top, r.width(), r.height())
        val escalado = Bitmap.createScaledBitmap(recorte, TAM, TAM, true)
        escalado.getPixels(pixeles, 0, TAM, 0, 0, TAM, TAM)

        // FaceNet espera la imagen estandarizada: (valor - media) / desviación de esa misma imagen
        var suma = 0.0
        var sumaCuadrados = 0.0
        for (p in pixeles) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            suma += r + g + b
            sumaCuadrados += (r * r + g * g + b * b).toDouble()
        }
        val n = pixeles.size * 3
        val media = suma / n
        val desviacion = maxOf(sqrt(maxOf(sumaCuadrados / n - media * media, 0.0)), 1.0 / sqrt(n.toDouble()))
        val m = media.toFloat()
        val d = desviacion.toFloat()

        entrada.rewind()
        for (p in pixeles) {
            entrada.putFloat((((p shr 16) and 0xFF) - m) / d)
            entrada.putFloat((((p shr 8) and 0xFF) - m) / d)
            entrada.putFloat(((p and 0xFF) - m) / d)
        }
        entrada.rewind()

        val salida = Array(1) { FloatArray(DIMENSION) }
        interpreter.run(entrada, salida)

        if (escalado !== recorte) escalado.recycle()
        if (recorte !== frame) recorte.recycle()
        return normalizar(salida[0])
    }

    @Synchronized
    override fun close() {
        if (cerrado) return
        cerrado = true
        interpreter.close()
    }

    companion object {
        /** Debe coincidir con la base de datos (sql/vectores_512.sql). */
        const val DIMENSION = 512
        private const val TAM = 160
        private const val MODELO = "facenet_512.tflite"

        fun normalizar(v: FloatArray): FloatArray {
            var suma = 0f
            for (x in v) suma += x * x
            val n = sqrt(suma).takeIf { it > 0f } ?: return v
            return FloatArray(v.size) { v[it] / n }
        }

        /** Promedio de varias muestras de la misma persona (más estable que una sola foto). */
        fun promedio(muestras: List<FloatArray>): FloatArray {
            val r = FloatArray(DIMENSION)
            for (m in muestras) for (i in r.indices) r[i] += m[i]
            return normalizar(r)
        }

        fun similitud(a: FloatArray, b: FloatArray): Float {
            var s = 0f
            for (i in a.indices) s += a[i] * b[i]
            return s
        }
    }
}
