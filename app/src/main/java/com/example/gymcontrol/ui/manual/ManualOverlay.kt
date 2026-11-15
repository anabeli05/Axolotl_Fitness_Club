package com.example.gymcontrol.ui.manual

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.GymButtonMinHeight
import com.example.gymcontrol.ui.components.GymButtonPadding
import com.example.gymcontrol.ui.components.GymButtonShape
import com.example.gymcontrol.ui.components.GymButtonText
import com.example.gymcontrol.ui.components.GymCardShape
import com.example.gymcontrol.ui.components.SesionActual
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private val Dorado = Color(0xFFF2D675)

/** Botón "?" de la barra superior: abre el manual del rol de la sesión (y solo ese). */
@Composable
fun BotonManual(modifier: Modifier = Modifier) {
    val descripcion = stringResource(R.string.manual_open)
    IconButton(
        onClick = { SesionActual.rol?.let { ManualTutorial.abrir(it) } },
        modifier = modifier
            .manualTarget("barra_manual")
            .semantics { contentDescription = descripcion }
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(1.8.dp, ClienteTema.IconoBarra, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "?",
                color = ClienteTema.IconoBarra,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Capa del manual. Va encima de toda la app (dentro de GymNavGraph) y solo existe mientras
 * el manual está abierto; al cerrarlo la app queda exactamente como estaba.
 */
@Composable
fun ManualHost(rol: UserRole?, seccionActual: String?, navegar: (String) -> Unit) {
    val context = LocalContext.current

    SideEffect {
        ManualTutorial.navegar = navegar
        ManualTutorial.seccionActual = seccionActual
    }

    // Primera vez de cada rol: se ofrece el manual (se puede omitir). Después solo con el botón "?".
    LaunchedEffect(rol) {
        if (rol == null) {
            ManualTutorial.cerrar()
        } else if (!ManualTutorial.visto(context, rol)) {
            delay(1200)
            if (ManualTutorial.fase == ManualFase.OCULTO && SesionActual.rol == rol) {
                ManualTutorial.abrir(rol)
            }
        }
    }

    val fase = ManualTutorial.fase
    // Cada usuario ve únicamente el manual de su propio rol
    if (rol == null || fase == ManualFase.OCULTO || ManualTutorial.rol != rol) return

    val salir: () -> Unit = {
        ManualTutorial.marcarVisto(context, rol)
        ManualTutorial.cerrar()
    }

    BackHandler(onBack = salir)

    var origen by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                origen = it.positionInWindow()
                ManualTutorial.altoVentana = it.size.height
            }
            // La capa recibe los toques para que no se activen botones de la app por accidente
            // mientras se explica; el recorrido siempre se puede cerrar.
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        when (fase) {
            ManualFase.BIENVENIDA -> Bienvenida(
                rol = rol,
                onComenzar = {
                    ManualTutorial.marcarVisto(context, rol)
                    ManualTutorial.comenzar()
                },
                onOmitir = salir,
                onCerrar = salir
            )
            ManualFase.RECORRIDO -> Recorrido(origen = origen, onSalir = salir)
            ManualFase.FIN -> Fin(onFinalizar = salir)
            ManualFase.OCULTO -> Unit
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Recorrido guiado
// ---------------------------------------------------------------------------------------------

private class FocoManual {
    var mostrado: Rect? = null
    var desde: Rect? = null
}

@Composable
private fun Recorrido(origen: Offset, onSalir: () -> Unit) {
    val pasos = ManualTutorial.pasos
    if (pasos.isEmpty()) return
    val indice = ManualTutorial.indice.coerceIn(0, pasos.lastIndex)
    val paso = pasos[indice]

    var buscando by remember { mutableStateOf(true) }
    var tarjetaArriba by remember { mutableStateOf(false) }
    val foco = remember { FocoManual() }
    val progreso = remember { Animatable(1f) }
    val tick = remember { mutableLongStateOf(0L) }

    // Se vuelve a dibujar en cada cuadro para que el resaltado siga al elemento (scroll, menú, etc.)
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { tick.longValue = it }
        }
    }

    // Prepara el paso: abre la sección, el menú lateral, busca el elemento y lo trae a la vista
    LaunchedEffect(indice) {
        buscando = true

        val cambioMenu = ManualTutorial.menuLateralAbierto != paso.menuLateral
        ManualTutorial.pedirMenuLateral(paso.menuLateral)

        val seccion = paso.seccion
        if (seccion != null && seccion != ManualTutorial.seccionActual) {
            ManualTutorial.navegar?.invoke(seccion)
            delay(750)
        }
        if (cambioMenu) delay(500)

        val id = paso.objetivo
        val encontrado = id != null && ubicar(id)
        val vivo = if (encontrado && id != null) ManualTutorial.rectDe(id) else null
        if (vivo != null) {
            val alto = ManualTutorial.altoVentana.toFloat()
            tarjetaArriba = vivo.top > alto - vivo.bottom
        }

        foco.desde = foco.mostrado
        progreso.snapTo(0f)
        buscando = false
        progreso.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }

    val transicion = rememberInfiniteTransition(label = "manual")
    val pulso by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "pulso"
    )
    val rebote by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rebote"
    )

    Box(Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            tick.longValue // se lee para redibujar en cada cuadro

            drawRect(Color.Black.copy(alpha = 0.74f))

            val id = paso.objetivo
            val vivo = if (!buscando && id != null) {
                ManualTutorial.rectDe(id)?.translate(-origen.x, -origen.y)
            } else null
            val desde = foco.desde
            val r: Rect? = when {
                buscando -> foco.mostrado
                vivo == null -> null
                desde == null -> vivo
                else -> androidx.compose.ui.geometry.lerp(desde, vivo, progreso.value)
            }
            foco.mostrado = r

            if (r != null) {
                val margen = 6.dp.toPx()
                val esquina = 16.dp.toPx()
                val topLeft = Offset(r.left - margen, r.top - margen)
                val tam = Size(r.width + 2 * margen, r.height + 2 * margen)

                // Hueco transparente sobre el elemento explicado + borde dorado
                drawRoundRect(Color.Transparent, topLeft, tam, CornerRadius(esquina), blendMode = BlendMode.Clear)
                drawRoundRect(Dorado, topLeft, tam, CornerRadius(esquina), style = Stroke(3.dp.toPx()))

                // Aro que se expande (pulso)
                val extra = pulso * 14.dp.toPx()
                drawRoundRect(
                    color = Dorado.copy(alpha = (1f - pulso) * 0.85f),
                    topLeft = Offset(topLeft.x - extra, topLeft.y - extra),
                    size = Size(tam.width + 2 * extra, tam.height + 2 * extra),
                    cornerRadius = CornerRadius(esquina + extra),
                    style = Stroke(2.dp.toPx())
                )

                // Flecha que rebota entre la tarjeta y el elemento
                if (r.height < size.height * 0.45f) {
                    val cx = (r.left + r.right) / 2f
                    val bote = rebote * 8.dp.toPx()
                    val mitad = 9.dp.toPx()
                    val largo = 16.dp.toPx()
                    val flecha = Path()
                    if (tarjetaArriba) {
                        val punta = topLeft.y - 4.dp.toPx() - bote
                        flecha.moveTo(cx, punta)
                        flecha.lineTo(cx - mitad, punta - largo)
                        flecha.lineTo(cx + mitad, punta - largo)
                    } else {
                        val punta = topLeft.y + tam.height + 4.dp.toPx() + bote
                        flecha.moveTo(cx, punta)
                        flecha.lineTo(cx - mitad, punta + largo)
                        flecha.lineTo(cx + mitad, punta + largo)
                    }
                    flecha.close()
                    drawPath(flecha, Dorado)
                }

                // "Toca aquí": punto con onda sobre el botón
                if (paso.tocar) {
                    val c = r.center
                    drawCircle(Dorado.copy(alpha = 0.95f), radius = 7.dp.toPx(), center = c)
                    drawCircle(
                        color = Dorado.copy(alpha = (1f - pulso) * 0.9f),
                        radius = 9.dp.toPx() + pulso * 26.dp.toPx(),
                        center = c,
                        style = Stroke(2.5.dp.toPx())
                    )
                }
            }
        }

        val centrada = paso.objetivo == null
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = when {
                centrada -> Alignment.Center
                tarjetaArriba -> Alignment.TopCenter
                else -> Alignment.BottomCenter
            }
        ) {
            key(indice) {
                val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = visible,
                    enter = fadeIn(tween(260)) + slideInVertically(tween(260)) {
                        if (centrada) it / 10 else if (tarjetaArriba) -it / 5 else it / 5
                    }
                ) {
                    TarjetaBase {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.manual_step_counter, indice + 1, pasos.size),
                                color = ClienteTema.TextoSec,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onSalir) {
                                Text(
                                    stringResource(R.string.manual_skip),
                                    color = ClienteTema.AzulSel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = onSalir, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.manual_close),
                                    tint = ClienteTema.TextoSec
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (indice + 1f) / pasos.size },
                            color = ClienteTema.AzulSel,
                            trackColor = ClienteTema.Borde.copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                        Spacer(Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                stringResource(paso.titulo),
                                color = ClienteTema.Texto,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(paso.texto),
                                color = ClienteTema.Texto,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { ManualTutorial.anterior() },
                                enabled = indice > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = GymButtonMinHeight),
                                shape = GymButtonShape,
                                contentPadding = GymButtonPadding,
                                border = BorderStroke(1.dp, ClienteTema.Borde),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                            ) { GymButtonText(stringResource(R.string.manual_prev)) }

                            Button(
                                onClick = { ManualTutorial.siguiente() },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = GymButtonMinHeight),
                                shape = GymButtonShape,
                                contentPadding = GymButtonPadding,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ClienteTema.AzulSel,
                                    contentColor = ClienteTema.SobreDorado
                                )
                            ) {
                                GymButtonText(
                                    if (indice == pasos.lastIndex) stringResource(R.string.manual_finish)
                                    else stringResource(R.string.manual_next)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Búsqueda del elemento de cada paso ----

/** Deja el elemento a la vista (desplazando la pantalla si hace falta). */
private suspend fun ubicar(id: String): Boolean {
    if (!esperarExistencia(id, 700)) {
        if (!buscarEnLista(id)) return false
    }
    withTimeoutOrNull(1000) { ManualTutorial.traer(id) }
    delay(150)
    return esperarVisible(id, 900)
}

private suspend fun esperarExistencia(id: String, milis: Long): Boolean =
    withTimeoutOrNull(milis) { snapshotFlow { ManualTutorial.existe(id) }.first { it } } != null

private suspend fun esperarVisible(id: String, milis: Long): Boolean {
    var espera = 0L
    while (espera <= milis) {
        if (ManualTutorial.rectDe(id) != null) return true
        delay(50)
        espera += 50
    }
    return false
}

/** Recorre la lista de la pantalla de arriba hacia abajo hasta que aparece el elemento. */
private suspend fun buscarEnLista(id: String): Boolean {
    val lista = ManualTutorial.lista ?: return false
    lista.scrollToItem(0)
    delay(150)
    if (ManualTutorial.existe(id)) return true
    for (i in 0 until 16) {
        if (!lista.canScrollForward) break
        lista.scrollBy(ManualTutorial.altoVentana * 0.55f)
        delay(140)
        if (ManualTutorial.existe(id)) return true
    }
    lista.scrollToItem(0)
    return false
}

// ---------------------------------------------------------------------------------------------
// Bienvenida y final
// ---------------------------------------------------------------------------------------------

@Composable
private fun TarjetaBase(content: @Composable ColumnScope.() -> Unit) {
    val maxAlto = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
    Column(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .heightIn(max = maxAlto)
            .background(ClienteTema.Beige, GymCardShape)
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel)),
                GymCardShape
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun Bienvenida(
    rol: UserRole,
    onComenzar: () -> Unit,
    onOmitir: () -> Unit,
    onCerrar: () -> Unit
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f)
        ) {
            TarjetaBase {
                Box(Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onCerrar,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.manual_close),
                            tint = ClienteTema.TextoSec
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.logo_final),
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.TopCenter)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        stringResource(R.string.manual_welcome_title),
                        color = ClienteTema.Texto,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(nombreRol(rol)),
                        color = ClienteTema.AzulSel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(textoBienvenida(rol)),
                        color = ClienteTema.Texto,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.manual_welcome_hint),
                        color = ClienteTema.TextoSec,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onComenzar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = GymButtonMinHeight),
                    shape = GymButtonShape,
                    contentPadding = GymButtonPadding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.AzulSel,
                        contentColor = ClienteTema.SobreDorado
                    )
                ) { GymButtonText(stringResource(R.string.manual_start)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOmitir,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = GymButtonMinHeight),
                    shape = GymButtonShape,
                    contentPadding = GymButtonPadding,
                    border = BorderStroke(1.dp, ClienteTema.Borde),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                ) { GymButtonText(stringResource(R.string.manual_omit)) }
            }
        }
    }
}

