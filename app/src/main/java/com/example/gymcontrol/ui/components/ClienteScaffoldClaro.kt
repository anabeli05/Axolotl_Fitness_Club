package com.example.gymcontrol.ui.components

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.R

/** Paleta del cliente: grafito/dorado (oscuro) y hueso/gris cálido claro (claro). */
object ClienteTema {
    // Estado: al cambiarlo, todo lo que lea estos colores se recompone solo
    var oscuro by mutableStateOf(true)

    // Fondo de pantalla: hueso mate, suave para la vista
    val Fondo get() = if (oscuro) Color(0xFF222223) else Color(0xFFF1EFEB)

    // Encabezado (logo) y barra inferior: gris cálido claro
    val Navy get() = if (oscuro) Color(0xFF111111) else Color(0xFFE4E2DE)

    // Tarjetas: casi blancas en claro (más claras que el fondo)
    val Beige get() = if (oscuro) Color(0xFF3D3D3F) else Color(0xFFFAF9F7)

    // Recuadros internos: sólido, un poco más oscuro que la tarjeta
    val Interior get() = if (oscuro) Color.Black.copy(alpha = 0.35f) else Color(0xFFEFEDE9)

    // Cajas de acción/información (Editar datos, Cambiar contraseña, Datos personales)
    // Claro: café 8E723A | Oscuro: negro translúcido como antes
    val Caja get() = if (oscuro) Color.Black.copy(alpha = 0.35f) else Color(0xFF8E723A)
    // Iconos, títulos y etiquetas dentro de la caja: casi negro en claro (se lee mejor que el dorado)
    val CajaAcento get() = if (oscuro) Color(0xFFE3E3EC) else Color(0xFF1B1C1F)
    // Valores / texto principal dentro de la caja: blanco cálido
    val CajaValor get() = if (oscuro) Color(0xFFEEEEF3) else Color(0xFFF8F4EA)
    // Fondos pastel para los chips (verde = activo/disponible, rojo = inactivo/lleno)
    val VerdePastel get() = if (oscuro) Verde.copy(alpha = 0.15f) else Color(0xFFD3EFDC)
    val RojoPastel get() = if (oscuro) Rojo.copy(alpha = 0.15f) else Color(0xFFFBD9D9)
    // Texto/icono sobre botones rellenos de dorado (AzulSel): oscuro en modo oscuro, blanco en claro
    val SobreDorado get() = if (oscuro) Color(0xFF111111) else Color.White
    // Fondo de campos de texto: transparente en oscuro, tarjeta blanca en claro
    val CampoFondo get() = if (oscuro) Color.Transparent else Color(0xFFFAF9F7)
    // Fondo de los chips de estado (sólido en claro para que se lean sobre el café)
    val ChipFondo get() = if (oscuro) Color.Transparent else Color(0xFFF8F4EA)

    // Botones y dorados
    val Azul get() = Color(0xFF8E723A)
    val AzulClaro get() = if (oscuro) Color(0xFFC1AA60) else Color(0xFFA68B35)
    // Dorado profundo en claro: texto/iconos seleccionados con buen contraste
    val AzulSel get() = if (oscuro) Color(0xFFAC933D) else Color(0xFF8A6D12)
    val Barras = Color(0xFFAF9A54)

    // Textos
    val Texto get() = if (oscuro) Color(0xFFEEEEF3) else Color(0xFF1B1C1F)
    val TextoSec get() = if (oscuro) Color(0xFFE3E3EC) else Color(0xFF5B5E66)

    // Bordes dorados
    val Borde get() = if (oscuro) Color(0xFF93834C) else Color(0xFFC9B26A)

    // Estados
    val Verde get() = if (oscuro) Color(0xFF2E8B57) else Color(0xFF1B7243)
    val Rojo get() = if (oscuro) Color(0xFFD64545) else Color(0xFFB92B2B)

    // Iconos de las barras
    val IconoBarra get() = if (oscuro) Color.White else Color(0xFF3A3C42)

    // Línea divisoria entre barras y contenido (solo se nota en claro)
    val LineaBarra get() = if (oscuro) Color.Transparent else Color(0xFFC9B26A).copy(alpha = 0.4f)
}

data class ScaffoldTab(val label: String, val icon: ImageVector)

private val TABS_CLARO = listOf(
    ScaffoldTab("Inicio", Icons.Filled.Home),
    ScaffoldTab("Asesorías", Icons.Filled.FitnessCenter),
    ScaffoldTab("Pagos", Icons.Filled.CreditCard),
    ScaffoldTab("Perfil", Icons.Filled.Person)
)

private val LogoutBorderClaro
    get() = Brush.linearGradient(listOf(ClienteTema.Azul, ClienteTema.AzulSel))

@Composable
fun translatedTabLabel(label: String): String {
    return when (label) {
        "Inicio" -> stringResource(R.string.tab_home)
        "Asesorías" -> stringResource(R.string.tab_advisories)
        "Pagos" -> stringResource(R.string.tab_payments)
        "Perfil" -> stringResource(R.string.tab_profile)
        else -> label
    }
}

/** Alterna claro/oscuro y guarda la preferencia. Se puede llamar desde cualquier pantalla. */
fun alternarTema(context: Context) {
    ClienteTema.oscuro = !ClienteTema.oscuro
    context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
        .edit().putBoolean("tema_oscuro", ClienteTema.oscuro).apply()
}

