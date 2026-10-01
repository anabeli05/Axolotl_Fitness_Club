package com.example.gymcontrol.ui.encargado.gastos

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")

@Composable
fun GastosScreen(onNavigate: (String) -> Unit = {}) {
    var showForm by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var concept by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    val allExpenses = GymApp.repository.expenses()

    fun closeForm() {
        showForm = false
        editingIndex = null
        concept = ""
        category = ""
        amount = ""
        date = ""
    }

    GymScaffold(
        currentSection = "Gastos",
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
                            concept = ""
                            category = ""
                            amount = ""
                            date = ""
                            showForm = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nuevo Gasto", fontWeight = FontWeight.Bold)
                    }
                }

                itemsIndexed(allExpenses) { index, expense ->
                    ExpenseCard(
                        concept = expense.concept,
                        category = expense.category,
                        amount = "$${expense.amount}",
                        date = expense.date,
                        onEdit = {
                            editingIndex = index
                            concept = expense.concept
                            category = expense.category
                            amount = "${expense.amount}"
                            date = expense.date
                            showForm = true
                        },
                        onDelete = { /* TODO: eliminar gasto */ }
                    )
                }
            }

            if (showForm) {
                ExpenseFormDialog(
                    isEditing = editingIndex != null,
                    concept = concept,
                    category = category,
                    amount = amount,
                    date = date,
                    onConceptChange = { concept = it },
                    onCategoryChange = { category = it },
                    onAmountChange = { amount = it },
                    onDateChange = { date = it },
                    onDismiss = { closeForm() },
                    onSave = {
                        // TODO: si editingIndex != null, actualizar ese gasto en GymApp.repository
                        // TODO: si editingIndex == null, crear uno nuevo en GymApp.repository
                        closeForm()
                    }
                )
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    concept: String,
    category: String,
    amount: String,
    date: String,
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
                text = concept,
                color = GymColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Categoría: $category",
                color = GymColors.TextSecondary,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Monto: $amount",
                color = GymColors.Gold,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Fecha: $date",
                color = GymColors.TextSecondary,
                fontSize = 14.sp
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
private fun ExpenseFormDialog(
    isEditing: Boolean,
    concept: String,
    category: String,
    amount: String,
    date: String,
    onConceptChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
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
                    text = if (isEditing) "Editar gasto" else "Nuevo gasto",
                    color = GymColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = GymColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            GymTextField(value = concept, onValueChange = onConceptChange, label = "Concepto")
            Spacer(Modifier.height(10.dp))
            GymTextField(value = category, onValueChange = onCategoryChange, label = "Categoría")
            Spacer(Modifier.height(10.dp))
            GymTextField(value = amount, onValueChange = onAmountChange, label = "Monto")
            Spacer(Modifier.height(10.dp))
            GymTextField(value = date, onValueChange = onDateChange, label = "Fecha")

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Guardar" else "Crear Gasto", fontWeight = FontWeight.Bold)
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