package com.example.gymcontrol.ui.encargado.servicios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors
import kotlinx.coroutines.launch
import com.example.gymcontrol.data.remote.ServicioRow
import com.example.gymcontrol.data.remote.ServiciosRepository

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")

// Borde degradado en tonos rojos para el cuadro de eliminar
private val DeleteBorder = Brush.linearGradient(
    listOf(Color(0xFFFF6B6B), GymColors.Red, Color(0xFF7A0A0A))
)

// El nombre debe tener al menos `min` letras (espacios y números no cuentan)
private fun nameHasMinLetters(s: String, min: Int = 3) = s.count { it.isLetter() } >= min

@Composable
fun ServiciosScreen(onNavigate: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var servicios by remember { mutableStateOf<List<ServicioRow>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var showForm by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<ServicioRow?>(null) }

    fun recargar() {
        scope.launch {
            cargando = true
            try {
                servicios = ServiciosRepository.listar()
                errorMsg = null
            } catch (e: Exception) {
                errorMsg = "No se pudo cargar: ${e.message}"
            }
            cargando = false
        }
    }

    LaunchedEffect(Unit) { recargar() }

    fun closeForm() {
        showForm = false
        editingIndex = null
        name = ""
        duration = ""
        price = ""
    }

    GymScaffold(
        currentSection = "Servicios",
        sections = ADMIN_SECTIONS,
        onNavigate = onNavigate
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Button(
                        onClick = {
                            editingIndex = null
                            name = ""
                            duration = ""
                            price = ""
                            showForm = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nuevo Servicio", fontWeight = FontWeight.Bold)
                    }
                }

                if (cargando) {
                    item { Text("Cargando...", color = GymColors.TextSecondary) }
                }
                errorMsg?.let { msg ->
                    item { Text(msg, color = GymColors.Red) }
                }

                itemsIndexed(servicios) { index, service ->
                    ServiceCard(
                        name = service.nombre,
                        duration = service.duracion,
                        price = "$${service.precio}",
                        onEdit = {
                            editingIndex = index
                            name = service.nombre
                            duration = service.duracion.filter { it.isDigit() }
                            price = service.precio.toString()
                            showForm = true
                        },
                        onDelete = { deleting = service }
                    )
                }
            }

            if (showForm) {
                ServiceFormDialog(
                    isEditing = editingIndex != null,
                    name = name,
                    duration = duration,
                    price = price,
                    onNameChange = { name = it },
                    onDurationChange = { duration = it },
                    onPriceChange = { price = it },
                    onDismiss = { closeForm() },
                    onSave = {
                        val nombre = name.trim()
                        val dias = duration.toInt()
                        val dur = if (dias == 1) "1 día" else "$dias días"
                        val precio = price.toDouble()
                        val idx = editingIndex
                        scope.launch {
                            try {
                                if (idx != null) {
                                    ServiciosRepository.actualizar(servicios[idx].id, nombre, dur, precio)
                                } else {
                                    ServiciosRepository.crear(nombre, dur, precio)
                                }
                                errorMsg = null
                            } catch (e: Exception) {
                                errorMsg = "No se pudo guardar: ${e.message}"
                            }
                            recargar()
                        }
                        closeForm()
                    }
                )
            }
        }
    }

    deleting?.let { target ->
        DeleteConfirmDialog(
            title = "¿Eliminar servicio?",
            message = "Se eliminará el servicio ${target.nombre}. Esta acción no se puede deshacer.",
            onConfirm = {
                scope.launch {
                    try {
                        ServiciosRepository.eliminar(target.id)
                        errorMsg = null
                    } catch (e: Exception) {
                        errorMsg = "No se pudo eliminar: ${e.message}"
                    }
                    recargar()
                }
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.5.dp, DeleteBorder, RoundedCornerShape(16.dp)),
        containerColor = GymColors.Surface,
        shape = RoundedCornerShape(16.dp),
        icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = GymColors.Red) },
        title = {
            Text(
                title,
                color = GymColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                message,
                color = GymColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GymColors.Red,
                    contentColor = Color.White
                )
            ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GymColors.Border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.TextPrimary)
            ) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ServiceCard(
    name: String,
    duration: String,
    price: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderBrush = Brush.linearGradient(colors = listOf(GymColors.Purple, GymColors.Gold))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GymColors.Surface, RoundedCornerShape(14.dp))
            .border(1.5.dp, borderBrush, RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = GymColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Duración: $duration",
                color = GymColors.TextSecondary,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Precio: $price",
                color = GymColors.Gold,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconCircleButton(
                icon = Icons.Filled.Edit,
                color = GymColors.Orange,
                onClick = onEdit
            )
            IconCircleButton(
                icon = Icons.Filled.Close,
                color = GymColors.Red,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun IconCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(1.5.dp, color, CircleShape)
            .background(color.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ServiceFormDialog(
    isEditing: Boolean,
    name: String,
    duration: String,
    price: String,
    onNameChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val borderBrush = Brush.linearGradient(colors = listOf(GymColors.Purple, GymColors.Gold))
    val nameValid = nameHasMinLetters(name)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(GymColors.Surface, RoundedCornerShape(16.dp))
                .border(1.5.dp, borderBrush, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Editar servicio" else "Nuevo servicio",
                    color = GymColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = GymColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            RestrictedField(
                value = name,
                onValueChange = onNameChange,
                label = "Nombre",
                accept = { s -> s.all { it.isLetterOrDigit() || it == ' ' } },
                maxLength = 30,
                warningText = "Solo se permiten letras y números",
                errorText = if (name.isNotEmpty() && !nameValid) "Mínimo 3 letras" else null
            )
            Spacer(Modifier.height(10.dp))
            RestrictedField(
                value = duration,
                onValueChange = onDurationChange,
                label = "Duración (días)",
                accept = { s -> s.all { it in '0'..'9' } },
                maxLength = 3,
                warningText = "Solo se permiten números",
                keyboardType = KeyboardType.Number,
                errorText = if (duration.isNotEmpty() && duration.toInt() == 0) "Debe ser mayor a 0" else null
            )
            Spacer(Modifier.height(10.dp))
            RestrictedField(
                value = price,
                onValueChange = onPriceChange,
                label = "Costo",
                accept = { s -> MONEY_REGEX.matches(s) },
                maxLength = 9,
                warningText = "Solo números (máx. 2 decimales)",
                keyboardType = KeyboardType.Decimal,
                prefix = "$"
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onSave,
                enabled = nameValid &&
                        (duration.toIntOrNull() ?: 0) > 0 &&
                        (price.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GymColors.Purple,
                    contentColor = GymColors.TextPrimary,
                    disabledContainerColor = GymColors.Purple.copy(alpha = 0.45f),
                    disabledContentColor = GymColors.TextPrimary.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Guardar" else "Crear Servicio", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GymColors.Purple,
    unfocusedBorderColor = GymColors.Border,
    focusedTextColor = GymColors.TextPrimary,
    unfocusedTextColor = GymColors.TextPrimary,
    focusedLabelColor = GymColors.Purple,
    cursorColor = GymColors.Gold,
    errorBorderColor = GymColors.Red,
    errorTextColor = GymColors.TextPrimary,
    errorLabelColor = GymColors.Red,
    errorCursorColor = GymColors.Red,
    errorSupportingTextColor = GymColors.Red
)

// Campo que rechaza lo que no cumple `accept` (muestra un aviso unos segundos)
// y limita la longitud. `errorText` es un error fijo (ej. "faltan dígitos").
@Composable
private fun RestrictedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accept: (String) -> Boolean,
    maxLength: Int,
    warningText: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: String? = null,
    prefix: String? = null
) {
    var warning by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(tick) {
        if (tick > 0) {
            warning = true
            delay(2500)
            warning = false
        }
    }

    val shownError = if (warning) warningText else errorText

    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            when {
                !accept(new) -> tick++
                new.length <= maxLength -> onValueChange(new)
            }
        },
        label = { Text(label, color = if (shownError != null) GymColors.Red else GymColors.TextSecondary) },
        singleLine = true,
        isError = shownError != null,
        supportingText = if (shownError != null) {
            { Text(shownError) }
        } else null,
        prefix = if (prefix != null) {
            { Text(prefix, color = GymColors.TextPrimary) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = fieldColors()
    )
}

private val MONEY_REGEX = Regex("^\\d{0,6}(\\.\\d{0,2})?$")