package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClienteRow(
    val id: Long,
    val nombre: String,
    val gmail: String,
    val telefono: String = "",
    @SerialName("fecha_registro") val fechaRegistro: String,
    @SerialName("codigo_qr") val codigoQr: String? = null,
    val plan: String? = null,
    @SerialName("fecha_fin") val fechaFin: String? = null,
    val activa: Boolean = false,
    @SerialName("numero_membresia") val numeroMembresia: String = "",
    val estado: String = "ACTIVO"
)

@Serializable
data class PlanRow(val nombre: String, val precio: Double = 0.0)

@Serializable
data class ClienteCreado(
    @SerialName("r_id") val id: Long,
    @SerialName("r_numero") val numero: String,
    @SerialName("r_qr") val qr: String? = null,
    @SerialName("r_vence") val vence: String? = null
)

@Serializable
private data class CrearClienteParams(
    val p_nombre: String, val p_telefono: String, val p_gmail: String,
    val p_plan: String, val p_registrado_por: String
)

@Serializable
private data class EditarClienteParams(
    val p_id: Long, val p_nombre: String, val p_telefono: String,
    val p_gmail: String, val p_estado: String
)

@Serializable
private data class RenovarParams(val p_cliente_id: Long, val p_plan: String, val p_registrado_por: String)

object ClientesRepository {
    fun fechaApp(f: String?): String {
        if (f.isNullOrBlank()) return "—"
        val p = f.take(10).split("-")
        return if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else f
    }

    suspend fun listar(): List<ClienteRow> =
        SupabaseProvider.client.from("clientes_lista")
            .select { order("nombre", Order.ASCENDING) }
            .decodeList<ClienteRow>()

    suspend fun planes(): List<PlanRow> =
        SupabaseProvider.client.from("tipos_membresia")
            .select { order("precio", Order.DESCENDING) }
            .decodeList<PlanRow>()

    suspend fun crear(nombre: String, telefono: String, gmail: String, plan: String, registradoPor: String): ClienteCreado =
        SupabaseProvider.client.postgrest.rpc(
            "crear_cliente",
            CrearClienteParams(nombre, telefono, gmail, plan, registradoPor)
        ).decodeList<ClienteCreado>().first()

    suspend fun editar(id: Long, nombre: String, telefono: String, gmail: String, estado: String) {
        SupabaseProvider.client.postgrest.rpc(
            "editar_cliente",
            EditarClienteParams(id, nombre, telefono, gmail, estado)
        )
    }

    /** Devuelve la nueva fecha de vencimiento (yyyy-MM-dd). */
    suspend fun renovar(clienteId: Long, plan: String, registradoPor: String): String =
        SupabaseProvider.client.postgrest.rpc(
            "renovar_membresia",
            RenovarParams(clienteId, plan, registradoPor)
        ).decodeAs<String>()
}