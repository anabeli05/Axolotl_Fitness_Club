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

@Serializable
data class PerfilInstructorRow(
    @SerialName("r_id") val id: Long,
    @SerialName("r_nombre") val nombre: String,
    @SerialName("r_telefono") val telefono: String = "",
    @SerialName("r_gmail") val gmail: String,
    @SerialName("r_estado") val estado: String = "ACTIVO",
    @SerialName("r_max_clientes") val maxClientes: Int = 10,
    @SerialName("r_activos") val activos: Int = 0
)

@Serializable
private data class EditarPerfilParams(
    val p_gmail_actual: String,
    val p_nombre: String,
    val p_telefono: String,
    val p_gmail: String
)

object InstructorRepository {
    /** Datos del instructor que inició sesión (se busca por su correo). */
    suspend fun perfil(gmail: String): PerfilInstructorRow? =
        SupabaseProvider.client.postgrest
            .rpc("perfil_instructor", MisClientesParams(gmail))
            .decodeList<PerfilInstructorRow>()
            .firstOrNull()

    suspend fun editarPerfil(gmailActual: String, nombre: String, telefono: String, gmail: String) {
        SupabaseProvider.client.postgrest
            .rpc("editar_perfil_instructor", EditarPerfilParams(gmailActual, nombre, telefono, gmail))
    }

    suspend fun misClientes(gmail: String): List<AsesoriaClienteRow> =
        SupabaseProvider.client.postgrest
            .rpc("mis_clientes_instructor", MisClientesParams(gmail))
            .decodeList<AsesoriaClienteRow>()

    suspend fun guardar(gmail: String, id: Long, dias: String, objetivo: String) {
        SupabaseProvider.client.postgrest
            .rpc("guardar_asesoria", GuardarAsesoriaParams(gmail, id, dias, objetivo))
    }
}