package com.example.gymcontrol.ui.encargado.usuarios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.data.model.Status
import com.example.gymcontrol.data.model.UserRole
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Gastos", "Reportes")

// Colores tomados del mockup. Si ya los tienes en GymColors, cámbialos por esos.
private val Purple = Color(0xFFA54FD9)
private val CardBackground = Color(0xFF1E1F20)
private val CardBorder = Color(0xFF4A4B4D)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB5B5B5)

// Datos mínimos de la persona seleccionada para editar o eliminar
private class StaffItem(
    val name: String,
    val email: String,
    val role: UserRole,
    val status: Status
)

@Composable
fun UsuariosScreen(
    onNewUser: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    var editing by remember { mutableStateOf<StaffItem?>(null) }
    var deleting by remember { mutableStateOf<StaffItem?>(null) }
    // Solo para la demo: oculta de la lista a quien se "elimina". TODO: quitar cuando haya validación real
    val removed = remember { mutableStateListOf<String>() }

    val personal = GymApp.repository.users()
        .filter { it.membershipNumber == null && "${it.email}" !in removed }

    // Formulario de edición (reemplaza la lista mientras está abierto)
    editing?.let { target ->
        EditarUsuarioScreen(
            name = target.name,
            email = target.email,
            role = target.role,
            status = target.status,
            onBack = { editing = null },
            onSave = { _, _, _, _ -> editing = null }
        )
        return
    }

    GymScaffold(
        currentSection = "Personal",
        sections = ADMIN_SECTIONS,
        onNavigate = onNavigate
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Personal",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Administra al personal del gimnasio",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            item {
                Button(
                    onClick = onNewUser,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar personal", fontWeight = FontWeight.Bold)
                }
            }

            items(personal) { user ->
                val statusColor = if (user.status == Status.ACTIVO) GymColors.Green else GymColors.Red

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar (alineado arriba, junto al nombre)
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(1.dp, Purple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // Info compacta
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            // Nombre + acciones en la misma fila
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = user.name,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        editing = StaffItem(user.name, "${user.email}", user.role, user.status)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = Purple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        deleting = StaffItem(user.name, "${user.email}", user.role, user.status)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = GymColors.Red,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${user.role}",
                                color = Purple,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${user.email}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(6.dp))

                            // Chip de estado
                            Box(
                                modifier = Modifier
                                    .border(1.dp, statusColor, RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${user.status}",
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Cuadro de confirmación para eliminar
    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = CardBackground,
            shape = RoundedCornerShape(16.dp),
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = GymColors.Red)
            },
            title = {
                Text("¿Eliminar personal?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Se eliminará a ${target.name}. Esta acción no se puede deshacer.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        removed.add(target.email)
                        deleting = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymColors.Red,
                        contentColor = Color.White
                    )
                ) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deleting = null },
                    border = BorderStroke(1.dp, CardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) { Text("Cancelar") }
            }
        )
    }
}