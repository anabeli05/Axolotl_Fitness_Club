package com.example.gymcontrol.ui.components

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

internal fun niceCeil(value: Float): Float {
    if (value <= 0f) return 10f
    val exponent = floor(log10(value.toDouble())).toInt()
    val magnitude = 10.0.pow(exponent.toDouble()).toFloat()
    val residual = value / magnitude
    val niceResidual = when {
        residual <= 1f -> 1f
        residual <= 2f -> 2f
        residual <= 5f -> 5f
        else -> 10f
    }
    return niceResidual * magnitude
}

internal fun axisLabels(maxValue: Float, steps: Int = 5): List<String> {
    val niceMax = niceCeil(maxValue)
    return (steps downTo 0).map { i ->
        val v = niceMax * i / steps
        if (v >= 1f) v.toInt().toString() else "0"
    }
}