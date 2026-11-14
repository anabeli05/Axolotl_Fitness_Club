package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReporteRow(
    val periodo: String,
    val servicios: Double,
    val asesorias: Double,
    val gastos: Double
)

/** Cancelación de membresía (con o sin reembolso), de la vista cancelaciones_lista. */
@Serializable
data class CancelacionRow(
    val tipo: String,
    val id: Long,
    @SerialName("cancelada_en") val canceladaEn: String? = null,
    val cliente: String,
    @SerialName("numero_membresia") val numeroMembresia: String? = null,
    val detalle: String? = null,
    val motivo: String? = null,
    val autorizo: String? = null,
    val reembolso: Double = 0.0,
    @SerialName("tipo_cancelacion") val tipoCancelacion: String? = null,
    @SerialName("sigue_activa") val sigueActiva: Boolean = false,
    @SerialName("fecha_fin") val fechaFin: String? = null,
    @SerialName("reembolso_id") val reembolsoId: Long? = null,
    @SerialName("estado_reembolso") val estadoReembolso: String? = null,
    @SerialName("metodo_reembolso") val metodoReembolso: String? = null,
    @SerialName("fecha_limite") val fechaLimite: String? = null,
    @SerialName("entregado_en") val entregadoEn: String? = null,
    val solicito: String? = null
) {
    val reembolsoPendiente get() = reembolsoId != null && estadoReembolso == "PENDIENTE"
}

/** Solicitud pendiente, con lo que aplicaría si el Encargado autoriza ahora. */
@Serializable
data class SolicitudCancelacionRow(
    val id: Long,
    @SerialName("cliente_id") val clienteId: Long,
    val cliente: String,
    @SerialName("numero_membresia") val numeroMembresia: String? = null,
    val tipo: String,
    val motivo: String,
    @SerialName("solicitada_en") val solicitadaEn: String,
    val solicito: String? = null,
    val plan: String? = null,
    @SerialName("fecha_fin") val fechaFin: String? = null,
    val horas: Double? = null,
    val reembolso: Double = 0.0,
    val metodo: String? = null,
    val regla: String,
    @SerialName("fecha_limite") val fechaLimite: String? = null
)

@Serializable
private data class ResolverCancelacionParams(
    val p_actor_gmail: String, val p_solicitud_id: Long, val p_autorizar: Boolean, val p_nota: String?
)

@Serializable
private data class EntregarReembolsoParams(val p_actor_gmail: String, val p_pago_id: Long)

object ReportesRepository {
    private val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    // "2026-09" -> "Septiembre 2026"
    fun nombrePeriodo(p: String): String {
        val partes = p.split("-")
        return "${meses[partes[1].toInt() - 1]} ${partes[0]}"
    }

    /** Últimos 6 meses, el mes actual primero. */
    suspend fun listar(): List<ReporteRow> =
        SupabaseProvider.client.from("reporte_mensual")
            .select { order("periodo", Order.DESCENDING) }
            .decodeList<ReporteRow>()

    /** Cancelaciones de membresía, las más recientes primero. */
    suspend fun cancelaciones(): List<CancelacionRow> =
        SupabaseProvider.client.from("cancelaciones_lista")
            .select {
                filter { eq("tipo", "MEMBRESIA") }
                order("cancelada_en", Order.DESCENDING)
            }
            .decodeList<CancelacionRow>()

    /** Solicitudes de cancelación por autorizar, las más antiguas primero. */
    suspend fun solicitudesCancelacion(): List<SolicitudCancelacionRow> =
        SupabaseProvider.client.from("solicitudes_cancelacion_pendientes")
            .select { order("solicitada_en", Order.ASCENDING) }
            .decodeList<SolicitudCancelacionRow>()

    /** Solo el Encargado. Al rechazar, [nota] es el motivo (obligatorio). */
    suspend fun resolverCancelacion(actorGmail: String, solicitudId: Long, autorizar: Boolean, nota: String?) {
        SupabaseProvider.client.postgrest.rpc(
            "resolver_cancelacion",
            ResolverCancelacionParams(actorGmail, solicitudId, autorizar, nota?.trim())
        )
    }

    /** Solo el Encargado. Desde ese momento el reembolso resta en los reportes. */
    suspend fun entregarReembolso(actorGmail: String, pagoId: Long) {
        SupabaseProvider.client.postgrest
            .rpc("entregar_reembolso", EntregarReembolsoParams(actorGmail, pagoId))
    }
}
