package com.example.gymcontrol.ui.recepcion.clientes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.CANCELACION_ERROR_COBRO
import com.example.gymcontrol.data.remote.CANCELACION_VOLUNTARIA
import com.example.gymcontrol.data.remote.ClienteRow
import com.example.gymcontrol.data.remote.ClientesRepository
import com.example.gymcontrol.data.remote.EvaluacionCancelacion
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.components.dinero
import kotlinx.coroutines.launch

/**
 * Recepción registra la solicitud de cancelación; el Encargado la autoriza después desde
 * su sesión (Reportes). Antes de enviarla se muestra lo que aplica según la política
 * (lo calcula la base). La membresía sigue activa mientras la solicitud está pendiente.
 * [onListo] recibe el mensaje para mostrar en la lista de clientes.
 */
@Composable
fun CancelarMembresiaDialog(
    cliente: ClienteRow,
    onDismiss: () -> Unit,
    onListo: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tipo by remember { mutableStateOf(CANCELACION_VOLUNTARIA) }
    var evaluacion by remember { mutableStateOf<EvaluacionCancelacion?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var motivo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    fun mensaje(e: Exception) = e.message?.substringBefore("\nCode:")?.lineSequence()?.firstOrNull()
        ?: context.getString(R.string.cancel_membership_failed)

    LaunchedEffect(tipo) {
        cargando = true
        evaluacion = null
        error = null
        try {
            evaluacion = ClientesRepository.evaluarCancelacion(SesionActual.correo ?: "", cliente.id, tipo)
        } catch (e: Exception) {
            error = mensaje(e)
        }
        cargando = false
    }

    val campoColores = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ClienteTema.AzulSel,
        unfocusedBorderColor = ClienteTema.Borde,
        focusedTextColor = ClienteTema.Texto,
        unfocusedTextColor = ClienteTema.Texto,
        cursorColor = ClienteTema.AzulSel,
        focusedLabelColor = ClienteTema.AzulSel,
        unfocusedLabelColor = ClienteTema.TextoSec,
        focusedContainerColor = ClienteTema.CampoFondo,
        unfocusedContainerColor = ClienteTema.CampoFondo
    )

    val listo = evaluacion != null && !enviando && motivo.trim().length >= 3

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        containerColor = ClienteTema.Beige,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                stringResource(R.string.cancel_membership_title, cliente.nombre),
                color = ClienteTema.Texto,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                listOf(
                    CANCELACION_VOLUNTARIA to R.string.cancel_membership_type_voluntary,
                    CANCELACION_ERROR_COBRO to R.string.cancel_membership_type_billing_error
                ).forEach { (valor, etiqueta) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !enviando) { tipo = valor }
                    ) {
                        RadioButton(
                            selected = tipo == valor,
                            onClick = { tipo = valor },
                            enabled = !enviando,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ClienteTema.AzulSel,
                                unselectedColor = ClienteTema.TextoSec
                            )
                        )
                        Text(stringResource(etiqueta), color = ClienteTema.Texto)
                    }
                }

                Spacer(Modifier.height(8.dp))
                when {
                    cargando -> Text(stringResource(R.string.cancel_membership_checking), color = ClienteTema.TextoSec)
                    evaluacion != null -> ResultadoPolitica(evaluacion!!)
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it.take(300) },
                    label = { Text(stringResource(R.string.cancel_membership_reason)) },
                    enabled = !enviando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = campoColores
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.cancel_membership_pending_note),
                    color = ClienteTema.TextoSec,
                    fontSize = 13.sp
                )

                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = ClienteTema.Rojo, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !enviando,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClienteTema.Borde),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto)
                ) { Text(stringResource(R.string.cancel)) }

                Button(
                    onClick = {
                        enviando = true
                        error = null
                        scope.launch {
                            try {
                                ClientesRepository.solicitarCancelacion(
                                    SesionActual.correo ?: "", cliente.id, tipo, motivo
                                )
                                onListo(context.getString(R.string.cancel_membership_request_sent, cliente.nombre))
                            } catch (e: Exception) {
                                error = mensaje(e)
                                enviando = false
                            }
                        }
                    },
                    enabled = listo,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.Rojo,
                        contentColor = ClienteTema.SobreDorado
                    )
                ) { Text(stringResource(R.string.cancel_membership_send_request), fontWeight = FontWeight.Bold) }
            }
        }
    )
}

/** Recuadro con lo que aplica: verde con reembolso, rojo sin reembolso. También lo usa el Encargado. */
@Composable
fun ResultadoPolitica(e: EvaluacionCancelacion) {
    val context = LocalContext.current
    val color = if (e.conReembolso) ClienteTema.Verde else ClienteTema.Rojo
    val fondo = if (e.conReembolso) ClienteTema.VerdePastel else ClienteTema.RojoPastel
    val razon = when (e.regla) {
        "DENTRO_48H" -> stringResource(R.string.cancel_rule_within_48h, (e.horas ?: 0.0).toInt())
        "ERROR_COBRO" -> stringResource(R.string.cancel_rule_billing_error)
        "FUERA_48H" -> stringResource(R.string.cancel_rule_after_48h)
        "YA_INGRESO" -> stringResource(R.string.cancel_rule_already_entered)
        "INGRESO_TRAS_SOLICITUD" -> stringResource(R.string.cancel_rule_entered_after_request)
        else -> stringResource(R.string.cancel_rule_no_payment)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(fondo, RoundedCornerShape(10.dp))
            .border(1.dp, color, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            if (e.conReembolso) stringResource(R.string.cancel_refund_full, dinero(e.reembolso), e.metodo ?: "—")
            else stringResource(R.string.cancel_no_refund),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(razon, color = ClienteTema.Texto, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            if (e.conReembolso)
                stringResource(R.string.cancel_refund_deadline, ClientesRepository.fechaApp(context, e.fechaLimite))
            else stringResource(R.string.cancel_keeps_until, ClientesRepository.fechaApp(context, e.fechaFin)),
            color = ClienteTema.TextoSec,
            fontSize = 14.sp
        )
        if (e.conReembolso) {
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.cancel_refund_access_note), color = ClienteTema.TextoSec, fontSize = 13.sp)
        }
    }
}
