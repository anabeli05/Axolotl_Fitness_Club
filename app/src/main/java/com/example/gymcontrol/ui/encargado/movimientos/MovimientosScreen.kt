package com.example.gymcontrol.ui.encargado.movimientos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.data.remote.MovimientoRolRow
import com.example.gymcontrol.data.remote.MovimientosRepository
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.GymButtonMinHeight
import com.example.gymcontrol.ui.components.GymButtonPadding
import com.example.gymcontrol.ui.components.GymButtonShape
import com.example.gymcontrol.ui.components.GymButtonText
import com.example.gymcontrol.ui.components.GymCardShape
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.SesionActual
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Minúsculas y sin acentos, para buscar "Recepción" escribiendo "recepcion". */
private fun normalizar(t: String): String =
    java.text.Normalizer.normalize(t.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

/** null = Todos */
private val ROLES = listOf(null, "ENCARGADO", "RECEPCION", "INSTRUCTOR", "CLIENTE")

/**
 * Bitácora de lo que hace cada persona del gimnasio (sql/auditoria.sql).
 * El filtro por rol se aplica en la base (p_rol); el buscador filtra en la app las filas
 * ya cargadas (sin distinguir mayúsculas ni acentos) y sigue aplicando al cargar más.
 */
@Composable
fun MovimientosScreen(onNavigate: (String) -> Unit = {}) {
    var buscar by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf<String?>(null) }
    var movimientos by remember { mutableStateOf<List<MovimientoRolRow>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var cargandoMas by remember { mutableStateOf(false) }
    var hayMas by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var detalle by remember { mutableStateOf<MovimientoRolRow?>(null) }
    var reintento by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun mensajeError(e: Exception) =
        context.getString(
            R.string.movements_load_failed,
            e.message?.substringBefore("\nCode:")?.lineSequence()?.firstOrNull() ?: ""
        )

    // Se vuelve a consultar al cambiar el rol (o al reintentar)
    LaunchedEffect(rol, reintento) {
        cargando = true
        try {
            val filas = MovimientosRepository.listar(SesionActual.correo ?: "", rol)
            movimientos = filas
            hayMas = filas.size == MovimientosRepository.BLOQUE
            error = null
        } catch (e: Exception) {
            movimientos = emptyList()
            hayMas = false
            error = mensajeError(e)
        }
        cargando = false
    }

    fun cargarMas() {
        if (cargandoMas) return
        cargandoMas = true
        scope.launch {
            try {
                val filas = MovimientosRepository.listar(SesionActual.correo ?: "", rol, offset = movimientos.size)
                movimientos = movimientos + filas.filter { f -> movimientos.none { it.id == f.id } }
                hayMas = filas.size == MovimientosRepository.BLOQUE
                error = null
            } catch (e: Exception) {
                error = mensajeError(e)
            }
            cargandoMas = false
        }
    }

    // Búsqueda local sobre lo cargado: usuario, gmail, rol, tipo de acción, módulo y descripción
    val q = normalizar(buscar.trim())
    val visibles = remember(movimientos, q) {
        if (q.isEmpty()) movimientos
        else movimientos.filter { m ->
            listOf(m.usuario, m.gmail, m.rol, m.tipoAccion, m.modulo, m.descripcion)
                .any { it != null && normalizar(it).contains(q) }
        }
    }

    GymScaffold(currentSection = "Movimientos", onNavigate = onNavigate) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.movements_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = ClienteTema.Texto,
                    fontSize = 22.sp
                )
            }

            // Buscador con lupa
            item {
                OutlinedTextField(
                    value = buscar,
                    onValueChange = { buscar = it },
                    placeholder = { Text(stringResource(R.string.movements_search), color = ClienteTema.TextoSec) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ClienteTema.TextoSec) },
                    trailingIcon = {
                        if (buscar.isNotEmpty()) {
                            IconButton(onClick = { buscar = "" }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.movements_clear_search),
                                    tint = ClienteTema.TextoSec
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClienteTema.AzulSel,
                        unfocusedBorderColor = ClienteTema.Borde,
                        focusedTextColor = ClienteTema.Texto,
                        unfocusedTextColor = ClienteTema.Texto,
                        cursorColor = ClienteTema.AzulSel,
                        focusedContainerColor = ClienteTema.CampoFondo,
                        unfocusedContainerColor = ClienteTema.CampoFondo
                    )
                )
            }

            // Filtro por rol
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ROLES.forEach { valor ->
                        val sel = rol == valor
                        FilterChip(
                            selected = sel,
                            onClick = { rol = valor },
                            label = {
                                Text(
                                    if (valor == null) stringResource(R.string.movements_all) else etiquetaRol(valor),
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ClienteTema.Navy,
                                labelColor = ClienteTema.Texto,
                                selectedContainerColor = ClienteTema.Azul,
                                selectedLabelColor = Color.White
                            ),
                            border = BorderStroke(1.dp, if (sel) ClienteTema.Azul else ClienteTema.Borde)
                        )
                    }
                }
            }

            error?.let {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(it, color = ClienteTema.Rojo, fontSize = 14.sp, textAlign = TextAlign.Center)
                        TextButton(onClick = { reintento++ }) {
                            Text(stringResource(R.string.movements_retry), color = ClienteTema.AzulSel, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (cargando) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ClienteTema.AzulSel)
                    }
                }
            } else {
                if (visibles.isEmpty() && error == null) {
                    item {
                        Text(
                            stringResource(if (movimientos.isEmpty()) R.string.movements_empty else R.string.movements_no_match),
                            color = ClienteTema.TextoSec,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        )
                    }
                }
                items(visibles, key = { it.id }) { m ->
                    MovimientoCard(m, onClick = { detalle = m })
                }
                // Siempre visible si hay más filas: así se puede seguir buscando en las anteriores
                if (hayMas) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            OutlinedButton(
                                onClick = { cargarMas() },
                                enabled = !cargandoMas,
                                shape = GymButtonShape,
                                border = BorderStroke(1.dp, ClienteTema.AzulSel),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.AzulSel),
                                modifier = Modifier.defaultMinSize(minHeight = GymButtonMinHeight),
                                contentPadding = GymButtonPadding
                            ) {
                                if (cargandoMas) {
                                    CircularProgressIndicator(color = ClienteTema.AzulSel, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                } else {
                                    GymButtonText(stringResource(R.string.movements_show_more))
                                }
                            }
                        }
                    }
                }
            }
        }

        detalle?.let { DetalleDialog(it, onDismiss = { detalle = null }) }
    }
}

