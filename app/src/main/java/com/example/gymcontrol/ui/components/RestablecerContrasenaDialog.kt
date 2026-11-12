package com.example.gymcontrol.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.AuthRepository
import com.example.gymcontrol.data.remote.CorreoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Colores del diálogo: el Login es oscuro y el Perfil usa el tema del cliente. */
data class ColoresDialogo(
    val fondo: Color,
    val texto: Color,
    val textoSec: Color,
    val acento: Color,
    val borde: Color,
    val error: Color,
    val boton: Color,
    val textoBoton: Color
)

/**
 * Cambiar la contraseña sin recordar la actual:
 *  1. correo + contraseña nueva → se envía un código de 6 dígitos al correo registrado
 *  2. se escribe el código → la contraseña cambia
 *
 * @param correoEditable false en el Perfil (ya se sabe quién es); true en el Login.
 */
@Composable
fun RestablecerContrasenaDialog(
    correoInicial: String,
    correoEditable: Boolean,
    colores: ColoresDialogo,
    onDismiss: () -> Unit,
    onListo: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var correo by remember { mutableStateOf(correoInicial) }
    var nueva by remember { mutableStateOf("") }
    var repetir by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var codigoEnviado by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var espera by remember { mutableIntStateOf(0) }   // segundos para poder reenviar

    LaunchedEffect(espera) {
        if (espera > 0) {
            delay(1000)
            espera--
        }
    }

    val coincide = nueva == repetir
    val datosListos = correo.contains("@") && nueva.length >= 8 && coincide

    fun enviarCodigo() {
        scope.launch {
            cargando = true
            error = null
            try {
                AuthRepository.revisarContrasenaNueva(nueva)?.let { motivo ->
                    error = motivo
                    return@launch
                }
                val falla = CorreoRepository.enviarCodigoContrasena(correo)
                if (falla == null) {
                    codigoEnviado = true
                    codigo = ""
                    espera = 60
                } else {
                    error = falla
                }
            } catch (e: Exception) {
                error = e.message?.substringBefore("\nCode:") ?: "No se pudo enviar el código"
            } finally {
                cargando = false
            }
        }
    }

    @Composable
    fun campo(
        valor: String, cambio: (String) -> Unit, etiqueta: String,
        tipo: KeyboardType, clave: Boolean = false, habilitado: Boolean = true
    ) {
        var visible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = valor,
            onValueChange = cambio,
            label = { Text(etiqueta) },
            singleLine = true,
            enabled = habilitado && !cargando,
            visualTransformation = if (clave && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = tipo),
            trailingIcon = if (clave) {
                {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (visible) stringResource(R.string.hide_password)
                            else stringResource(R.string.show_password)
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colores.texto,
                unfocusedTextColor = colores.texto,
                disabledTextColor = colores.textoSec,
                focusedBorderColor = colores.acento,
                unfocusedBorderColor = colores.borde,
                disabledBorderColor = colores.borde.copy(alpha = 0.4f),
                focusedLabelColor = colores.acento,
                unfocusedLabelColor = colores.textoSec,
                disabledLabelColor = colores.textoSec,
                cursorColor = colores.acento,
                focusedTrailingIconColor = colores.acento,
                unfocusedTrailingIconColor = colores.textoSec
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }

    AlertDialog(
        onDismissRequest = { if (!cargando) onDismiss() },
        containerColor = colores.fondo,
        title = {
            Text(
                if (codigoEnviado) "Revisa tu correo" else "Olvidé mi contraseña",
                color = colores.texto,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (!codigoEnviado) {
                    Text(
                        "Escribe tu contraseña nueva. Te enviaremos un código a tu correo registrado para confirmar que eres tú.",
                        color = colores.textoSec,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(10.dp))
                    campo(correo, { correo = it.trim() }, stringResource(R.string.login_email),
                        KeyboardType.Email, habilitado = correoEditable)
                    campo(nueva, { nueva = it }, stringResource(R.string.new_password), KeyboardType.Password, clave = true)
                    campo(repetir, { repetir = it }, stringResource(R.string.repeat_new_password), KeyboardType.Password, clave = true)
                    Text(
                        stringResource(R.string.password_requirements),
                        color = colores.textoSec,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (repetir.isNotEmpty() && !coincide) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.passwords_dont_match), color = colores.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text(
                        "Si $correo está registrado, te llegó un código de 6 dígitos. Escríbelo aquí (vence en 15 minutos). Revisa también la carpeta de spam.",
                        color = colores.textoSec,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(10.dp))
                    campo(codigo, { v -> codigo = v.filter { it.isDigit() }.take(6) }, "Código", KeyboardType.NumberPassword)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            enabled = !cargando,
                            onClick = { codigoEnviado = false; error = null }
                        ) { Text("Cambiar datos", color = colores.textoSec) }
                        TextButton(
                            enabled = !cargando && espera == 0,
                            onClick = { enviarCodigo() }
                        ) {
                            Text(
                                if (espera > 0) "Reenviar en $espera s" else "Reenviar código",
                                color = if (espera > 0) colores.textoSec else colores.acento
                            )
                        }
                    }
                }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = colores.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                TextButton(enabled = !cargando, onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = colores.textoSec)
                }
                Button(
                    enabled = !cargando && if (codigoEnviado) codigo.length == 6 else datosListos,
                    colors = ButtonDefaults.buttonColors(containerColor = colores.boton, contentColor = colores.textoBoton),
                    onClick = {
                        if (!codigoEnviado) {
                            enviarCodigo()
                        } else scope.launch {
                            cargando = true
                            error = null
                            try {
                                AuthRepository.restablecerContrasena(correo, codigo, nueva)
                                onListo()
                            } catch (e: Exception) {
                                error = e.message?.substringBefore("\nCode:") ?: "No se pudo cambiar la contraseña"
                            } finally {
                                cargando = false
                            }
                        }
                    }
                ) {
                    if (cargando) {
                        CircularProgressIndicator(color = colores.textoBoton, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(if (codigoEnviado) "Confirmar" else "Enviar código")
                    }
                }
            }
        }
    )
}
