package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SolicitudAdminRow(
    val id: Long,
    val estado: String,
    val costo: Double = 0.0,
    @SerialName("fecha_solicitud") val fecha: String = "",
    val cliente: String,
    @SerialName("numero_membresia") val numeroMembresia: String = "",
    val instructor: String
)

@Serializable
private data class ResolverParams(val p_id: Long, val p_estado: String)

object SolicitudesRepository {
    suspend fun listar(): List<SolicitudAdminRow> =
        SupabaseProvider.client.from("solicitudes_lista")
            .select { order("id", Order.DESCENDING) }
            .decodeList<SolicitudAdminRow>()

    suspend fun resolver(id: Long, estado: String) {
        SupabaseProvider.client.postgrest.rpc("resolver_solicitud", ResolverParams(id, estado))
    }
}