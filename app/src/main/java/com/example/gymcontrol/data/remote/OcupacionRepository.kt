package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

@Serializable
data class OcupacionRow(val hora: String, val personas: Int)

object OcupacionRepository {
    suspend fun hoy(): List<OcupacionRow> =
        SupabaseProvider.client.from("ocupacion_por_hora")
            .select { order("hora", Order.ASCENDING) }
            .decodeList<OcupacionRow>()
}