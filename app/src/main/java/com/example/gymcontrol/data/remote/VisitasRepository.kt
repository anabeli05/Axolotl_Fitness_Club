package com.example.gymcontrol.data.remote

import com.example.gymcontrol.ui.components.SesionActual
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VisitaRow(
    val id: Long,
    val nombre: String,
    val telefono: String,
    val monto: Double,
    @SerialName("metodo_pago") val metodo: String,
    val hora: String,
    val registro: String? = null
)

@Serializable
private data class RegistrarVisitaParams(
    val p_actor_gmail: String,
    val p_nombre: String,
    val p_telefono: String,
    val p_contacto_nombre: String,
    val p_contacto_telefono: String,
    val p_metodo: String
)

/**
 * Visitas (pase de un día): no son membresías ni salen en la app del cliente.
 * El precio es el del servicio marcado como "Visita" en Servicios; Recepción o el
 * Encargado las cobran y el cobro entra solo en los ingresos.
 */
object VisitasRepository {
    private val actor get() = SesionActual.correo.orEmpty()

    /** Cobra la visita al precio del servicio de visita. */
    suspend fun registrar(
        nombre: String, telefono: String, contactoNombre: String, contactoTelefono: String, metodo: String
    ) {
        SupabaseProvider.client.postgrest.rpc(
            "registrar_visita",
            RegistrarVisitaParams(actor, nombre.trim(), telefono, contactoNombre.trim(), contactoTelefono, metodo)
        )
    }

    suspend fun hoy(): List<VisitaRow> =
        SupabaseProvider.client.from("visitas_hoy").select().decodeList<VisitaRow>()
}
