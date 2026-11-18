package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Fila de la vista movimientos_por_rol (sql/auditoria.sql). */
@Serializable
data class MovimientoRolRow(
    val id: Long,
    val rol: String,
    @SerialName("usuario_id") val usuarioId: Long? = null,
    val usuario: String,
    val gmail: String? = null,
    @SerialName("tipo_accion") val tipoAccion: String,
    val modulo: String,
    val descripcion: String? = null,
    @SerialName("valores_anteriores") val valoresAnteriores: JsonElement? = null,
    @SerialName("valores_nuevos") val valoresNuevos: JsonElement? = null,
    @SerialName("fecha_hora") val fechaHora: String? = null,
    @SerialName("fecha_hora_mx") val fechaHoraMx: String
)

@Serializable
private data class MovimientosParams(
    val p_actor_gmail: String,
    val p_rol: String?,
    val p_limite: Int,
    val p_offset: Int
)

/** Bitácora de movimientos por rol. Solo el Encargado (lo valida movimientos_de_rol). */
object MovimientosRepository {
    const val BLOQUE = 50

    suspend fun listar(
        actorGmail: String,
        rol: String?,
        offset: Int = 0,
        limite: Int = BLOQUE
    ): List<MovimientoRolRow> =
        SupabaseProvider.client.postgrest
            .rpc(
                "movimientos_de_rol",
                MovimientosParams(actorGmail, rol, limite, offset)
            )
            .decodeList<MovimientoRolRow>()
}
