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
import com.example.gymcontrol.ui.components.LocaleHelper
import com.example.gymcontrol.ui.components.SesionActual
import kotlinx.coroutines.launch

// Paleta tomada del diseño
private val FondoNegro = Color(0xFF000000)
private val TarjetaOscura = Color(0xFF1B1B1B)
private val MoradoCampo = Color(0xFF2F2E2E)
private val MoradoBorde = Color(0xFF6B5326)
private val DoradoBorde = Color(0xFFC9A24B)
private val Dorado = Color(0xFFC9A24B)
private val BlancoTexto = Color(0xFFFFFFFF)
private val RojoError = Color(0xFFFF6B6B)

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
    var mostrarCambio by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val strInvalidCreds = stringResource(R.string.error_invalid_credentials)
    val strConnErrorPrefix = stringResource(R.string.error_connection_prefix)
    val strPassUpdated = stringResource(R.string.password_updated_success)

    val campoShape = RoundedCornerShape(50)
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
                .clip(RoundedCornerShape(28.dp))
                .background(TarjetaOscura)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(MoradoBorde, DoradoBorde)),
                    shape = RoundedCornerShape(28.dp)
                )
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
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
                val textoTerminos = buildAnnotatedString {
                    append(stringResource(R.string.accept_terms_prefix))
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
                                onNavigateToTerms()
                            }
                        )
                    ) {
                        append(stringResource(R.string.terms_and_conditions))
                    }
                    append(stringResource(R.string.accept_terms_and))
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
                                onNavigateToPrivacy()
                            }
                        )
                    ) {
                        append(stringResource(R.string.privacy_policy))
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
                            error = if ("bloqueada" in msg) msg else "$strConnErrorPrefix$msg"
                        }
                        loading = false
                    }
                },
                enabled = !loading && terminosAceptados,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoradoCampo,
                    contentColor = BlancoTexto,
                    disabledContainerColor = MoradoCampo.copy(alpha = 0.5f),
                    disabledContentColor = BlancoTexto.copy(alpha = 0.7f)
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
            TextButton(onClick = { mostrarCambio = true; aviso = null }) {
                Text(stringResource(R.string.change_password), color = Dorado)
            }
        }

        IconButton(
            onClick = { LocaleHelper.toggleLanguage(context) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = stringResource(R.string.change_language),
                tint = Dorado
            )
        }
    }

    if (mostrarCambio) {
        CambiarContrasenaDialog(
            correoInicial = identifier.trim(),
            onDismiss = { mostrarCambio = false },
            onListo = {
                mostrarCambio = false
                password = ""
                aviso = strPassUpdated
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CambiarContrasenaDialog(
    correoInicial: String,
    onDismiss: () -> Unit,
    onListo: () -> Unit
) {
    var correo by remember { mutableStateOf(correoInicial) }
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var repetir by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val strChangeFailed = stringResource(R.string.error_change_failed)

    val coincide = nueva == repetir
    val puedeGuardar = !cargando && correo.isNotBlank() && actual.isNotBlank() &&
            nueva.length >= 8 && coincide

    @Composable
    fun campo(valor: String, cambio: (String) -> Unit, etiqueta: String, clave: Boolean) {
        var visible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = valor,
            onValueChange = cambio,
            label = { Text(etiqueta) },
            singleLine = true,
            visualTransformation = if (clave && !visible) PasswordVisualTransformation()
            else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (clave) KeyboardType.Password else KeyboardType.Email
            ),
            trailingIcon = if (clave) {
                {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            imageVector = if (visible) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = if (visible) stringResource(R.string.hide_password)
                            else stringResource(R.string.show_password)
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = BlancoTexto,
                unfocusedTextColor = BlancoTexto,
                focusedBorderColor = Dorado,
                unfocusedBorderColor = DoradoBorde,
                focusedLabelColor = Dorado,
                unfocusedLabelColor = BlancoTexto,
                cursorColor = Dorado,
                focusedTrailingIconColor = Dorado,
                unfocusedTrailingIconColor = BlancoTexto
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TarjetaOscura,
        title = {
            Text(
                stringResource(R.string.change_password),
                color = BlancoTexto,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                campo(correo, { correo = it }, stringResource(R.string.login_email), false)
                campo(actual, { actual = it }, stringResource(R.string.current_password), true)
                campo(nueva, { nueva = it }, stringResource(R.string.new_password), true)
                campo(repetir, { repetir = it }, stringResource(R.string.repeat_new_password), true)
                Text(
                    stringResource(R.string.password_requirements),
                    color = BlancoTexto.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                if (repetir.isNotEmpty() && !coincide) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.passwords_dont_match), color = RojoError,
                        style = MaterialTheme.typography.bodySmall)
                }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = RojoError, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = BlancoTexto) }

                Button(
                    enabled = puedeGuardar,
                    onClick = {
                        scope.launch {
                            cargando = true
                            error = null
                            try {
                                AuthRepository.cambiarContrasena(correo, actual, nueva)
                                onListo()
                            } catch (e: Exception) {
                                error = e.message?.substringBefore("\nCode:") ?: strChangeFailed
                            }
                            cargando = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoradoCampo, contentColor = BlancoTexto)
                ) { Text(if (cargando) stringResource(R.string.saving) else stringResource(R.string.save)) }
            }
        }
    )
}
