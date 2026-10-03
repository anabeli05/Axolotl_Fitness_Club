package com.example.gymcontrol.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gymcontrol.data.model.UserRole

object SesionActual {
    var rol: UserRole? by mutableStateOf(null)
        private set

    fun iniciar(rol: UserRole?) { this.rol = rol }
    fun cerrar() { rol = null }
}