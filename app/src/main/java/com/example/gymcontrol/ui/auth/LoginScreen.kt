package com.example.gymcontrol.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.data.remote.AuthRepository
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.ColoresDialogo
import com.example.gymcontrol.ui.components.LocaleHelper
import com.example.gymcontrol.ui.components.RestablecerContrasenaDialog
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.components.alternarTema
import kotlinx.coroutines.launch

// Paleta del login: en oscuro conserva el diseño original (negro/dorado);
// en claro toma los tokens de ClienteTema. Al cambiar ClienteTema.oscuro todo se  todo se recompone solo.
private val FondoNegro get() = if (ClienteTema.oscuro) Color(0xFF000000) else ClienteTema.Fondo
private val TarjetaOscura get() = if (ClienteTema.oscuro) Color(0xFF1B1B1B) else ClienteTema.Beige
private val MoradoCampo get() = if (ClienteTema.oscuro) Color(0xFF2F2E2E) else ClienteTema.Interior
private val MoradoBorde get() = if (ClienteTema.oscuro) Color(0xFF6B5326) else ClienteTema.Azul
private val DoradoBorde get() = if (ClienteTema.oscuro) Color(0xFFC9A24B) else ClienteTema.Borde
private val Dorado get() = if (ClienteTema.oscuro) Color(0xFFC9A24B) else ClienteTema.AzulSel
private val BlancoTexto get() = if (ClienteTema.oscuro) Color(0xFFFFFFFF) else ClienteTema.Texto
private val RojoError get() = if (ClienteTema.oscuro) Color(0xFFFF6B6B) else ClienteTema.Rojo

