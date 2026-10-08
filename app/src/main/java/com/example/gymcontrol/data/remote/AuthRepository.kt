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

@Serializable
private data class LoginAutoParams(val p_gmail: String, val p_password: String)

@Serializable
private data class CambiarParams(val p_gmail: String, val p_actual: String, val p_nueva: String)

object AuthRepository {
    suspend fun login(identifier: String, password: String, role: UserRole): LoginRow? {
        return SupabaseProvider.client.postgrest
            .rpc("login", LoginParams(identifier, password, role.name))
            .decodeList<LoginRow>()
            .firstOrNull()
    }

    // Verifica la contraseña una sola vez y devuelve el rol del usuario
    suspend fun loginAuto(identifier: String, password: String): LoginRow? =
        SupabaseProvider.client.postgrest
            .rpc("login_auto", LoginAutoParams(identifier.trim(), password))
            .decodeList<LoginRow>()
            .firstOrNull()

    suspend fun cambiarContrasena(gmail: String, actual: String, nueva: String) {
        SupabaseProvider.client.postgrest.rpc(
            "cambiar_contrasena",
            CambiarParams(gmail.trim(), actual, nueva)
        )
    }
}