package com.example.gymcontrol.data.remote

import com.example.gymcontrol.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object CorreoRepository {
    suspend fun enviarBienvenida(gmail: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/enviar-bienvenida")
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.connectTimeout = 15000
            con.readTimeout = 30000
            con.doOutput = true
            con.setRequestProperty("Content-Type", "application/json")
            con.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
            con.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_KEY}")
            con.outputStream.use { it.write(JSONObject().put("gmail", gmail).toString().toByteArray()) }
            val ok = con.responseCode in 200..299
            con.disconnect()
            ok
        } catch (e: Exception) { false }
    }
}