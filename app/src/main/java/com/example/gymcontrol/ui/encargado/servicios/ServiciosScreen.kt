package com.example.gymcontrol.ui.encargado.servicios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")

// Borde degradado en tonos rojos para el cuadro de eliminar
private val DeleteBorder = Brush.linearGradient(
    listOf(Color(0xFFFF6B6B), GymColors.Red, Color(0xFF7A0A0A))
)

@Composable
fun ServiciosScreen(onNavigate: (String) -> Unit = {}) {
    var showForm by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    // Nombre del servicio que se quiere eliminar (null = cuadro cerrado)
    var deleting by remember { mutableStateOf<String?>(null) }
    // Solo para la demo: oculta de la lista lo que se "elimina". TODO: quitar cuando haya validación real
    val removed = remember { mutableStateListOf<String>() }

    val allServices = GymApp.repository.services().filter { it.name !in removed }

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

                itemsIndexed(allServices) { index, service ->
                    ServiceCard(
                        name = service.name,
                        duration = "${service.duration}",
                        price = "$${service.price}",
                        onEdit = {
                            editingIndex = index
                            name = service.name
                            duration = "${service.duration}"
                            price = "${service.price}"
                            showForm = true
                        },
                        onDelete = { deleting = service.name }
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
                        // TODO: si editingIndex != null, actualizar ese servicio en GymApp.repository
                        // TODO: si editingIndex == null, crear uno nuevo en GymApp.repository
                        closeForm()
                    }
                )
            }
        }
    }

    // Cuadro de confirmación para eliminar
    deleting?.let { target ->
        DeleteConfirmDialog(
            title = "¿Eliminar servicio?",
            message = "Se eliminará el servicio $target. Esta acción no se puede deshacer.",
            onConfirm = {
                // TODO: eliminar el servicio en GymApp.repository
                removed.add(target)
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
            Text(title, color = GymColors.TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(message, color = GymColors.TextSecondary)
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

            GymTextField(value = name, onValueChange = onNameChange, label = "Nombre")
            Spacer(Modifier.height(10.dp))
            GymTextField(value = duration, onValueChange = onDurationChange, label = "Duración")
            Spacer(Modifier.height(10.dp))
            GymTextField(value = price, onValueChange = onPriceChange, label = "Costo")

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Guardar" else "Crear Servicio", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GymTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = GymColors.TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GymColors.Purple,
            unfocusedBorderColor = GymColors.Border,
            focusedTextColor = GymColors.TextPrimary,
            unfocusedTextColor = GymColors.TextPrimary,
            cursorColor = GymColors.Gold
        ),
        shape = RoundedCornerShape(10.dp)
    )
}