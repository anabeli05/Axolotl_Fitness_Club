package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResumenRow(
    val activos: Int,
    val inactivos: Int,
    @SerialName("ingresos_mes") val ingresosMes: Double
)

@Serializable
data class ClientesMesRow(val periodo: String, val activos: Int, val inactivos: Int)

@Serializable
data class IngresoDiaRow(
    val fecha: String,
    @SerialName("dia_semana") val diaSemana: Int,
    val total: Double
)

object ResumenRepository {
    private val mesesCortos = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
    private val diasCortos = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    // "2026-09" -> "Sep"
    fun mesCorto(periodo: String): String =
        periodo.split("-").getOrNull(1)?.toIntOrNull()?.let { mesesCortos.getOrNull(it - 1) } ?: periodo

    // 1 -> "Lun" … 7 -> "Dom"
    fun diaCorto(diaSemana: Int): String = diasCortos.getOrElse(diaSemana - 1) { "" }

    suspend fun obtener(): ResumenRow =
        SupabaseProvider.client.from("resumen_dashboard")
            .select()
            .decodeSingle<ResumenRow>()

    /** Activos/inactivos de los últimos 12 meses (el último es el mes actual). */
    suspend fun clientesPorMes(): List<ClientesMesRow> =
        SupabaseProvider.client.from("clientes_por_mes")
            .select { order("periodo", Order.ASCENDING) }
            .decodeList<ClientesMesRow>()

    /** Ingresos de cada día de la semana actual (lunes a domingo). */
    suspend fun ingresosSemana(): List<IngresoDiaRow> =
        SupabaseProvider.client.from("ingresos_semana")
            .select { order("fecha", Order.ASCENDING) }
            .decodeList<IngresoDiaRow>()
}
