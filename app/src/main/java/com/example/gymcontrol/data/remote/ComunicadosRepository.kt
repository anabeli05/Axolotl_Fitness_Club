package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Tipos de comunicado y los días mínimos de anticipación (Términos punto 25, Política punto 12). */
enum class TipoComunicado(val diasMinimos: Int) { TERMINOS(15), PRIVACIDAD(15), HORARIO(3) }

@Serializable
data class ComunicadoRow(
    val id: Long,
    val tipo: String,
    val titulo: String,
    val mensaje: String,
    @SerialName("fecha_vigor") val fechaVigor: String,
    @SerialName("creado_en") val creadoEn: String,
    val publico: String? = null,
    val destinatarios: Int = 0,
    val enviados: Int = 0
)

@Serializable
private data class CrearComunicadoParams(
    val p_actor_gmail: String, val p_tipo: String, val p_titulo: String,
    val p_mensaje: String, val p_fecha_vigor: String
)

/** sql/comunicados.sql + Edge Function enviar-comunicados. */
object ComunicadosRepository {
    suspend fun listar(): List<ComunicadoRow> =
        SupabaseProvider.client.from("comunicados_lista")
            .select { order("creado_en", Order.DESCENDING) }
            .decodeList<ComunicadoRow>()

    /** Solo el Encargado. [fechaVigor] en yyyy-MM-dd. Devuelve el id del comunicado. */
    suspend fun crear(actorGmail: String, tipo: TipoComunicado, titulo: String, mensaje: String, fechaVigor: String): Long =
        SupabaseProvider.client.postgrest
            .rpc("crear_comunicado", CrearComunicadoParams(actorGmail, tipo.name, titulo.trim(), mensaje.trim(), fechaVigor))
            .decodeAs<Long>()
}
