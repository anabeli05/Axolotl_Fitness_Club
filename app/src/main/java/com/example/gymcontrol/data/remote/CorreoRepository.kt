package com.example.gymcontrol.data.remote

import com.example.gymcontrol.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object CorreoRepository {
    suspend fun enviarBienvenida(gmail: String): Boolean =
        llamar("enviar-bienvenida", JSONObject().put("gmail", gmail)) == null

    /**
     * Manda al correo registrado del cliente el comprobante del pago que se acaba de
     * registrar (Edge Function enviar-recibo). null si el correo salió; si no, el motivo.
     */
    suspend fun enviarRecibo(clienteId: Long): String? =
        llamar("enviar-recibo", JSONObject().put("cliente_id", clienteId))

    /**
     * Manda un código de 6 dígitos al correo para cambiar la contraseña sin la actual
     * (Edge Function enviar-codigo-contrasena). null si se envió; si no, el motivo.
     */
    suspend fun enviarCodigoContrasena(gmail: String): String? =
        llamar("enviar-codigo-contrasena", JSONObject().put("gmail", gmail.trim()))

    /** null si respondió bien; si no, el motivo (campo "motivo" del JSON o el texto/código). */
    private suspend fun llamar(funcion: String, cuerpo: JSONObject): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/$funcion")
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.connectTimeout = 15000
            con.readTimeout = 30000
            con.doOutput = true
            con.setRequestProperty("Content-Type", "application/json")
            con.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
            con.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_KEY}")
            con.outputStream.use { it.write(cuerpo.toString().toByteArray()) }
            val codigo = con.responseCode
            if (codigo in 200..299) {
                con.disconnect()
                return@withContext null
            }
            val texto = con.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            con.disconnect()
            val motivo = try { JSONObject(texto).optString("motivo").ifBlank { JSONObject(texto).optString("message") } }
                         catch (_: Exception) { texto }
            motivo.ifBlank { "HTTP $codigo" }.take(300)
        } catch (e: Exception) { "Sin conexión con el servidor de correo (${e.message})" }
    }
}
