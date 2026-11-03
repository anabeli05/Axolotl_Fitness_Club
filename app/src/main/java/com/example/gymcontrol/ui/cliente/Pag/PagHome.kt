package com.example.gymcontrol.ui.cliente.Pag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.R
import com.example.gymcontrol.ui.components.ClienteScaffoldClaro
import com.example.gymcontrol.ui.components.ClienteTema

@Composable
fun PagHomeScreen(onNavigate: (String) -> Unit = {}) {
    var selectedMethod by remember { mutableStateOf<PaymentMethodInfo?>(null) }

    ClienteScaffoldClaro(
        currentSection = "Pagos",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_final
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Métodos de Pago",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClienteTema.Texto,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 1. Pago por Transferencia
            item {
                MetodoPagoCard(
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    title = "Pago por\nTransferencia",
                    description = null,
                    subtitle = "Transferencia bancaria directa",
                    onClick = {
                        selectedMethod = PaymentMethodInfo(
                            type = PaymentType.TRANSFERENCIA,
                            title = "Pago por Transferencia",
                            details = listOf(
                                "Banco:" to "Banco",
                                "CLABE:" to "012 095 01569158521 0",
                                "Cuenta:" to "1234567890",
                                "Beneficiario:" to "Gimnasio Axolotl S.A. de C.V.",
                                "Concepto:" to "Tu número de Membresía"
                            ),
                            instructions = "Ingresa la CLABE en tu banca móvil y coloca tu número de membresía o nombre completo en el concepto de pago."
                        )
                    }
                )
            }

            // 2. Pago por Depósito
            item {
                MetodoPagoCard(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Pago por Depósito",
                    description = "Depósito en sucursal bancaria o corresponsal",
                    subtitle = "Depósito en sucursal bancaria o corresponsal",
                    onClick = {
                        selectedMethod = PaymentMethodInfo(
                            type = PaymentType.DEPOSITO,
                            title = "Pago por Depósito",
                            details = listOf(
                                "No. Tarjeta:" to "4152 3138 9012 3456",
                                "Corresponsales:" to "OXXO, 7-Eleven, Farmacias del Ahorro",
                                "Sucursal:" to "BBVA Bancomer",
                                "Beneficiario:" to "Gimnasio Axolotl"
                            ),
                            instructions = "Presenta el número de tarjeta en cualquier sucursal bancaria o tienda de conveniencia para realizar tu depósito."
                        )
                    }
                )
            }

            // 3. Pago en Efectivo
            item {
                MetodoPagoCard(
                    icon = Icons.Filled.Payments,
                    title = "Pago en Efectivo",
                    description = "Pago físico en recepción\n(Gimnasio Axolotl)",
                    subtitle = "Pago físico en recepción (Gimnasio Axolotl)",
                    onClick = {
                        selectedMethod = PaymentMethodInfo(
                            type = PaymentType.EFECTIVO,
                            title = "Pago en Efectivo",
                            details = listOf(
                                "Ubicación:" to "Recepción - Gimnasio Axolotl",
                                "Horario:" to "Lunes a Domingo (06:00 - 00:00 hrs)",
                                "Atención:" to "Caja principal"
                            ),
                            instructions = "Acude directamente a la recepción del gimnasio y proporciona tu nombre o número de membresía al encargado."
                        )
                    }
                )
            }
        }
    }

    // Modal de detalles del método de pago
    selectedMethod?.let { info ->
        PaymentDetailDialog(
            info = info,
            onDismiss = { selectedMethod = null }
        )
    }
}

@Composable
private fun MetodoPagoCard(
    icon: ImageVector,
    title: String,
    description: String?,
    subtitle: String,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = forma,
        colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
        border = BorderStroke(1.5.dp, ClienteTema.Borde)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ClienteTema.AzulSel,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClienteTema.Texto,
                        lineHeight = 24.sp
                    )
                    if (description != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            fontSize = 13.sp,
                            color = ClienteTema.TextoSec,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = ClienteTema.Borde.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = ClienteTema.TextoSec
            )
        }
    }
}

private enum class PaymentType { TRANSFERENCIA, DEPOSITO, EFECTIVO }

private data class PaymentMethodInfo(
    val type: PaymentType,
    val title: String,
    val details: List<Pair<String, String>>,
    val instructions: String
)

@Composable
private fun PaymentDetailDialog(
    info: PaymentMethodInfo,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Beige, RoundedCornerShape(20.dp))
                .border(1.5.dp, ClienteTema.Borde, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = info.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClienteTema.Texto
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar",
                        tint = ClienteTema.TextoSec
                    )
                }
            }

            HorizontalDivider(
                color = ClienteTema.Borde.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            info.details.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = ClienteTema.TextoSec,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = value,
                        fontSize = 13.sp,
                        color = ClienteTema.Texto,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        ClienteTema.Navy.copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(1.dp, ClienteTema.Borde.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = info.instructions,
                    fontSize = 12.sp,
                    color = ClienteTema.TextoSec,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClienteTema.Azul,
                    contentColor = Color.White
                )
            ) {
                Text("Aceptar", fontWeight = FontWeight.Bold)
            }
        }
    }
}