@Composable
private fun etiquetaRol(rol: String): String = when (rol) {
    "ENCARGADO" -> stringResource(R.string.role_manager)
    "RECEPCION" -> stringResource(R.string.role_reception)
    "INSTRUCTOR" -> stringResource(R.string.role_instructor)
    "CLIENTE" -> stringResource(R.string.role_client)
    else -> stringResource(R.string.movements_no_role)
}

@Composable
private fun etiquetaAccion(tipo: String): String = stringResource(
    when (tipo) {
        "INSERT" -> R.string.movements_action_insert
        "UPDATE" -> R.string.movements_action_update
        "DELETE" -> R.string.movements_action_delete
        "CAMBIO_PERMISO" -> R.string.movements_action_permission
        "LOGIN" -> R.string.movements_action_login
        "CANCELACION" -> R.string.movements_action_cancel
        "PAGO" -> R.string.movements_action_payment
        else -> R.string.movements_action_other
    }
)

private fun colorAccion(tipo: String): Color = when (tipo) {
    "INSERT", "PAGO" -> ClienteTema.Verde
    "DELETE", "CANCELACION" -> ClienteTema.Rojo
    "LOGIN", "OTRO" -> ClienteTema.TextoSec
    else -> ClienteTema.AzulSel
}

/** "2026-11-17T21:10:05.123" (ya en hora de CDMX) → "17 nov 2026 · 21:10" */
@Composable
private fun fechaHora(valor: String): String {
    val context = LocalContext.current
    val hora = valor.replace(' ', 'T').substringAfter('T', "").take(5)
    val fecha = ClientesRepository.fechaApp(context, valor)
    return if (hora.length == 5) "$fecha · $hora" else fecha
}

