package com.example.gymcontrol.ui.recepcion.clientes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val CardBorder = Brush.linearGradient(listOf(GymColors.Purple, GymColors.Gold))
private val RECEPCION_SECTIONS = listOf("Home", "Clientes", "Solicitudes")

// Tamaño compartido por el badge (Activo/Inactivo), el lápiz y el botón "Renovar"
private val ActionButtonWidth = 96.dp
private val ActionButtonHeight = 36.dp

@Composable
fun ClientesScreen(onNavigate: (String) -> Unit = {}) {
    var search by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    // Solo para la demo: clientes renovados en esta sesión. TODO: quitar cuando se guarde en Supabase
    val renewed = remember { mutableStateListOf<String>() }

    val clients = GymApp.repository.users()
        .filter { it.role == UserRole.CLIENTE }
        .filter { it.name.contains(search, true) || (it.membershipNumber ?: "").contains(search, true) }

    // Cliente que se está editando (null cuando se crea uno nuevo)
    val editingClient = editingIndex?.let { clients.getOrNull(it) }

    GymScaffold(
        currentSection = "Clientes",
        sections = RECEPCION_SECTIONS,
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
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Nombre o membresía", color = GymColors.TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = GymColors.TextSecondary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GymColors.Purple,
                            unfocusedBorderColor = GymColors.Border,
                            focusedTextColor = GymColors.TextPrimary,
                            unfocusedTextColor = GymColors.TextPrimary,
                            cursorColor = GymColors.Gold
                        )
                    )
                }

                item {
                    Button(
                        onClick = {
                            editingIndex = null
                            showForm = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nuevo Cliente", fontWeight = FontWeight.Bold)
                    }
                }

                itemsIndexed(clients) { index, client ->
                    // Identifica al cliente: su membresía, o su nombre si no tiene
                    val clientKey = client.membershipNumber ?: client.name

                    ClientCard(
                        name = client.name,
                        membership = client.membershipNumber ?: "—",
                        registrationDate = client.registrationDate,
                        isActive = client.status.name == "ACTIVO" || clientKey in renewed,
                        onEdit = {
                            editingIndex = index
                            showForm = true
                        },
                        onRenovar = {
                            // TODO: registrar la renovación y el pago en Supabase
                            if (clientKey !in renewed) renewed.add(clientKey)
                        }
                    )
                }

                if (clients.isEmpty()) {
                    item {
                        Text(
                            text = "No se encontraron clientes",
                            color = GymColors.TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (showForm) {
                // Separa "Ana López" en nombre y apellido para llenar el formulario
                val nameParts = (editingClient?.name ?: "").trim().split(" ", limit = 2)

                ClientFormDialog(
                    isEditing = editingIndex != null,
                    initialName = nameParts.getOrElse(0) { "" },
                    initialLastName = nameParts.getOrElse(1) { "" },
                    // TODO: pasar el teléfono y el plan del cliente cuando el modelo los tenga
                    initialPhone = "",
                    initialEmail = editingClient?.let { "${it.email}" } ?: "",
                    initialPlan = null,
                    onDismiss = {
                        showForm = false
                        editingIndex = null
                    },
                    onSave = {
                        // TODO: si editingIndex != null, actualizar cliente en Supabase
                        // TODO: si editingIndex == null, crear uno nuevo (membresía se genera automática)
                        showForm = false
                        editingIndex = null
                    }
                )
            }
        }
    }
}

@Composable
private fun ClientCard(
    name: String,
    membership: String,
    registrationDate: String,
    isActive: Boolean,
    onEdit: () -> Unit,
    onRenovar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GymColors.Surface, RoundedCornerShape(14.dp))
            .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = GymColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text("Membresía: $membership", color = GymColors.TextSecondary, fontSize = 17.sp)
            Spacer(Modifier.height(2.dp))
            Text("Registro: $registrationDate", color = GymColors.TextSecondary, fontSize = 17.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            StatusBadge(isActive = isActive)
            Spacer(Modifier.height(10.dp))

            // Activo -> lápiz | Inactivo -> botón "Renovar" (mismo tamaño)
            if (isActive) {
                IconSquareButton(
                    icon = Icons.Filled.Edit,
                    color = GymColors.Orange,
                    onClick = onEdit,
                    modifier = Modifier.size(ActionButtonWidth, ActionButtonHeight)
                )
            } else {
                OutlinedButton(
                    onClick = onRenovar,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GymColors.Orange),
                    border = BorderStroke(1.dp, GymColors.Orange),
                    modifier = Modifier.size(ActionButtonWidth, ActionButtonHeight),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Renovar", fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun IconSquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .border(1.dp, color, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = "Editar", tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    val color = if (isActive) GymColors.Green else GymColors.Red
    val label = if (isActive) "Activo" else "Inactivo"

    Box(
        modifier = Modifier
            .size(ActionButtonWidth, ActionButtonHeight)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}