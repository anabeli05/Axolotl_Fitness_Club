package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResumenRow(
    val activos: Int,
    val inactivos: Int,
    @SerialName("ingresos_mes") val ingresosMes: Double
)

object ResumenRepository {
    suspend fun obtener(): ResumenRow =
        SupabaseProvider.client.from("resumen_dashboard")
            .select()
            .decodeSingle<ResumenRow>()
}