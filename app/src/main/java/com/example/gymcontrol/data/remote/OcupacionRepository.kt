package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OcupacionRow(val hora: String, val personas: Int)

object OcupacionRepository {
    suspend fun hoy(): List<OcupacionRow> =
        SupabaseProvider.client.from("ocupacion_por_hora")
            .select { order("hora", Order.ASCENDING) }
            .decodeList<OcupacionRow>()
}
@Serializable
data class AfluenciaRow(
    @SerialName("dia_semana") val diaSemana: Int,
    val hora: Int,
    val promedio: Double
)

/** Promedio real de entradas por día y hora (vista afluencia_semanal, últimos 28 días). */
object AfluenciaRepository {
    suspend fun semanal(): List<AfluenciaRow> =
        SupabaseProvider.client.from("afluencia_semanal")
            .select()
            .decodeList<AfluenciaRow>()
}
