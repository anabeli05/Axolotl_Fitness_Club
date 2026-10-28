package com.example.gymcontrol.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gymcontrol.data.model.UserRole

object SesionActual {
    var rol: UserRole? by mutableStateOf(null)
        private set

    var correo: String? = null
        private set

    // Datos del usuario que inició sesión (vienen de login_auto)
    var id: Long? = null
        private set

    var nombre: String? by mutableStateOf(null)
        private set

    var codigoQr: String? = null
        private set

    fun iniciar(rol: UserRole?) { this.rol = rol }
    fun fijarCorreo(correo: String) { this.correo = correo }

    fun fijarDatos(id: Long, nombre: String, codigoQr: String?) {
        this.id = id
        this.nombre = nombre
        this.codigoQr = codigoQr
    }

    fun fijarNombre(nombre: String) { this.nombre = nombre }

    fun cerrar() {
        rol = null
        correo = null
        id = null
        nombre = null
        codigoQr = null
    }
}