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

/** Métodos de pago. Efectivo y Tarjeta se confirman al registrarlos; Transferencia la confirma el Encargado. */
val METODOS_PAGO = listOf("Efectivo", "Tarjeta", "Transferencia")

@Serializable
data class ContactoEmergencia(
    @SerialName("r_nombre") val nombre: String = "",
    @SerialName("r_telefono") val telefono: String = ""
)

@Serializable
data class PagoPendienteRow(
    val id: Long,
    val monto: Double,
    @SerialName("metodo_pago") val metodo: String,
    @SerialName("fecha_pago") val fecha: String,
    @SerialName("cliente_id") val clienteId: Long,
    val cliente: String,
    @SerialName("numero_membresia") val numeroMembresia: String = "",
    val plan: String? = null,
    @SerialName("tiene_comprobante") val tieneComprobante: Boolean = false
)

@Serializable
private data class CrearClienteParams(
    val p_nombre: String, val p_telefono: String, val p_gmail: String,
    val p_plan: String, val p_registrado_por: String,
    val p_contacto_nombre: String, val p_contacto_telefono: String, val p_metodo: String
)

@Serializable
private data class EditarClienteParams(
    val p_id: Long, val p_nombre: String, val p_telefono: String,
    val p_gmail: String, val p_estado: String,
    val p_contacto_nombre: String? = null, val p_contacto_telefono: String? = null
)

@Serializable
private data class RenovarParams(
    val p_cliente_id: Long, val p_plan: String, val p_registrado_por: String, val p_metodo: String
)

@Serializable
private data class ContactoParams(val p_actor_gmail: String, val p_cliente_id: Long)

@Serializable
private data class GmailParams(val p_gmail: String)

@Serializable
private data class MiContactoParams(val p_gmail: String, val p_nombre: String, val p_telefono: String)

@Serializable
private data class ConfirmarPagoParams(val p_actor_gmail: String, val p_pago_id: Long, val p_aceptar: Boolean)

@Serializable
private data class CancelarMembresiaParams(
    val p_actor_gmail: String, val p_password: String, val p_cliente_id: Long,
    val p_motivo: String, val p_reembolso: Double
)

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

    /** Datos del cliente que inició sesión (se busca por su correo). */
    suspend fun porCorreo(gmail: String): ClienteRow? =
        SupabaseProvider.client.from("clientes_lista")
            .select { filter { eq("gmail", gmail.trim()) } }
            .decodeList<ClienteRow>()
            .firstOrNull()

    suspend fun planes(): List<PlanRow> =
        SupabaseProvider.client.from("tipos_membresia")
            .select { order("precio", Order.DESCENDING) }
            .decodeList<PlanRow>()

    /** r_vence llega en null si el pago es por transferencia y queda pendiente. */
    suspend fun crear(
        nombre: String, telefono: String, gmail: String, plan: String, registradoPor: String,
        contactoNombre: String = "", contactoTelefono: String = "", metodo: String = "Efectivo"
    ): ClienteCreado =
        SupabaseProvider.client.postgrest.rpc(
            "crear_cliente",
            CrearClienteParams(nombre, telefono, gmail, plan, registradoPor, contactoNombre, contactoTelefono, metodo)
        ).decodeList<ClienteCreado>().first()

    /** contacto* en null = se conserva el contacto de emergencia que ya tenía. */
    suspend fun editar(
        id: Long, nombre: String, telefono: String, gmail: String, estado: String,
        contactoNombre: String? = null, contactoTelefono: String? = null
    ) {
        SupabaseProvider.client.postgrest.rpc(
            "editar_cliente",
            EditarClienteParams(id, nombre, telefono, gmail, estado, contactoNombre, contactoTelefono)
        )
    }

    /** Nueva fecha de vencimiento (yyyy-MM-dd), o null si queda pendiente de confirmar. */
    suspend fun renovar(clienteId: Long, plan: String, registradoPor: String, metodo: String = "Efectivo"): String? =
        SupabaseProvider.client.postgrest.rpc(
            "renovar_membresia",
            RenovarParams(clienteId, plan, registradoPor, metodo)
        ).decodeAs<String?>()

    /** Solo Recepción y Encargado (la base lo valida). */
    suspend fun contactoEmergencia(actorGmail: String, clienteId: Long): ContactoEmergencia =
        SupabaseProvider.client.postgrest
            .rpc("contacto_emergencia_cliente", ContactoParams(actorGmail, clienteId))
            .decodeList<ContactoEmergencia>()
            .firstOrNull() ?: ContactoEmergencia()

    /** Foto de perfil (JPEG en base64) del cliente con ese correo; null si aún no tiene. */
    suspend fun fotoPerfil(gmail: String): String? =
        SupabaseProvider.client.postgrest
            .rpc("foto_cliente", GmailParams(gmail.trim()))
            .decodeAs<String?>()

    /** Contacto de emergencia del cliente que inició sesión. */
    suspend fun miContacto(gmail: String): ContactoEmergencia =
        SupabaseProvider.client.postgrest
            .rpc("mi_contacto_emergencia", GmailParams(gmail.trim()))
            .decodeList<ContactoEmergencia>()
            .firstOrNull() ?: ContactoEmergencia()

    /** El cliente cambia su propio contacto de emergencia (nombre y teléfono obligatorios). */
    suspend fun editarMiContacto(gmail: String, nombre: String, telefono: String) {
        SupabaseProvider.client.postgrest
            .rpc("editar_mi_contacto", MiContactoParams(gmail.trim(), nombre.trim(), telefono))
    }

    suspend fun pagosPendientes(): List<PagoPendienteRow> =
        SupabaseProvider.client.from("pagos_pendientes")
            .select { order("fecha_pago", Order.ASCENDING) }
            .decodeList<PagoPendienteRow>()

    /** Al aceptar, la vigencia empieza ese día. Devuelve el vencimiento o null si se rechazó. */
    suspend fun confirmarPago(actorGmail: String, pagoId: Long, aceptar: Boolean): String? =
        SupabaseProvider.client.postgrest
            .rpc("confirmar_pago", ConfirmarPagoParams(actorGmail, pagoId, aceptar))
            .decodeAs<String?>()

    /** Solo el Encargado, con su contraseña. */
    suspend fun cancelarMembresia(
        actorGmail: String, password: String, clienteId: Long, motivo: String, reembolso: Double
    ) {
        SupabaseProvider.client.postgrest.rpc(
            "cancelar_membresia",
            CancelarMembresiaParams(actorGmail, password, clienteId, motivo, reembolso)
        )
    }
}