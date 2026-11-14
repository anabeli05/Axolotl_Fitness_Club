package com.example.gymcontrol.ui.recepcion.visitas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.data.remote.ServiciosRepository
import com.example.gymcontrol.data.remote.VisitaRow
import com.example.gymcontrol.data.remote.VisitasRepository
import com.example.gymcontrol.ui.components.SesionActual
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.dinero
import com.example.gymcontrol.ui.components.metodoPagoLocalizado
import kotlinx.coroutines.launch

private val METODOS = listOf("Efectivo", "Tarjeta")

private fun letras(s: String) = s.count { it.isLetter() }

/**
 * Cobro de visitas (pase de un día). No crea cliente ni membresía: solo guarda
 * los datos del visitante y el cobro, que entra directo en los ingresos.
 * El precio es el del servicio marcado como "Visita" en Servicios. El Encargado
 * siempre puede cobrarla; Recepción solo si la visita es visible para Recepción.
 */
@Composable
fun VisitasScreen(onNavigate: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var precio by remember { mutableStateOf<Double?>(null) }
    var visibleRecepcion by remember { mutableStateOf(true) }
    val esEncargado = SesionActual.rol == UserRole.ENCARGADO
    var visitas by remember { mutableStateOf<List<VisitaRow>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var procesando by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var contactoNombre by remember { mutableStateOf("") }
    var contactoTelefono by remember { mutableStateOf("") }
    var metodo by remember { mutableStateOf(METODOS.first()) }

    suspend fun recargar() {
        try {
            val visita = ServiciosRepository.listar().firstOrNull { it.es_visita }
            precio = visita?.precio ?: 0.0
            visibleRecepcion = visita?.visible_recepcion ?: false
            visitas = VisitasRepository.hoy()
            error = null
        } catch (e: Exception) {
            error = ctx.applicationContext.getString(R.string.visits_load_error, e.message?.substringBefore("\nCode:").orEmpty())
        }
        cargando = false
    }

    LaunchedEffect(Unit) { recargar() }

    val precioFijado = (precio ?: 0.0) > 0.0
    val disponible = precioFijado && (esEncargado || visibleRecepcion)
    val formValido = letras(nombre) >= 3 && telefono.length == 10 &&
        letras(contactoNombre) >= 3 && contactoTelefono.length == 10

    GymScaffold(currentSection = "Visitas", onNavigate = onNavigate) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.visits_title),
                    color = ClienteTema.Texto,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        cargando -> stringResource(R.string.common_loading)
                        !precioFijado -> stringResource(R.string.visits_no_price)
                        !disponible -> stringResource(R.string.visits_not_for_reception)
                        else -> stringResource(R.string.visits_price, dinero(precio ?: 0.0))
                    },
                    color = if (cargando || disponible) ClienteTema.AzulSel else ClienteTema.Rojo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            mensaje?.let { item { Text(it, color = ClienteTema.Verde, fontSize = 14.sp) } }
            error?.let { item { Text(it, color = ClienteTema.Rojo, fontSize = 14.sp) } }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
                    border = BorderStroke(1.5.dp, ClienteTema.Borde)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.visits_visitor), color = ClienteTema.Texto, fontWeight = FontWeight.Bold)
                        Campo(nombre, { if (it.length <= 60 && it.all { c -> c.isLetter() || c == ' ' }) nombre = it },
                            stringResource(R.string.visits_name), enabled = !procesando)
                        Campo(telefono, { if (it.length <= 10 && it.all(Char::isDigit)) telefono = it },
                            stringResource(R.string.visits_phone), KeyboardType.Phone, !procesando)

                        Text(stringResource(R.string.visits_emergency), color = ClienteTema.Texto, fontWeight = FontWeight.Bold)
                        Campo(contactoNombre, { if (it.length <= 60 && it.all { c -> c.isLetter() || c == ' ' }) contactoNombre = it },
                            stringResource(R.string.visits_name), enabled = !procesando)
                        Campo(contactoTelefono, { if (it.length <= 10 && it.all(Char::isDigit)) contactoTelefono = it },
                            stringResource(R.string.visits_phone), KeyboardType.Phone, !procesando)

                        Text(stringResource(R.string.visits_method), color = ClienteTema.Texto, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            METODOS.forEach { m ->
                                FilterChip(
                                    selected = metodo == m,
                                    onClick = { metodo = m },
                                    enabled = !procesando,
                                    label = {
                                        Text(metodoPagoLocalizado(m))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ClienteTema.AzulSel.copy(alpha = 0.2f),
                                        selectedLabelColor = ClienteTema.Texto,
                                        labelColor = ClienteTema.TextoSec
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = { confirmar = true },
                            enabled = disponible && formValido && !procesando,
                            colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Verde),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.visits_charge, dinero(precio ?: 0.0)),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.visits_today, visitas.size),
                    color = ClienteTema.Texto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(visitas, key = { it.id }) { v -> VisitaCard(v) }

            if (visitas.isEmpty() && !cargando) {
                item {
                    Text(
                        stringResource(R.string.visits_none_today),
                        color = ClienteTema.TextoSec,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { if (!procesando) confirmar = false },
            containerColor = ClienteTema.Beige,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    stringResource(R.string.visits_confirm_title),
                    color = ClienteTema.Texto,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.visits_confirm_message, dinero(precio ?: 0.0), nombre.trim(),
                        metodoPagoLocalizado(metodo)
                    ),
                    color = ClienteTema.TextoSec,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = !procesando,
                    colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Verde),
                    onClick = {
                        scope.launch {
                            procesando = true
                            try {
                                VisitasRepository.registrar(nombre, telefono, contactoNombre, contactoTelefono, metodo)
                                mensaje = ctx.getString(R.string.visits_registered, nombre.trim())
                                nombre = ""; telefono = ""; contactoNombre = ""; contactoTelefono = ""
                                metodo = METODOS.first()
                                error = null
                                recargar()
                            } catch (e: Exception) {
                                mensaje = null
                                error = ctx.getString(R.string.visits_save_error, e.message?.substringBefore("\nCode:").orEmpty())
                            } finally {
                                procesando = false
                                confirmar = false
                            }
                        }
                    }
                ) { Text(stringResource(R.string.visits_confirm_button)) }
            },
            dismissButton = {
                TextButton(enabled = !procesando, onClick = { confirmar = false }) {
                    Text(stringResource(R.string.cancel), color = ClienteTema.TextoSec)
                }
            }
        )
    }
}

@Composable
private fun Campo(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ClienteTema.Texto,
            unfocusedTextColor = ClienteTema.Texto,
            focusedBorderColor = ClienteTema.AzulSel,
            unfocusedBorderColor = ClienteTema.Borde,
            focusedLabelColor = ClienteTema.AzulSel,
            unfocusedLabelColor = ClienteTema.TextoSec,
            cursorColor = ClienteTema.AzulSel
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun VisitaCard(v: VisitaRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
        border = BorderStroke(1.dp, ClienteTema.Borde)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(v.nombre, color = ClienteTema.Texto, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${v.telefono} · ${v.hora}", color = ClienteTema.TextoSec, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(dinero(v.monto), color = ClienteTema.AzulSel, fontWeight = FontWeight.Bold)
                Text(metodoPagoLocalizado(v.metodo), color = ClienteTema.TextoSec, fontSize = 12.sp)
            }
        }
    }
}
