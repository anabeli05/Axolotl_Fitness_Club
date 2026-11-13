package com.example.gymcontrol.data.remote

import com.example.gymcontrol.ui.components.SesionActual
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable

@Serializable
data class ServicioRow(
    val id: Long, val nombre: String, val duracion: String, val precio: Double,
    val visible_clientes: Boolean = true, val visible_recepcion: Boolean = true,
    val es_visita: Boolean = false
)

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

@Serializable
private data class ServicioConfig(
    val p_actor_gmail: String, val p_id: Long, val p_clientes: Boolean, val p_recepcion: Boolean,
    val p_es_visita: Boolean
)

object ServiciosRepository {
    private val actor get() = SesionActual.correo ?: ""

    // La lectura sigue siendo pública (los clientes ven los servicios al pagar)
    suspend fun listar(): List<ServicioRow> =
        SupabaseProvider.client.from("servicios")
            .select { order("id", Order.ASCENDING) }
            .decodeList<ServicioRow>()

    suspend fun crear(
        nombre: String, duracion: String, precio: Double,
        visibleClientes: Boolean = true, visibleRecepcion: Boolean = true, esVisita: Boolean = false
    ) {
        SupabaseProvider.client.postgrest.rpc(
            "crear_servicio", ServicioCrear(actor, nombre, duracion, precio)
        )
        // Un servicio nuevo nace visible para todos y sin ser visita; solo se ajusta si se pidió otra cosa
        if (!visibleClientes || !visibleRecepcion || esVisita) {
            val nuevo = listar().filter { it.nombre.equals(nombre, ignoreCase = true) }.maxByOrNull { it.id }
            if (nuevo != null) configurar(nuevo.id, visibleClientes, visibleRecepcion, esVisita)
        }
    }

    suspend fun actualizar(
        id: Long, nombre: String, duracion: String, precio: Double,
        visibleClientes: Boolean = true, visibleRecepcion: Boolean = true, esVisita: Boolean = false
    ) {
        SupabaseProvider.client.postgrest.rpc(
            "actualizar_servicio", ServicioEditar(actor, id, nombre, duracion, precio)
        )
        configurar(id, visibleClientes, visibleRecepcion, esVisita)
    }

    /**
     * Para quién se ve el servicio (clientes en Pagos y/o Recepción) y si es la visita.
     * La visita no sale como plan: se cobra en la sección "Visitas". Solo puede haber una.
     */
    suspend fun configurar(id: Long, clientes: Boolean, recepcion: Boolean, esVisita: Boolean) {
        SupabaseProvider.client.postgrest.rpc(
            "configurar_servicio", ServicioConfig(actor, id, clientes, recepcion, esVisita)
        )
    }

    suspend fun eliminar(id: Long) {
        SupabaseProvider.client.postgrest.rpc("eliminar_servicio", ServicioId(actor, id))
    }
}