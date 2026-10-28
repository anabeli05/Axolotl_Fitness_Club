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
object PersonalRepository {
    suspend fun listar(): List<PersonalRow> =
        SupabaseProvider.client.from("personal_lista")
            .select { order("id", Order.ASCENDING) }
            .decodeList<PersonalRow>()

    suspend fun crear(nombre: String, telefono: String, gmail: String, rol: String, costo: Double?, capacidad: Int?) {
        SupabaseProvider.client.postgrest.rpc(
            "crear_personal",
            CrearPersonalParams(nombre, telefono, gmail, rol, costo, capacidad)
        )
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