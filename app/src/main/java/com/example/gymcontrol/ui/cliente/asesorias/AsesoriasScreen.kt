package com.example.gymcontrol.ui.cliente.asesorias

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import com.example.gymcontrol.ui.components.GymScaffold
import com.example.gymcontrol.ui.theme.GymColors

private val CLIENT_SECTIONS = listOf("Inicio", "Asesorías", "Perfil")

private val Purple = Color(0xFFA54FD9)
private val CardBackground = Color(0xFF1E1F20)
private val CardBorder = Color(0xFF4A4B4D)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB5B5B5)

private fun formatPrice(cost: Double): String =
    if (cost % 1.0 == 0.0) "$${"%.0f".format(cost)}" else "$${"%.2f".format(cost)}"

@Composable
fun AsesoriasScreen(onNavigate: (String) -> Unit = {}) {
    val instructors = GymApp.repository.instructors()
    val pending = remember { mutableStateMapOf<Int, Boolean>() }

    GymScaffold(
        currentSection = "Asesorías",
        sections = CLIENT_SECTIONS,
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
                        text = "Asesorías",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Elige tu instructor",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            items(instructors) { instructor ->
                val isPending = pending[instructor.id] == true

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        //Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(1.dp, CardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        //Datos del instructor
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = instructor.name,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Cupo: ${instructor.activeClients}/${instructor.maxClients}",
                                color = Purple,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = "${formatPrice(instructor.cost)} /mes",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        if (isPending) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                border = BorderStroke(1.dp, GymColors.Gold),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    disabledContentColor = TextPrimary
                                )
                            ) {
                                Text("Pendiente", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Button(
                                onClick = { pending[instructor.id] = true },
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Purple,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Solicitar", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}