@Composable
private fun Fin(onFinalizar: () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f)
        ) {
            TarjetaBase {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(ClienteTema.Verde, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.manual_end_title),
                    color = ClienteTema.Texto,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.manual_end_text),
                    color = ClienteTema.Texto,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onFinalizar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = GymButtonMinHeight),
                    shape = GymButtonShape,
                    contentPadding = GymButtonPadding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.AzulSel,
                        contentColor = ClienteTema.SobreDorado
                    )
                ) { GymButtonText(stringResource(R.string.manual_finish)) }
            }
        }
    }
}

private fun nombreRol(rol: UserRole): Int = when (rol) {
    UserRole.CLIENTE -> R.string.manual_role_cliente
    UserRole.RECEPCION -> R.string.manual_role_recepcion
    UserRole.INSTRUCTOR -> R.string.manual_role_instructor
    UserRole.ENCARGADO -> R.string.manual_role_encargado
}

private fun textoBienvenida(rol: UserRole): Int = when (rol) {
    UserRole.CLIENTE -> R.string.manual_welcome_cliente
    UserRole.RECEPCION -> R.string.manual_welcome_recepcion
    UserRole.INSTRUCTOR -> R.string.manual_welcome_instructor
    UserRole.ENCARGADO -> R.string.manual_welcome_encargado
}
