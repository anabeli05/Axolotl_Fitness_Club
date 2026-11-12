package com.example.gymcontrol.data.remote

import com.example.gymcontrol.ui.components.SesionActual
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable

@Serializable
data class ServicioRow(val id: Long, val nombre: String, val duracion: String, val precio: Double)

@Serializable
private data class ServicioCrear(
    val p_actor_gmail: String, val p_nombre: String, val p_duracion: String, val p_precio: Double
)

@Serializable
private data class ServicioEditar(
    val p_actor_gmail: String, val p_id: Long, val p_nombre: String,
    val p_duracion: String, val p_precio: Double
)

@Serializable
private data class ServicioId(val p_actor_gmail: String, val p_id: Long)

object ServiciosRepository {
    private val actor get() = SesionActual.correo ?: ""

    // La lectura sigue siendo pública (los clientes ven los servicios al pagar)
    suspend fun listar(): List<ServicioRow> =
        SupabaseProvider.client.from("servicios")
            .select { order("id", Order.ASCENDING) }
            .decodeList<ServicioRow>()

    suspend fun crear(nombre: String, duracion: String, precio: Double) {
        SupabaseProvider.client.postgrest.rpc(
            "crear_servicio", ServicioCrear(actor, nombre, duracion, precio)
        )
    }

    suspend fun actualizar(id: Long, nombre: String, duracion: String, precio: Double) {
        SupabaseProvider.client.postgrest.rpc(
            "actualizar_servicio", ServicioEditar(actor, id, nombre, duracion, precio)
        )
    }

    suspend fun eliminar(id: Long) {
        SupabaseProvider.client.postgrest.rpc("eliminar_servicio", ServicioId(actor, id))
    }
}