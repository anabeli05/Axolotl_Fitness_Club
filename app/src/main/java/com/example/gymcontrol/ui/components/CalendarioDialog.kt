package com.example.gymcontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymcontrol.R

/**
 * Calendario con el mismo estilo que los demás diálogos de la app
 * (tarjeta Beige, borde degradado y colores de ClienteTema, en modo claro u oscuro).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioDialog(
    titulo: String,
    estado: DatePickerState,
    onDismiss: () -> Unit,
    onAceptar: () -> Unit
) {
    val shape = GymCardShape
    Dialog(
        onDismissRequest = onDismiss,
        // El calendario necesita ~360 dp; el ancho estándar del diálogo lo recorta
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(ClienteTema.Beige)
                .border(BorderStroke(1.dp, Brush.linearGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel))), shape)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp)
        ) {
            DatePicker(
                state = estado,
                title = {
                    Text(
                        titulo,
                        color = ClienteTema.TextoSec,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    )
                },
                colors = coloresCalendario()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = GymButtonShape,
                    border = BorderStroke(1.dp, ClienteTema.Borde),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.TextoSec),
                    modifier = Modifier.defaultMinSize(minHeight = GymButtonMinHeight),
                    contentPadding = GymButtonPadding
                ) {
                    GymButtonText(stringResource(R.string.cancel), color = ClienteTema.TextoSec)
                }
                Button(
                    onClick = onAceptar,
                    shape = GymButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.AzulSel, contentColor = Color.White),
                    modifier = Modifier.defaultMinSize(minHeight = GymButtonMinHeight),
                    contentPadding = GymButtonPadding
                ) {
                    GymButtonText(stringResource(R.string.common_accept), color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun coloresCalendario() = DatePickerDefaults.colors(
    containerColor = ClienteTema.Beige,
    titleContentColor = ClienteTema.TextoSec,
    headlineContentColor = ClienteTema.Texto,
    weekdayContentColor = ClienteTema.TextoSec,
    subheadContentColor = ClienteTema.TextoSec,
    navigationContentColor = ClienteTema.Texto,
    yearContentColor = ClienteTema.Texto,
    disabledYearContentColor = ClienteTema.TextoSec.copy(alpha = 0.4f),
    currentYearContentColor = ClienteTema.AzulSel,
    selectedYearContentColor = Color.White,
    selectedYearContainerColor = ClienteTema.AzulSel,
    dayContentColor = ClienteTema.Texto,
    disabledDayContentColor = ClienteTema.TextoSec.copy(alpha = 0.4f),
    selectedDayContentColor = Color.White,
    selectedDayContainerColor = ClienteTema.AzulSel,
    todayContentColor = ClienteTema.AzulSel,
    todayDateBorderColor = ClienteTema.AzulSel,
    dividerColor = ClienteTema.Borde.copy(alpha = 0.6f),
    dateTextFieldColors = coloresCampo()
)
