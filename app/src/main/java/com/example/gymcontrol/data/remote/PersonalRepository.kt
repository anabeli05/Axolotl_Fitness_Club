package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalRow(
    val id: Long,
    val nombre: String,
    val gmail: String,
    val estado: String,
    val rol: String
)

@Serializable
private data class CrearPersonalParams(
    val p_nombre: String, val p_telefono: String, val p_gmail: String, val p_rol: String,
    val p_costo: Double? = null, val p_max_clientes: Int? = null
)

@Serializable
data class PersonalDetalleRow(
    @SerialName("r_telefono") val telefono: String = "",
    @SerialName("r_costo") val costo: Double? = null,
    @SerialName("r_max_clientes") val maxClientes: Int? = null
)

@Serializable
private data class DetallePersonalParams(val p_id: Long)

@Serializable
private data class EditarPersonalParams(
    val p_actor_gmail: String, val p_id: Long, val p_nombre: String,
    val p_gmail: String, val p_rol: String, val p_estado: String,
    val p_telefono: String? = null, val p_costo: Double? = null, val p_max_clientes: Int? = null
)
@Serializable
private data class EliminarPersonalParams(val p_actor_gmail: String, val p_id: Long)

@Serializable
data class PerfilPersonalRow(
    @SerialName("r_id") val id: Long,
    @SerialName("r_nombre") val nombre: String,
    @SerialName("r_gmail") val gmail: String,
    @SerialName("r_telefono") val telefono: String = "",
    @SerialName("r_estado") val estado: String = "ACTIVO",
    @SerialName("r_fecha_registro") val fechaRegistro: String? = null
)

@Serializable
private data class MiTelefonoParams(val p_gmail: String, val p_telefono: String)

@Serializable
private data class FotoUsuarioParams(val p_usuario_id: Long)

@Serializable
private data class FotoPersonalParams(val p_gmail: String)

@Serializable
private data class GuardarFotoParams(val p_actor_gmail: String, val p_usuario_id: Long, val p_foto: String)
object PersonalRepository {
    suspend fun listar(): List<PersonalRow> =
        SupabaseProvider.client.from("personal_lista")
            .select { order("id", Order.ASCENDING) }
            .decodeList<PersonalRow>()

    /** Devuelve el id del nuevo usuario (para tomarle su foto enseguida). */
    suspend fun crear(nombre: String, telefono: String, gmail: String, rol: String, costo: Double?, capacidad: Int?): Long =
        SupabaseProvider.client.postgrest.rpc(
            "crear_personal",
            CrearPersonalParams(nombre, telefono, gmail, rol, costo, capacidad)
        ).decodeAs<Long>()

    /** Foto de perfil (JPEG en base64) de un miembro del personal; null si no tiene. */
    suspend fun foto(usuarioId: Long): String? =
        SupabaseProvider.client.postgrest
            .rpc("foto_usuario", FotoUsuarioParams(usuarioId))
            .decodeAs<String?>()

    /** Foto del miembro del personal que inició sesión. */
    suspend fun fotoPorCorreo(gmail: String): String? =
        SupabaseProvider.client.postgrest
            .rpc("foto_personal", FotoPersonalParams(gmail.trim()))
            .decodeAs<String?>()

    /** Datos del miembro del personal que inició sesión (perfil de Recepción). */
    suspend fun miPerfil(gmail: String): PerfilPersonalRow? =
        SupabaseProvider.client.postgrest
            .rpc("perfil_personal", FotoPersonalParams(gmail.trim()))
            .decodeList<PerfilPersonalRow>()
            .firstOrNull()

    /** Recepción cambia su propio teléfono. */
    suspend fun editarMiTelefono(gmail: String, telefono: String) {
        SupabaseProvider.client.postgrest
            .rpc("editar_mi_telefono", MiTelefonoParams(gmail.trim(), telefono))
    }

    /** Solo el Encargado: toma o actualiza la foto de un miembro del personal. */
    suspend fun guardarFoto(actorGmail: String, usuarioId: Long, foto: String) {
        SupabaseProvider.client.postgrest
            .rpc("guardar_foto_personal", GuardarFotoParams(actorGmail, usuarioId, foto))
    }

    /** Teléfono, costo y capacidad (no vienen en personal_lista). */
    suspend fun detalle(id: Long): PersonalDetalleRow? =
        SupabaseProvider.client.postgrest
            .rpc("detalle_personal", DetallePersonalParams(id))
            .decodeList<PersonalDetalleRow>()
            .firstOrNull()

    // telefono, costo y capacidad en null = se conserva lo que ya había
    suspend fun editar(
        actorGmail: String, id: Long, nombre: String, gmail: String, rol: String, estado: String,
        telefono: String? = null, costo: Double? = null, capacidad: Int? = null
    ) {
        SupabaseProvider.client.postgrest.rpc(
            "editar_personal",
            EditarPersonalParams(actorGmail, id, nombre, gmail, rol, estado, telefono, costo, capacidad)
        )
    }
    suspend fun eliminar(actorGmail: String, id: Long) {
        SupabaseProvider.client.postgrest.rpc(
            "eliminar_personal",
            EliminarPersonalParams(actorGmail, id)
        )
    }
}