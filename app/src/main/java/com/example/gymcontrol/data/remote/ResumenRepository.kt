package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
    // "2026-09" -> "Sep" (localized according to locale)
    fun mesCorto(periodo: String, locale: Locale = Locale.getDefault()): String {
        val parts = periodo.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val date = LocalDate.of(year, month, 1)
        return date.format(DateTimeFormatter.ofPattern("MMM", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    // 1 -> "Lun" / "Mon" … 7 -> "Dom" / "Sun" (localized according to locale)
    fun diaCorto(diaSemana: Int, locale: Locale = Locale.getDefault()): String {
        val dayOfWeek = java.time.DayOfWeek.of(diaSemana.coerceIn(1, 7))
        return dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

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
