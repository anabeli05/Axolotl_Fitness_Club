package com.example.gymcontrol.ui.manual

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.gymcontrol.data.model.UserRole

/**
 * Un paso del manual interactivo.
 *
 * @param objetivo id del elemento a resaltar (ver [manualTarget]); null = tarjeta centrada sin resaltar.
 * @param seccion sección/pantalla que debe estar abierta durante el paso.
 * @param menuLateral true si el paso necesita el menú lateral abierto (Recepción y Dueño).
 * @param tocar true si el elemento es algo que se presiona (se muestra la animación de "toca aquí").
 */
class ManualPaso(
    val objetivo: String?,
    @StringRes val titulo: Int,
    @StringRes val texto: Int,
    val seccion: String?,
    val menuLateral: Boolean = false,
    val tocar: Boolean = false
)

enum class ManualFase { OCULTO, BIENVENIDA, RECORRIDO, FIN }

private class RegistroObjetivo(
    val id: String,
    val coords: LayoutCoordinates,
    val traer: BringIntoViewRequester
)

/**
 * Estado global del manual interactivo (igual que SesionActual, vive fuera de la composición).
 * Solo se muestra encima de la app; no cambia nada de la lógica ni de las pantallas.
 */
object ManualTutorial {
    var fase by mutableStateOf(ManualFase.OCULTO)
        private set

    var rol: UserRole? by mutableStateOf(null)
        private set

    var indice by mutableIntStateOf(0)
        private set

    /** Los pasos del menú lateral piden abrirlo; GymScaffold lo obedece. */
    var menuLateralAbierto by mutableStateOf(false)
        private set

    /** Alto de la ventana en px (lo fija la capa del manual). */
    internal var altoVentana by mutableIntStateOf(0)

    /** Navegación hacia una sección (la entrega GymNavGraph) y la sección visible ahora. */
    internal var navegar: ((String) -> Unit)? = null
    internal var seccionActual: String? = null

    /** Lista con scroll de la pantalla visible (si la tiene), para buscar elementos fuera de vista. */
    internal var lista: LazyListState? = null

    private val registros = mutableStateMapOf<Any, RegistroObjetivo>()

    val pasos: List<ManualPaso>
        get() = rol?.let { ManualPasos.de(it) } ?: emptyList()

    // ---------- Control del recorrido ----------

    fun abrir(rol: UserRole) {
        this.rol = rol
        indice = 0
        menuLateralAbierto = false
        fase = ManualFase.BIENVENIDA
    }

    fun comenzar() {
        indice = 0
        fase = ManualFase.RECORRIDO
    }

    fun siguiente() {
        if (indice < pasos.lastIndex) indice++ else terminar()
    }

    fun anterior() {
        if (indice > 0) indice--
    }

    private fun terminar() {
        menuLateralAbierto = false
        fase = ManualFase.FIN
    }

    /** Omitir, saltar o cerrar: regresa a la app tal cual estaba. */
    fun cerrar() {
        menuLateralAbierto = false
        fase = ManualFase.OCULTO
    }

    fun pedirMenuLateral(abierto: Boolean) {
        menuLateralAbierto = abierto
    }

    // ---------- "Ya se ofreció" (para no mostrarlo solo cada vez) ----------

    private fun clave(rol: UserRole) = "manual_visto_${rol.name}"

    fun visto(context: Context, rol: UserRole): Boolean =
        context.getSharedPreferences("ajustes", Context.MODE_PRIVATE).getBoolean(clave(rol), false)

    fun marcarVisto(context: Context, rol: UserRole) {
        context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
            .edit().putBoolean(clave(rol), true).apply()
    }

    // ---------- Registro de elementos resaltables ----------

    internal fun registrar(dueno: Any, id: String, coords: LayoutCoordinates, traer: BringIntoViewRequester) {
        val actual = registros[dueno]
        if (actual == null || actual.coords !== coords || actual.id != id) {
            registros[dueno] = RegistroObjetivo(id, coords, traer)
        }
    }

    internal fun quitar(dueno: Any) {
        registros.remove(dueno)
    }

    internal fun registrarLista(estado: LazyListState) {
        lista = estado
    }

    internal fun quitarLista(estado: LazyListState) {
        if (lista === estado) lista = null
    }

    private fun candidatos(id: String): List<RegistroObjetivo> =
        registros.values.filter { it.id == id && it.coords.isAttached }

    /** True si el elemento ya está compuesto en pantalla (aunque aún esté fuera de vista). */
    internal fun existe(id: String): Boolean = candidatos(id).isNotEmpty()

    /** Rectángulo actual (en px de la ventana) del elemento visible más arriba con ese id. */
    internal fun rectDe(id: String): Rect? =
        candidatos(id)
            .map { it.coords.boundsInWindow() }
            .filter { it.width >= 8f && it.height >= 8f }
            .minByOrNull { it.top }

    /** Pide que el elemento quede completamente a la vista (hace scroll si está en una lista). */
    @OptIn(ExperimentalFoundationApi::class)
    internal suspend fun traer(id: String) {
        val registro = candidatos(id).minByOrNull { it.coords.boundsInWindow().top } ?: return
        try {
            registro.traer.bringIntoView()
        } catch (_: Exception) {
            // si no se puede desplazar, el elemento se queda donde está
        }
    }
}

/**
 * Marca un elemento de la pantalla para que el manual pueda resaltarlo.
 * No cambia su aspecto ni su comportamiento.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.manualTarget(id: String): Modifier = composed {
    val dueno = remember { Any() }
    val traer = remember { BringIntoViewRequester() }
    DisposableEffect(dueno) {
        onDispose { ManualTutorial.quitar(dueno) }
    }
    this
        .bringIntoViewRequester(traer)
        .onGloballyPositioned { ManualTutorial.registrar(dueno, id, it, traer) }
}

/**
 * Igual que rememberLazyListState(), pero avisa al manual para que pueda desplazar la lista
 * y encontrar elementos que aún no están a la vista.
 */
@Composable
fun rememberManualLista(): LazyListState {
    val estado = rememberLazyListState()
    DisposableEffect(estado) {
        ManualTutorial.registrarLista(estado)
        onDispose { ManualTutorial.quitarLista(estado) }
    }
    return estado
}
