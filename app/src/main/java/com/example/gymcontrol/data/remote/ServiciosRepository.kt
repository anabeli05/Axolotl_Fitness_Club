package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

@Serializable
data class ServicioRow(val id: Long, val nombre: String, val duracion: String, val precio: Double)

@Serializable
data class ServicioNuevo(val nombre: String, val duracion: String, val precio: Double)

object ServiciosRepository {
    private val tabla get() = SupabaseProvider.client.from("servicios")

    suspend fun listar(): List<ServicioRow> =
        tabla.select { order("id", Order.ASCENDING) }.decodeList<ServicioRow>()

    suspend fun crear(nombre: String, duracion: String, precio: Double) {
        tabla.insert(ServicioNuevo(nombre, duracion, precio))
    }

    suspend fun actualizar(id: Long, nombre: String, duracion: String, precio: Double) {
        tabla.update(ServicioNuevo(nombre, duracion, precio)) { filter { eq("id", id) } }
    }

    suspend fun eliminar(id: Long) {
        tabla.delete { filter { eq("id", id) } }
    }
}