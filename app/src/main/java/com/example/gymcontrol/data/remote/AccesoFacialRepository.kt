package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Resultado de un movimiento.
 * resultado: ENTRADA, SALIDA, DENEGADO, REPETIDO, YA_DENTRO, SIN_ENTRADA o NO_RECONOCIDO.
 * esEntrada: true (1) = entrada · false (0) = salida.
 */
@Serializable
data class MovimientoRow(
    @SerialName("r_resultado") val resultado: String,
    @SerialName("r_cliente_id") val clienteId: Long? = null,
    @SerialName("r_nombre") val nombre: String? = null,
    @SerialName("r_membresia") val membresia: String? = null,
    @SerialName("r_activa") val activa: Boolean? = null,
    @SerialName("r_es_entrada") val esEntrada: Boolean? = null,
    @SerialName("r_fecha_hora") val fechaHora: String? = null,
    @SerialName("r_entrada_desde") val entradaDesde: String? = null,
    @SerialName("r_dentro_total") val dentroTotal: Int = 0,
    @SerialName("r_similitud") val similitud: Float? = null
)

@Serializable
data class RostroRegistradoRow(@SerialName("cliente_id") val clienteId: Long)

@Serializable
data class ClienteDentroRow(
    @SerialName("cliente_id") val clienteId: Long,
    val desde: String
)

@Serializable
private data class AccesoParams(val p_embedding: List<Float>)

@Serializable
private data class ManualParams(val p_cliente_id: Long, val p_es_entrada: Boolean)

@Serializable
private data class RegistrarRostroParams(val p_cliente_id: Long, val p_embedding: List<Float>)

/** Funciones de sql/reconocimiento_facial.sql. Solo viaja el vector del rostro, nunca la imagen. */
object AccesoFacialRepository {

    /** Identifica el rostro y registra ENTRADA (1) o SALIDA (0) automáticamente. */
    suspend fun acceso(vector: FloatArray): MovimientoRow =
        SupabaseProvider.client.postgrest
            .rpc("acceso_por_rostro", AccesoParams(vector.toList()))
            .decodeList<MovimientoRow>()
            .first()

    /** Botones Entrada / Salida de Recepción. */
    suspend fun manual(clienteId: Long, esEntrada: Boolean): MovimientoRow =
        SupabaseProvider.client.postgrest
            .rpc("registrar_movimiento_manual", ManualParams(clienteId, esEntrada))
            .decodeList<MovimientoRow>()
            .first()

    suspend fun registrarRostro(clienteId: Long, vector: FloatArray) {
        SupabaseProvider.client.postgrest
            .rpc("registrar_rostro_cliente", RegistrarRostroParams(clienteId, vector.toList()))
    }

    suspend fun clientesConRostro(): Set<Long> =
        SupabaseProvider.client.postgrest
            .rpc("clientes_rostro_registrado")
            .decodeList<RostroRegistradoRow>()
            .map { it.clienteId }
            .toSet()

    /** Personas dentro del gimnasio ahora (entradas − salidas de hoy). */
    suspend fun ocupacionActual(): Int =
        SupabaseProvider.client.postgrest
            .rpc("ocupacion_actual")
            .decodeAs<Int>()

    /** Clientes dentro ahora → hora de su entrada (ISO). */
    suspend fun clientesDentro(): Map<Long, String> =
        SupabaseProvider.client.postgrest
            .rpc("clientes_dentro")
            .decodeList<ClienteDentroRow>()
            .associate { it.clienteId to it.desde }

    /** Emite cada vez que se registra una entrada o salida (Supabase Realtime). */
    fun movimientos(): Flow<Unit> = channelFlow {
        val canal = SupabaseProvider.client.channel("registros_acceso_${System.nanoTime()}")
        canal.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "registros_acceso"
        }.onEach { send(Unit) }.launchIn(this)
        canal.subscribe()
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { SupabaseProvider.client.realtime.removeChannel(canal) }
        }
    }
}