private fun tieneValores(v: JsonElement?): Boolean =
    v != null && v !is JsonNull && !(v is JsonObject && v.isEmpty())

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Text(
        texto,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
private fun MovimientoCard(m: MovimientoRolRow, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ClienteTema.Beige, RoundedCornerShape(14.dp))
            .border(1.5.dp, ClienteTema.Borde, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    m.usuario,
                    color = ClienteTema.Texto,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                m.gmail?.let {
                    Text(it, color = ClienteTema.TextoSec, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            Etiqueta(etiquetaRol(m.rol), ClienteTema.AzulSel)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Etiqueta(etiquetaAccion(m.tipoAccion), colorAccion(m.tipoAccion))
            Spacer(Modifier.width(8.dp))
            Text(
                m.modulo.replaceFirstChar { it.uppercase() },
                color = ClienteTema.TextoSec,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        m.descripcion?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = ClienteTema.Texto, fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(fechaHora(m.fechaHoraMx), color = ClienteTema.TextoSec, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (tieneValores(m.valoresAnteriores) || tieneValores(m.valoresNuevos)) {
                Text(
                    stringResource(R.string.movements_view_detail),
                    color = ClienteTema.AzulSel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun textoValor(v: JsonElement?): String = when {
    v == null || v is JsonNull -> "—"
    v is JsonPrimitive -> v.content.ifBlank { "—" }
    else -> v.toString()
}

@Composable
private fun DetalleDialog(m: MovimientoRolRow, onDismiss: () -> Unit) {
    val antes = m.valoresAnteriores as? JsonObject
    val despues = m.valoresNuevos as? JsonObject
    val claves = ((antes?.keys ?: emptySet()) + (despues?.keys ?: emptySet())).toList()
    val ambos = tieneValores(antes) && tieneValores(despues)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ClienteTema.Beige,
        shape = GymCardShape,
        title = {
            Text(
                stringResource(R.string.movements_detail_title),
                color = ClienteTema.Texto,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Etiqueta(etiquetaRol(m.rol), ClienteTema.AzulSel)
                    Spacer(Modifier.width(6.dp))
                    Etiqueta(etiquetaAccion(m.tipoAccion), colorAccion(m.tipoAccion))
                }
                Spacer(Modifier.height(8.dp))
                Text(m.usuario, color = ClienteTema.Texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                m.gmail?.let { Text(it, color = ClienteTema.TextoSec, fontSize = 12.sp) }
                Text(fechaHora(m.fechaHoraMx), color = ClienteTema.TextoSec, fontSize = 12.sp)
                m.descripcion?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = ClienteTema.Texto, fontSize = 14.sp)
                }

                if (antes == null && despues == null) {
                    // Valores que no son objeto (texto, lista): se muestran tal cual
                    listOf(
                        R.string.movements_before to m.valoresAnteriores,
                        R.string.movements_after to m.valoresNuevos
                    ).filter { tieneValores(it.second) }.forEach { (titulo, v) ->
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(titulo), color = ClienteTema.AzulSel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(v.toString(), color = ClienteTema.Texto, fontSize = 12.sp)
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    claves.forEach { k ->
                        val va = textoValor(antes?.get(k))
                        val vd = textoValor(despues?.get(k))
                        val cambio = ambos && va != vd
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(
                                    if (cambio) ClienteTema.AzulSel.copy(alpha = 0.10f) else ClienteTema.Interior,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(k, color = ClienteTema.TextoSec, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (antes != null) {
                                Text(
                                    "${stringResource(R.string.movements_before)}: $va",
                                    color = if (cambio) ClienteTema.Rojo else ClienteTema.Texto,
                                    fontSize = 13.sp,
                                    textDecoration = if (cambio) TextDecoration.LineThrough else null
                                )
                            }
                            if (despues != null) {
                                Text(
                                    "${stringResource(R.string.movements_after)}: $vd",
                                    color = if (cambio) ClienteTema.Verde else ClienteTema.Texto,
                                    fontSize = 13.sp,
                                    fontWeight = if (cambio) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = onDismiss,
                    shape = GymButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.AzulSel,
                        contentColor = ClienteTema.SobreDorado
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = GymButtonMinHeight),
                    contentPadding = GymButtonPadding
                ) { GymButtonText(stringResource(R.string.common_close)) }
            }
        }
    )
}
