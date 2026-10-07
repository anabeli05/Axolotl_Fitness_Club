package com.example.gymcontrol.data.remote

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

@Serializable
data class GastoRow(val id: Long, val concepto: String, val categoria: String, val monto: Double, val fecha: String)

@Serializable
data class GastoNuevo(val concepto: String, val categoria: String, val monto: Double, val fecha: String)

object GastosRepository {
    private val tabla get() = SupabaseProvider.client.from("gastos")

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
        tabla.select {
            order("fecha", Order.DESCENDING)
            order("id", Order.DESCENDING)
        }.decodeList<GastoRow>()

    suspend fun crear(concepto: String, categoria: String, monto: Double, fecha: String) {
        tabla.insert(GastoNuevo(concepto, categoria, monto, aBd(fecha)))
    }

    suspend fun actualizar(id: Long, concepto: String, categoria: String, monto: Double, fecha: String) {
        tabla.update(GastoNuevo(concepto, categoria, monto, aBd(fecha))) { filter { eq("id", id) } }
    }

    suspend fun eliminar(id: Long) {
        tabla.delete { filter { eq("id", id) } }
    }
}