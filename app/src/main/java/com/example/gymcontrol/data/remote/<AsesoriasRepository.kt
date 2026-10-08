package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstructorRow(
    val id: Long,
    val nombre: String,
    val gmail: String = "",
    val costo: Double = 0.0,
    @SerialName("max_clientes") val maxClientes: Int = 10,
    val activos: Int = 0
)

@Serializable
data class SolicitudRow(
    @SerialName("r_instructor_id") val instructorId: Long,
    @SerialName("r_estado") val estado: String
)

@Serializable
private data class MisSolicitudesParams(val p_gmail: String)

@Serializable
private data class SolicitudParams(val p_gmail: String, val p_instructor_id: Long)

object AsesoriasRepository {
    suspend fun instructores(): List<InstructorRow> =
        SupabaseProvider.client.from("instructores_lista")
            .select { order("nombre", Order.ASCENDING) }
            .decodeList<InstructorRow>()

    suspend fun misSolicitudes(gmail: String): List<SolicitudRow> =
        SupabaseProvider.client.postgrest
            .rpc("mis_solicitudes", MisSolicitudesParams(gmail))
            .decodeList<SolicitudRow>()

    suspend fun solicitar(gmail: String, instructorId: Long) {
        SupabaseProvider.client.postgrest
            .rpc("solicitar_asesoria", SolicitudParams(gmail, instructorId))
    }

    suspend fun cancelar(gmail: String, instructorId: Long) {
        SupabaseProvider.client.postgrest
            .rpc("cancelar_solicitud", SolicitudParams(gmail, instructorId))
    }
}