// Botones rellenos: gris oscuro en oscuro, café en claro (texto siempre blanco)
private val BotonFondo get() = if (ClienteTema.oscuro) Color(0xFF2F2E2E) else ClienteTema.Azul
private val BotonTexto = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToTerms: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    // true cuando el usuario regresó de Términos o Privacidad con el botón "Aceptar"
    legalAceptado: Boolean = false,
    onLegalAceptadoConsumido: () -> Unit = {},
    onLogin: (UserRole) -> Unit
) {
    // rememberSaveable: los datos se conservan al abrir y cerrar las pantallas legales
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var terminosAceptados by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(legalAceptado) {
        if (legalAceptado) {
            terminosAceptados = true
            onLegalAceptadoConsumido()
        }
    }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrarRestablecer by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun safeNavigate(action: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= 600L) {
            lastClickTime = currentTime
            action()
        }
    }

    val strInvalidCreds = stringResource(R.string.error_invalid_credentials)
    val strConnErrorPrefix = stringResource(R.string.error_connection_prefix)
    val strPassUpdated = stringResource(R.string.password_updated_success)

    val campoShape = RoundedCornerShape(50)
    // Se calcula en cada recomposición para que siga al tema
    val campoColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MoradoCampo,
        unfocusedContainerColor = MoradoCampo,
        focusedBorderColor = Dorado,
        unfocusedBorderColor = DoradoBorde,
        focusedTextColor = BlancoTexto,
        unfocusedTextColor = BlancoTexto,
        cursorColor = Dorado,
        focusedLabelColor = Dorado,
        unfocusedLabelColor = BlancoTexto,
        focusedTrailingIconColor = Dorado,
        unfocusedTrailingIconColor = BlancoTexto
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoNegro)
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(TarjetaOscura)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(MoradoBorde, DoradoBorde)),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón de tema claro/oscuro (izquierda)
                IconButton(
                    onClick = { alternarTema(context) }
                ) {
                    Icon(
                        imageVector = if (ClienteTema.oscuro) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = stringResource(
                            if (ClienteTema.oscuro) R.string.light_theme else R.string.dark_theme
                        ),
                        tint = Dorado
                    )
                }

                // Botón de cambio de idioma (derecha)
                IconButton(
                    onClick = { LocaleHelper.toggleLanguage(context) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = stringResource(R.string.change_language),
                        tint = Dorado
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_final),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(140.dp)
                )
                Spacer(Modifier.height(24.dp))

                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text(stringResource(R.string.login_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = campoShape,
                    colors = campoColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.login_password)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) stringResource(R.string.hide_password)
                                else stringResource(R.string.show_password)
                            )
                        }
                    },
                    shape = campoShape,
                    colors = campoColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = terminosAceptados,
                        onCheckedChange = { terminosAceptados = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Dorado,
                            uncheckedColor = DoradoBorde,
                            checkmarkColor = TarjetaOscura
                        )
                    )
                    val prefix = stringResource(R.string.accept_terms_prefix).trim()
                    val terms = stringResource(R.string.terms_and_conditions).trim()
                    val connector = stringResource(R.string.accept_terms_and).trim()
                    val privacy = stringResource(R.string.privacy_policy).trim()

                    val textoTerminos = buildAnnotatedString {
                        append("$prefix ")
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = "terminos",
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = Dorado,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Medium
                                    )
                                ),
                                linkInteractionListener = {
                                    safeNavigate(onNavigateToTerms)
                                }
                            )
                        ) {
                            append(terms)
                        }
                        append(" $connector ")
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = "politica",
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = Dorado,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Medium
                                    )
                                ),
                                linkInteractionListener = {
                                    safeNavigate(onNavigateToPrivacy)
                                }
                            )
                        ) {
                            append(privacy)
                        }
                    }
                    Text(
                        text = textoTerminos,
                        style = MaterialTheme.typography.bodySmall.copy(color = BlancoTexto),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                val id = identifier.trim()
                                val fila = AuthRepository.loginAuto(id, password)
                                val rolEncontrado = fila?.let {
                                    runCatching { UserRole.valueOf(it.rol) }.getOrNull()
                                }
                                if (rolEncontrado != null && fila != null) {
                                    // Se guarda el correo real aunque haya entrado con su número de membresía
                                    SesionActual.fijarCorreo(fila.gmail.ifBlank { id })
                                    SesionActual.fijarDatos(fila.id, fila.nombre, fila.codigoQr)
                                    onLogin(rolEncontrado)
                                } else {
                                    error = strInvalidCreds
                                }
                            } catch (e: Exception) {
                                val msg = e.message?.substringBefore("\nCode:") ?: ""
                                error = if ("bloqueada" in msg.lowercase() || "locked" in msg.lowercase()) {
                                    val minutos = Regex("""(\d+)""").find(msg)?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                    context.applicationContext.getString(R.string.account_locked_minutes, minutos)
                                } else {
                                    "$strConnErrorPrefix$msg"
                                }
                            }
                            loading = false
                        }
                    },
                    enabled = !loading && terminosAceptados,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotonFondo,
                        contentColor = BotonTexto,
                        disabledContainerColor = BotonFondo.copy(alpha = 0.5f),
                        disabledContentColor = BotonTexto.copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(1.dp, DoradoBorde),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text(if (loading) stringResource(R.string.logging_in) else stringResource(R.string.login_button))
                }
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = RojoError, style = MaterialTheme.typography.bodySmall)
                }
                aviso?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = Dorado, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { mostrarRestablecer = true; aviso = null }) {
                    Text(stringResource(R.string.forgot_current_password), color = Dorado)
                }
            }
        }
    }

    // Olvidé mi contraseña: contraseña nueva + código por correo
    if (mostrarRestablecer) {
        RestablecerContrasenaDialog(
            correoInicial = identifier.trim(),
            correoEditable = true,
            colores = ColoresDialogo(
                fondo = TarjetaOscura, texto = BlancoTexto, textoSec = BlancoTexto.copy(alpha = 0.7f),
                acento = Dorado, borde = DoradoBorde, error = RojoError,
                boton = BotonFondo, textoBoton = BotonTexto,
                campo = MoradoCampo
            ),
            onDismiss = { mostrarRestablecer = false },
            onListo = {
                mostrarRestablecer = false
                password = ""
                aviso = strPassUpdated
            }
        )
    }
}