/** Botón sol/luna: en oscuro muestra el sol (pasar a claro), en claro muestra la luna (pasar a oscuro). */
@Composable
fun BotonTema(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    IconButton(
        onClick = { alternarTema(context) },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (ClienteTema.oscuro) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            contentDescription = if (ClienteTema.oscuro) stringResource(R.string.light_theme) else stringResource(R.string.dark_theme),
            tint = ClienteTema.IconoBarra,
            modifier = Modifier.size(26.dp)
        )
    }
}

/** Parte izquierda de la barra superior: idioma + tema. */
@Composable
private fun BarraIzquierda(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier.padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { LocaleHelper.toggleLanguage(context) }) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = stringResource(R.string.change_language),
                tint = ClienteTema.IconoBarra,
                modifier = Modifier.size(26.dp)
            )
        }
        BotonTema()
    }
}

@Composable
private fun LineaDivisoria() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ClienteTema.LineaBarra)
    )
}

@Composable
private fun BarraInferior(
    tabs: List<ScaffoldTab>,
    currentSection: String,
    onNavigate: (String) -> Unit
) {
    Column {
        LineaDivisoria()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Navy)
                .navigationBarsPadding()
        ) {
            NavigationBar(
                modifier = Modifier.height(64.dp),
                containerColor = ClienteTema.Navy,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                tabs.forEach { tab ->
                    val translatedLabel = translatedTabLabel(tab.label)
                    NavigationBarItem(
                        selected = translatedLabel == currentSection || tab.label == currentSection,
                        onClick = { onNavigate(tab.label) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = translatedLabel) },
                        label = { Text(translatedLabel, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ClienteTema.AzulSel,
                            selectedTextColor = ClienteTema.AzulSel,
                            unselectedIconColor = ClienteTema.IconoBarra,
                            unselectedTextColor = ClienteTema.IconoBarra,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ClienteScaffoldClaro(
    currentSection: String,
    onNavigate: (String) -> Unit,
    @DrawableRes logoRes: Int,
    onLogout: () -> Unit = LocalOnLogout.current,
    content: @Composable () -> Unit
) {
    var confirmarLogout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClienteTema.Fondo)
    ) {
        // Encabezado recto con idioma, tema y botón de cerrar sesión
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Navy)
                .statusBarsPadding()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BarraIzquierda(modifier = Modifier.align(Alignment.CenterStart))

            Image(
                painter = painterResource(logoRes),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .height(28.dp)
                    .graphicsLayer(scaleX = 1.6f, scaleY = 1.6f)
            )
            IconButton(
                onClick = { confirmarLogout = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = stringResource(R.string.logout),
                    tint = ClienteTema.IconoBarra,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        LineaDivisoria()

        Box(modifier = Modifier.weight(1f)) { content() }

        BarraInferior(tabs = TABS_CLARO, currentSection = currentSection, onNavigate = onNavigate)
    }

    if (confirmarLogout) {
        DialogoCerrarSesion(
            onDismiss = { confirmarLogout = false },
            onConfirm = { confirmarLogout = false; onLogout() }
        )
    }
}

// ---------- Scaffold usado por las pantallas del instructor ----------

private val CLIENTE_TABS = listOf(
    ScaffoldTab("Inicio", Icons.Filled.Home),
    ScaffoldTab("Asesorías", Icons.Filled.FitnessCenter),
    ScaffoldTab("Perfil", Icons.Filled.Person)
)

@Composable
fun ClienteScaffold(
    currentSection: String,
    onNavigate: (String) -> Unit,
    @DrawableRes logoRes: Int,
    tabs: List<ScaffoldTab> = CLIENTE_TABS,
    onLogout: () -> Unit = LocalOnLogout.current,
    content: @Composable () -> Unit
) {
    var confirmarLogout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClienteTema.Fondo)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Navy)
                .statusBarsPadding()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BarraIzquierda(modifier = Modifier.align(Alignment.CenterStart))

            Image(
                painter = painterResource(logoRes),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .height(28.dp)
                    .graphicsLayer(scaleX = 1.6f, scaleY = 1.6f)
            )
            IconButton(
                onClick = { confirmarLogout = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = stringResource(R.string.logout),
                    tint = ClienteTema.IconoBarra,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        LineaDivisoria()

        Box(modifier = Modifier.weight(1f)) {
            content()
        }

        BarraInferior(tabs = tabs, currentSection = currentSection, onNavigate = onNavigate)
    }

    if (confirmarLogout) {
        DialogoCerrarSesion(
            onDismiss = { confirmarLogout = false },
            onConfirm = { confirmarLogout = false; onLogout() }
        )
    }
}

@Composable
private fun DialogoCerrarSesion(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Beige, RoundedCornerShape(20.dp))
                .border(1.5.dp, LogoutBorderClaro, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = ClienteTema.Azul
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.logout),
                color = ClienteTema.Texto,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.logout_confirmation),
                color = ClienteTema.TextoSec,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.Rojo,
                        contentColor = Color.White
                    )
                ) { Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold) }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.Verde,
                        contentColor = Color.White
                    )
                ) { Text(stringResource(R.string.exit), fontWeight = FontWeight.Bold) }
            }
        }
    }
}