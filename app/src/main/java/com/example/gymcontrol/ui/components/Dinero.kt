package com.example.gymcontrol.ui.components

import java.util.Locale

/** Cantidades de dinero con separador de miles y siempre 2 decimales: 5534.06 -> "$5,534.06". */
fun dinero(valor: Double): String {
    val texto = "%,.2f".format(Locale.US, kotlin.math.abs(valor))
    return if (valor < -0.004) "-$$texto" else "$$texto"
}
