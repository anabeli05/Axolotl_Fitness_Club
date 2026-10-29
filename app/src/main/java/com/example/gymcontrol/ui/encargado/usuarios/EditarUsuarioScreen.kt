package com.example.gymcontrol.ui.encargado.usuarios

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.data.model.Status
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.components.ClienteTema
import androidx.compose.foundation.background

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarUsuarioScreen(
    name: String,
    email: String,
    role: UserRole,
    status: Status,
    onBack: () -> Unit,
    onSave: (String, String, UserRole, Status) -> Unit = { _, _, _, _ -> }
) {
    var editedName by remember { mutableStateOf(name) }
    var editedEmail by remember { mutableStateOf(email) }
    var editedRole by remember { mutableStateOf(role) }
    var editedStatus by remember { mutableStateOf(status) }

    var roleMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }

    GymScaffold(
        currentSection = "Personal",
        sections = ADMIN_SECTIONS,
        onNavigate = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Editar personal",
                color = ClienteTema.Texto,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            GymTextField(value = editedName, onValueChange = { editedName = it }, label = "Nombre")
            GymTextField(value = editedEmail, onValueChange = { editedEmail = it }, label = "Correo")

            ExposedDropdownMenuBox(
                expanded = roleMenuExpanded,
                onExpandedChange = { roleMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = editedRole.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rol", color = ClienteTema.TextoSec) },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = ClienteTema.TextoSec)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClienteTema.AzulSel,
                        unfocusedBorderColor = ClienteTema.Borde,
                        focusedTextColor = ClienteTema.Texto,
                        unfocusedTextColor = ClienteTema.Texto,
                        cursorColor = ClienteTema.AzulSel
                    )
                )
                ExposedDropdownMenu(
                    expanded = roleMenuExpanded,
                    onDismissRequest = { roleMenuExpanded = false },
                    modifier = Modifier.background(ClienteTema.Beige)
                ) {
                    UserRole.values().forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r.name, color = ClienteTema.Texto) },
                            onClick = {
                                editedRole = r
                                roleMenuExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = statusMenuExpanded,
                onExpandedChange = { statusMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = editedStatus.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Estado", color = ClienteTema.TextoSec) },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = ClienteTema.TextoSec)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClienteTema.AzulSel,
                        unfocusedBorderColor = ClienteTema.Borde,
                        focusedTextColor = ClienteTema.Texto,
                        unfocusedTextColor = ClienteTema.Texto,
                        cursorColor = ClienteTema.AzulSel
                    )
                )
                ExposedDropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = { statusMenuExpanded = false },
                    modifier = Modifier.background(ClienteTema.Beige)
                ) {
                    Status.values().forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.name, color = ClienteTema.Texto) },
                            onClick = {
                                editedStatus = s
                                statusMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClienteTema.Texto),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ClienteTema.Borde)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        onSave(editedName, editedEmail, editedRole, editedStatus)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClienteTema.Azul),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
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
        label = { Text(label, color = ClienteTema.TextoSec) },
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClienteTema.AzulSel,
            unfocusedBorderColor = ClienteTema.Borde,
            focusedTextColor = ClienteTema.Texto,
            unfocusedTextColor = ClienteTema.Texto,
            cursorColor = ClienteTema.AzulSel
        )
    )
}