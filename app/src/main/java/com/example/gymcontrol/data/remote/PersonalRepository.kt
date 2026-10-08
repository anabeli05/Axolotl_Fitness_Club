package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
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
private data class EditarPersonalParams(
    val p_actor_gmail: String, val p_id: Long, val p_nombre: String,
    val p_gmail: String, val p_rol: String, val p_estado: String
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

    suspend fun editar(actorGmail: String, id: Long, nombre: String, gmail: String, rol: String, estado: String) {
        SupabaseProvider.client.postgrest.rpc(
            "editar_personal",
            EditarPersonalParams(actorGmail, id, nombre, gmail, rol, estado)
        )
    }
    suspend fun eliminar(actorGmail: String, id: Long) {
        SupabaseProvider.client.postgrest.rpc(
            "eliminar_personal",
            EliminarPersonalParams(actorGmail, id)
        )
    }
}