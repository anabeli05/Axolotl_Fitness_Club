package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AsesoriaClienteRow(
    @SerialName("r_id") val id: Long,
    @SerialName("r_nombre") val nombre: String,
    @SerialName("r_dias") val dias: String = "",
    @SerialName("r_objetivo") val objetivo: String = ""
)

@Serializable
private data class MisClientesParams(val p_gmail: String)

@Serializable
private data class GuardarAsesoriaParams(
    val p_gmail: String,
    val p_id: Long,
    val p_dias: String,
    val p_objetivo: String
)

object InstructorRepository {
    suspend fun misClientes(gmail: String): List<AsesoriaClienteRow> =
        SupabaseProvider.client.postgrest
            .rpc("mis_clientes_instructor", MisClientesParams(gmail))
            .decodeList<AsesoriaClienteRow>()

    suspend fun guardar(gmail: String, id: Long, dias: String, objetivo: String) {
        SupabaseProvider.client.postgrest
            .rpc("guardar_asesoria", GuardarAsesoriaParams(gmail, id, dias, objetivo))
    }
}