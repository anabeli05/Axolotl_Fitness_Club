package com.example.gymcontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.DatosSalud
import com.example.gymcontrol.data.remote.TIPOS_SANGRE
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val MAX_TEXTO_SALUD = 300
private const val EDAD_MAXIMA = 100

/** La fecha de nacimiento es válida si es pasada y de hace menos de 100 años. */
fun fechaNacimientoValida(iso: String?): Boolean {
    val f = iso?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
    val hoy = LocalDate.now(ZONA_MX)
    return f.isBefore(hoy) && !f.isBefore(hoy.minusYears(EDAD_MAXIMA.toLong()))
}

private val ZONA_MX: ZoneId = ZoneId.of("America/Mexico_City")

/**
 * Fecha de nacimiento, tipo de sangre, alergias, padecimientos y medicamentos.
 * Se usa en los formularios de cliente de Recepción y del Encargado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatosSaludCampos(
    datos: DatosSalud,
    onChange: (DatosSalud) -> Unit,
    fechaObligatoria: Boolean
) {
    Column {
        Text(
            stringResource(R.string.client_form_health_title),
            color = ClienteTema.Texto,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))

        CampoFechaNacimiento(
            valor = datos.fechaNacimiento,
            obligatoria = fechaObligatoria,
            onChange = { onChange(datos.copy(fechaNacimiento = it)) }
        )
        Spacer(Modifier.height(10.dp))

        // Tipo de sangre (opcional)
        var menu by remember { mutableStateOf(false) }
        val noEspecificado = stringResource(R.string.client_form_blood_type_unknown)
        ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
            OutlinedTextField(
                value = datos.tipoSangre ?: noEspecificado,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.client_form_blood_type), color = ClienteTema.TextoSec) },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = ClienteTema.TextoSec) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = coloresCampo()
            )
            ExposedDropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                modifier = Modifier.background(ClienteTema.Navy)
            ) {
                (listOf<String?>(null) + TIPOS_SANGRE).forEach { tipo ->
                    DropdownMenuItem(
                        text = { Text(tipo ?: noEspecificado, color = ClienteTema.Texto) },
                        onClick = {
                            onChange(datos.copy(tipoSangre = tipo))
                            menu = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        CampoTextoSalud(datos.alergias, stringResource(R.string.client_form_allergies)) {
            onChange(datos.copy(alergias = it))
        }
        Spacer(Modifier.height(10.dp))
        CampoTextoSalud(datos.padecimientos, stringResource(R.string.client_form_conditions)) {
            onChange(datos.copy(padecimientos = it))
        }
        Spacer(Modifier.height(10.dp))
        CampoTextoSalud(datos.medicamentos, stringResource(R.string.client_form_medications)) {
            onChange(datos.copy(medicamentos = it))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoFechaNacimiento(valor: String?, obligatoria: Boolean, onChange: (String) -> Unit) {
    var picker by remember { mutableStateOf(false) }
    val fecha = valor?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val texto = fecha?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: ""
    val edad = fecha?.let { Period.between(it, LocalDate.now(ZONA_MX)).years }
    val label = stringResource(
        if (obligatoria) R.string.client_form_birthdate_required else R.string.client_form_birthdate
    )

    Box {
        OutlinedTextField(
            value = texto,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label, color = ClienteTema.TextoSec) },
            placeholder = { Text("dd/mm/aaaa", color = ClienteTema.TextoSec) },
            supportingText = edad?.let { { Text(stringResource(R.string.client_form_age, it), color = ClienteTema.TextoSec) } },
            trailingIcon = {
                IconButton(onClick = { picker = true }) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = stringResource(R.string.client_form_choose_date),
                        tint = ClienteTema.AzulSel
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = coloresCampo()
        )
        // Tocar cualquier parte del campo abre el calendario
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 56.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    picker = true
                }
        )
    }

    if (picker) {
        val hoy = LocalDate.now(ZONA_MX)
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha?.let { aMillis(it) },
            // Escribir la fecha es más rápido que recorrer años en el calendario
            initialDisplayMode = DisplayMode.Input,
            yearRange = (hoy.year - EDAD_MAXIMA)..hoy.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    fechaNacimientoValida(deMillis(utcTimeMillis).toString())
            }
        )
        CalendarioDialog(
            titulo = label,
            estado = estado,
            onDismiss = { picker = false },
            onAceptar = {
                estado.selectedDateMillis?.let { ms ->
                    val elegida = deMillis(ms).toString()
                    if (fechaNacimientoValida(elegida)) onChange(elegida)
                }
                picker = false
            }
        )
    }
}

@Composable
private fun CampoTextoSalud(valor: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { if (it.length <= MAX_TEXTO_SALUD) onChange(it) },
        label = { Text(label, color = ClienteTema.TextoSec) },
        placeholder = { Text(stringResource(R.string.client_form_none_placeholder), color = ClienteTema.TextoSec) },
        maxLines = 3,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = coloresCampo()
    )
}

// El DatePicker trabaja con medianoche UTC
private fun aMillis(f: LocalDate) = f.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun deMillis(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

/** Colores de los campos de texto de los formularios (también los usa el calendario). */
@Composable
internal fun coloresCampo() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ClienteTema.AzulSel,
    unfocusedBorderColor = ClienteTema.Borde,
    focusedTextColor = ClienteTema.Texto,
    unfocusedTextColor = ClienteTema.Texto,
    focusedLabelColor = ClienteTema.AzulSel,
    unfocusedLabelColor = ClienteTema.TextoSec,
    focusedPlaceholderColor = ClienteTema.TextoSec,
    unfocusedPlaceholderColor = ClienteTema.TextoSec,
    cursorColor = ClienteTema.AzulSel
)
