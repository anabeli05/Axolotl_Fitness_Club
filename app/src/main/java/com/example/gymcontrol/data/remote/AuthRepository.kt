package com.example.gymcontrol.data.remote

import com.example.gymcontrol.data.model.UserRole
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRow(
    val id: Long,
    val nombre: String,
    val gmail: String,
    val rol: String,
    @SerialName("codigo_qr") val codigoQr: String? = null
)

@Serializable
private data class LoginParams(
    val p_gmail: String,
    val p_password: String,
    val p_rol: String
)

object AuthRepository {
    suspend fun login(identifier: String, password: String, role: UserRole): LoginRow? {
        return SupabaseProvider.client.postgrest
            .rpc("login", LoginParams(identifier, password, role.name))
            .decodeList<LoginRow>()
            .firstOrNull()
    }
}