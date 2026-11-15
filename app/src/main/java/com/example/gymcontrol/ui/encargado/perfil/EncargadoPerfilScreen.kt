package com.example.gymcontrol.ui.encargado.perfil

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gymcontrol.R
import com.example.gymcontrol.ui.components.PerfilPersonalScreen

/** Perfil del Encargado: mismo diseño que Recepción, y además toma o actualiza su propia foto. */
@Composable
fun EncargadoPerfilScreen(onNavigate: (String) -> Unit = {}) {
    PerfilPersonalScreen(
        rolTexto = stringResource(R.string.role_manager),
        puedeTomarFoto = true,
        onNavigate = onNavigate
    )
}
