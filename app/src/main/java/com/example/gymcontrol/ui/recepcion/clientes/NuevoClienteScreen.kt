package com.example.gymcontrol.ui.recepcion.clientes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.theme.GymColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormDialog(
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf<String?>(null) }
    var planMenuExpanded by remember { mutableStateOf(false) }

    val plans = GymApp.repository.services().map { it.name }
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
                    text = if (isEditing) "Editar cliente" else "Nuevo cliente",
                    color = GymColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = GymColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            GymTextField(value = name, onValueChange = { name = it }, label = "Nombre")
            Spacer(Modifier.height(10.dp))
            GymTextField(value = lastName, onValueChange = { lastName = it }, label = "Apellido")
            Spacer(Modifier.height(10.dp))
            GymTextField(value = phone, onValueChange = { phone = it }, label = "Teléfono")
            Spacer(Modifier.height(10.dp))
            GymTextField(value = email, onValueChange = { email = it }, label = "Correo")
            Spacer(Modifier.height(10.dp))

            ExposedDropdownMenuBox(
                expanded = planMenuExpanded,
                onExpandedChange = { planMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedPlan ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Selecciona un plan", color = GymColors.TextSecondary) },
                    label = { Text("Plan", color = GymColors.TextSecondary) },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = GymColors.TextSecondary)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GymColors.Purple,
                        unfocusedBorderColor = GymColors.Border,
                        focusedTextColor = GymColors.TextPrimary,
                        unfocusedTextColor = GymColors.TextPrimary,
                        cursorColor = GymColors.Gold
                    )
                )
                ExposedDropdownMenu(
                    expanded = planMenuExpanded,
                    onDismissRequest = { planMenuExpanded = false },
                    modifier = Modifier.background(GymColors.Surface)
                ) {
                    plans.forEach { plan ->
                        DropdownMenuItem(
                            text = { Text(plan, color = GymColors.TextPrimary) },
                            onClick = {
                                selectedPlan = plan
                                planMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = GymColors.Purple),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Guardar" else "Crear Cliente", fontWeight = FontWeight.Bold)
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