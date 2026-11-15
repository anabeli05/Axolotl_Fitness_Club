package com.example.gymcontrol.ui.recepcion.perfil

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymcontrol.R
import com.example.gymcontrol.ui.components.PerfilPersonalScreen

/** Perfil de Recepción: su foto la toma el Encargado. */
@Composable
fun RecepcionPerfilScreen(onNavigate: (String) -> Unit = {}) {
    PerfilPersonalScreen(
        rolTexto = stringResource(R.string.role_reception),
        puedeTomarFoto = false,
        onNavigate = onNavigate
    )
}
