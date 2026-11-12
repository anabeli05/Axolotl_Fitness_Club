package com.example.gymcontrol.data.remote

import com.example.gymcontrol.ui.components.SesionActual
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable

@Serializable
data class GastoRow(val id: Long, val concepto: String, val categoria: String, val monto: Double, val fecha: String)

@Serializable
private data class GastoActor(val p_actor_gmail: String)

@Serializable
private data class GastoCrear(
    val p_actor_gmail: String, val p_concepto: String, val p_categoria: String,
    val p_monto: Double, val p_fecha: String
)

@Serializable
private data class GastoEditar(
    val p_actor_gmail: String, val p_id: Long, val p_concepto: String, val p_categoria: String,
    val p_monto: Double, val p_fecha: String
)

@Serializable
private data class GastoId(val p_actor_gmail: String, val p_id: Long)

object GastosRepository {
    private val actor get() = SesionActual.correo ?: ""

    // La app usa dd/MM/yyyy y la base de datos aaaa-mm-dd
    fun aBd(f: String): String {
        val p = f.split("/")
        return "${p[2]}-${p[1]}-${p[0]}"
    }

    fun deBd(f: String): String {
        val p = f.split("-")
        return "${p[2]}/${p[1]}/${p[0]}"
    }

    suspend fun listar(): List<GastoRow> =
        SupabaseProvider.client.postgrest
            .rpc("listar_gastos", GastoActor(actor))
            .decodeList<GastoRow>()

    suspend fun crear(concepto: String, categoria: String, monto: Double, fecha: String) {
        SupabaseProvider.client.postgrest.rpc(
            "crear_gasto", GastoCrear(actor, concepto, categoria, monto, aBd(fecha))
        )
    }

    suspend fun actualizar(id: Long, concepto: String, categoria: String, monto: Double, fecha: String) {
        SupabaseProvider.client.postgrest.rpc(
            "actualizar_gasto", GastoEditar(actor, id, concepto, categoria, monto, aBd(fecha))
        )
    }

    suspend fun eliminar(id: Long) {
        SupabaseProvider.client.postgrest.rpc("eliminar_gasto", GastoId(actor, id))
    }
}