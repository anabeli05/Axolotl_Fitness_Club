package com.example.gymcontrol.ui.components

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Calcula la escala uniforme de 10 divisiones para el eje Y de cualquier gráfica.
 * Retorna un Pair con:
 * - techoEjeY: El valor máximo del eje Y (techo ajustado a un múltiplo limpio).
 * - marcas: La lista de 10 marcas visibles por encima del 0.
 */
fun calcularEscalaEjeY(maxValor: Float, numDivisiones: Int = 10): Pair<Float, List<Float>> {
    if (maxValor <= 0f) {
        val marcas = (1..numDivisiones).map { it.toFloat() }
        return numDivisiones.toFloat() to marcas
    }
    if (maxValor <= 10f) {
        val marcas = (1..10).map { it.toFloat() }
        return 10f to marcas
    }

    val pasoBruto = maxValor / numDivisiones
    val magnitud = 10.0.pow(floor(log10(pasoBruto.toDouble()))).toFloat()
    val residuo = pasoBruto / magnitud

    val pasoLimpio = when {
        residuo <= 1f -> 1f * magnitud
        residuo <= 2f -> 2f * magnitud
        residuo <= 2.5f -> 2.5f * magnitud
        residuo <= 5f -> 5f * magnitud
        else -> 10f * magnitud
    }

    val marcas = (1..numDivisiones).map { it * pasoLimpio }
    val techoEjeY = pasoLimpio * numDivisiones
    return techoEjeY to marcas
}
