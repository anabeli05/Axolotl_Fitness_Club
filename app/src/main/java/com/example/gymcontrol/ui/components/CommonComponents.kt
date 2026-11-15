package com.example.gymcontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.R
import com.example.gymcontrol.data.model.HourOccupancy

/** Forma de píldora estándar para botones principales y secundarios en toda la app. */
val GymButtonShape = RoundedCornerShape(50)

/** Radio estándar para tarjetas y contenedores de datos. */
val GymCardShape = RoundedCornerShape(20.dp)

/** Forma de píldora estándar para campos de texto de una línea. */
val GymFieldShape = RoundedCornerShape(50)

/** Radio estándar para campos de texto multilínea. */
val GymMultilineFieldShape = RoundedCornerShape(16.dp)

/** Altura mínima uniforme para botones (46.dp). */
val GymButtonMinHeight = 46.dp

/** Padding de contenido equilibrado para botones. */
val GymButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)

/** Texto estándar para el interior de botones y badges. */
@Composable
fun GymButtonText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
    )
}

/** Colores estándar de OutlinedTextField según el tema activo (ClienteTema). */
@Composable
fun gymFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = ClienteTema.CampoFondo,
    unfocusedContainerColor = ClienteTema.CampoFondo,
    focusedBorderColor = ClienteTema.AzulSel,
    unfocusedBorderColor = ClienteTema.Borde,
    focusedTextColor = ClienteTema.Texto,
    unfocusedTextColor = ClienteTema.Texto,
    focusedLabelColor = ClienteTema.AzulSel,
    unfocusedLabelColor = ClienteTema.TextoSec,
    focusedPlaceholderColor = ClienteTema.TextoSec,
    unfocusedPlaceholderColor = ClienteTema.TextoSec,
    cursorColor = ClienteTema.AzulSel,
    errorBorderColor = ClienteTema.Rojo,
    errorTextColor = ClienteTema.Texto,
    errorLabelColor = ClienteTema.Rojo,
    errorCursorColor = ClienteTema.Rojo,
    errorSupportingTextColor = ClienteTema.Rojo
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenContainer(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
fun OccupancyChart(data: List<HourOccupancy>, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.common_people_per_hour), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            val max = (data.maxOfOrNull { it.people } ?: 1).coerceAtLeast(1)
            data.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.hour, modifier = Modifier.width(54.dp))
                    LinearProgressIndicator(
                        progress = { item.people.toFloat() / max.toFloat() },
                        modifier = Modifier.weight(1f).height(10.dp)
                    )
                    Text("${item.people}", modifier = Modifier.width(36.dp))
                }
            }
        }
    }
}

@Composable
fun SimpleEmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
