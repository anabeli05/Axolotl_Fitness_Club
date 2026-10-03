package com.example.gymcontrol.ui.cliente.asesorias

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.R
import com.example.gymcontrol.ui.components.ClienteScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val CardBorder = Brush.linearGradient(listOf(GymColors.Purple, GymColors.Gold))

private val ButtonHeight = 44.dp

private fun formatPrice(cost: Double): String =
    if (cost % 1.0 == 0.0) "$${"%.0f".format(cost)}" else "$${"%.2f".format(cost)}"

// "Marco Díaz" -> "MD"
private fun initials(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

@Composable
fun AsesoriasScreen(onNavigate: (String) -> Unit = {}) {
    val instructors = GymApp.repository.instructors()
    val pending = remember { mutableStateMapOf<Int, Boolean>() }

    ClienteScaffold(
        currentSection = "Asesorías",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_axolotl
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
                        text = "Asesorías",
                        color = GymColors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Elige tu instructor",
                        color = GymColors.TextSecondary,
                        fontSize = 16.sp
                    )
                }
            }

            items(instructors) { instructor ->
                val isPending = pending[instructor.id] == true

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GymColors.Surface, RoundedCornerShape(14.dp))
                        .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(20.dp)
                ) {
                    // Avatar + datos del instructor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InstructorAvatar(name = instructor.name)

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = instructor.name,
                                color = GymColors.TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Cupo: ${instructor.activeClients}/${instructor.maxClients}",
                                color = GymColors.Purple,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${formatPrice(instructor.cost)} /mes",
                                color = GymColors.Gold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Botones
                    if (isPending) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Estado pendiente
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(ButtonHeight)
                                    .background(GymColors.Gold.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .border(1.dp, GymColors.Gold, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Pendiente",
                                    color = GymColors.Gold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Cancelar la solicitud: vuelve a mostrar "Solicitar"
                            OutlinedButton(
                                onClick = { pending[instructor.id] = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(ButtonHeight),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, GymColors.Red),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = GymColors.Red.copy(alpha = 0.1f),
                                    contentColor = GymColors.Red
                                )
                            ) {
                                Text("Cancelar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = { pending[instructor.id] = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ButtonHeight),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GymColors.Purple,
                                contentColor = GymColors.TextPrimary
                            )
                        ) {
                            Text("Solicitar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Círculo con las iniciales del instructor (más adelante se puede reemplazar por su foto)
@Composable
private fun InstructorAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .background(GymColors.Purple.copy(alpha = 0.18f), CircleShape)
            .border(2.dp, CardBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials(name),
            color = GymColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}