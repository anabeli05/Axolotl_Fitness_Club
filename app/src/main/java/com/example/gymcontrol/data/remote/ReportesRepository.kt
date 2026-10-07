package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

@Serializable
data class ReporteRow(
    val periodo: String,
    val servicios: Double,
    val asesorias: Double,
    val gastos: Double
)

object ReportesRepository {
    private val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    // "2026-09" -> "Septiembre 2026"
    fun nombrePeriodo(p: String): String {
        val partes = p.split("-")
        return "${meses[partes[1].toInt() - 1]} ${partes[0]}"
    }

    suspend fun listar(): List<ReporteRow> =
        SupabaseProvider.client.from("reporte_mensual")
            .select { order("periodo", Order.ASCENDING) }
            .decodeList<ReporteRow>()
}
