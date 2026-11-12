package com.example.gymcontrol.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream

@Serializable
data class MiComprobanteRow(
    @SerialName("r_id") val id: Long,
    @SerialName("r_plan") val plan: String? = null,
    @SerialName("r_monto") val monto: Double = 0.0,
    @SerialName("r_metodo") val metodo: String = "",
    @SerialName("r_estado") val estado: String,
    @SerialName("r_enviado") val enviado: String? = null,
    @SerialName("r_resuelto") val resuelto: String? = null,
    @SerialName("r_motivo") val motivo: String? = null
)

@Serializable
data class PlanPagoRow(
    val nombre: String,
    @SerialName("duracion_dias") val duracionDias: Int = 1,
    val precio: Double
)

@Serializable
private data class EnviarComprobanteParams(
    val p_gmail: String, val p_plan: String, val p_metodo: String, val p_comprobante: String
)

@Serializable
private data class ComprobanteParams(val p_actor_gmail: String, val p_pago_id: Long)

@Serializable
private data class RechazarParams(val p_actor_gmail: String, val p_pago_id: Long, val p_motivo: String)

@Serializable
private data class GmailComprobanteParams(val p_gmail: String)

/**
 * Comprobantes de pago que sube el cliente (ver sql/comprobantes_pago.sql).
 * Aceptar se hace con [ClientesRepository.confirmarPago]; rechazar, con [rechazar] (lleva motivo).
 */
object ComprobantesRepository {
    private const val LADO_MAX = 1400   // px del lado mayor; se lee bien y no pesa demasiado
    private const val CALIDAD = 80

    /** Lee la imagen elegida, la reduce y la devuelve como JPEG en base64. */
    suspend fun imagenABase64(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        var muestreo = 1
        while (maxOf(opciones.outWidth, opciones.outHeight) / (muestreo * 2) >= LADO_MAX) muestreo *= 2

        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestreo })
        } ?: throw IllegalArgumentException("No se pudo leer la imagen")

        val escala = minOf(1f, LADO_MAX.toFloat() / maxOf(bitmap.width, bitmap.height))
        val final = if (escala < 1f)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true)
        else bitmap

        val salida = ByteArrayOutputStream()
        final.compress(Bitmap.CompressFormat.JPEG, CALIDAD, salida)
        Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
    }

    /** Planes que el cliente puede pagar: los de "Servicios", con su precio actual. */
    suspend fun planesPago(): List<PlanPagoRow> =
        SupabaseProvider.client.from("planes_pago")
            .select { order("duracion_dias", Order.ASCENDING) }
            .decodeList<PlanPagoRow>()

    /** El cliente envía su comprobante; queda PENDIENTE hasta que el Encargado lo valide. */
    suspend fun enviar(gmail: String, plan: String, metodo: String, comprobanteBase64: String) {
        SupabaseProvider.client.postgrest.rpc(
            "enviar_comprobante",
            EnviarComprobanteParams(gmail.trim(), plan, metodo, comprobanteBase64)
        )
    }

    /** Último comprobante del cliente (null si nunca ha enviado uno). */
    suspend fun miUltimo(gmail: String): MiComprobanteRow? =
        SupabaseProvider.client.postgrest
            .rpc("mi_ultimo_comprobante", GmailComprobanteParams(gmail.trim()))
            .decodeList<MiComprobanteRow>()
            .firstOrNull()

    /** El Encargado rechaza el pago; el cliente verá [motivo] en su pestaña Pagos. */
    suspend fun rechazar(actorGmail: String, pagoId: Long, motivo: String) {
        SupabaseProvider.client.postgrest.rpc(
            "rechazar_pago",
            RechazarParams(actorGmail.trim(), pagoId, motivo.trim())
        )
    }

    /** Imagen (base64) del comprobante de un pago. Solo el Encargado. */
    suspend fun imagen(actorGmail: String, pagoId: Long): String? =
        SupabaseProvider.client.postgrest
            .rpc("comprobante_pago", ComprobanteParams(actorGmail.trim(), pagoId))
            .decodeAs<String?>()
}
