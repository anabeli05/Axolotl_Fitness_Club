package com.example.gymcontrol.ui.cliente.Pag

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.R
import com.example.gymcontrol.ui.components.ClienteScaffoldClaro
import com.example.gymcontrol.ui.components.ClienteTema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO: tomar nombre y membresía del cliente en sesión (igual que en Inicio/Perfil)
private const val NOMBRE_USUARIO = "Ana López"
private const val NUMERO_MEMBRESIA = "M-1001"

/** Planes disponibles. Precios temporales: cámbialos aquí cuando tengas los definitivos. */
private data class PlanInfo(val nombre: String, val precio: Int, val duracion: String)

private val PLANES = listOf(
    PlanInfo(nombre = "Semana", precio = 100, duracion = "7 días de acceso"),
    PlanInfo(nombre = "Mes", precio = 400, duracion = "30 días de acceso"),
    PlanInfo(nombre = "Trimestre", precio = 1200, duracion = "90 días de acceso")
)

@Composable
fun PagHomeScreen(onNavigate: (String) -> Unit = {}) {
    var selectedPlan by remember { mutableStateOf<PlanInfo?>(null) }
    var selectedMethod by remember { mutableStateOf<PaymentMethodInfo?>(null) }

    // Con el botón "atrás" se regresa a la selección de plan
    BackHandler(enabled = selectedPlan != null) { selectedPlan = null }

    ClienteScaffoldClaro(
        currentSection = "Pagos",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_final
    ) {
        val plan = selectedPlan
        if (plan == null) {
            SeleccionPlanContent(onPlanSelected = { selectedPlan = it })
        } else {
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
                    color = ClienteTema.Texto
                )
            }

            item {
                PlanResumenCard(plan = plan, onCambiar = { selectedPlan = null })
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
                                "Beneficiario:" to "Gimnasio Axolotl S.A. de C.V."
                            ),
                            instructions = "Ingresa la CLABE en tu banca móvil, coloca el concepto indicado abajo y después sube tu comprobante."
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
                            instructions = "Presenta el número de tarjeta en cualquier sucursal bancaria o tienda de conveniencia, realiza tu depósito y sube la foto de tu comprobante."
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
                            instructions = "Acude a la recepción del gimnasio, realiza tu pago y sube la foto de tu comprobante o ticket."
                        )
                    }
                )
            }
        }
        }
    }

    // Modal de detalles del método de pago + comprobante
    val planActual = selectedPlan
    selectedMethod?.let { info ->
        if (planActual != null) {
            PaymentDetailDialog(
                info = info,
                plan = planActual,
                nombre = NOMBRE_USUARIO,
                membresia = NUMERO_MEMBRESIA,
                onDismiss = { selectedMethod = null },
                onEnviar = { /* TODO: subir imagen (uri) al servidor/Supabase */ }
            )
        }
    }
}

@Composable
private fun SeleccionPlanContent(onPlanSelected: (PlanInfo) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Selecciona tu plan",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = ClienteTema.Texto
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Elige el plan que vas a pagar para continuar con el método de pago.",
                fontSize = 13.sp,
                color = ClienteTema.TextoSec
            )
        }
        items(PLANES) { plan ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlanSelected(plan) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
                border = BorderStroke(1.5.dp, ClienteTema.Borde)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = plan.nombre,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClienteTema.Texto
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(plan.duracion, fontSize = 13.sp, color = ClienteTema.TextoSec)
                    }
                    Text(
                        text = "$${plan.precio}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClienteTema.AzulSel
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanResumenCard(plan: PlanInfo, onCambiar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
        border = BorderStroke(1.5.dp, ClienteTema.AzulSel)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Plan seleccionado", fontSize = 12.sp, color = ClienteTema.TextoSec)
                Text(
                    text = "${plan.nombre} - $${plan.precio}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClienteTema.Texto
                )
            }
            TextButton(onClick = onCambiar) {
                Text("Cambiar", color = ClienteTema.AzulSel, fontWeight = FontWeight.Bold)
            }
        }
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
    plan: PlanInfo,
    nombre: String,
    membresia: String,
    onDismiss: () -> Unit,
    onEnviar: (Uri) -> Unit
) {
    val context = LocalContext.current
    var comprobanteUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) comprobanteUri = uri
    }

    val concepto = "Pago plan ${plan.nombre} ($${plan.precio}) - $nombre - Membresía $membresia"

    // Miniatura del comprobante (se decodifica fuera del hilo principal)
    val miniatura by produceState<android.graphics.Bitmap?>(initialValue = null, comprobanteUri) {
        val uri = comprobanteUri
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
            }.getOrNull()
        }
    }
    val nombreArchivo = remember(comprobanteUri) {
        comprobanteUri?.let { uri ->
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0 && c.moveToFirst()) c.getString(i) else null
                }
            }.getOrNull()
        } ?: "comprobante"
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .background(ClienteTema.Beige, RoundedCornerShape(20.dp))
                .border(1.5.dp, ClienteTema.Borde, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
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
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = value,
                        fontSize = 13.sp,
                        color = ClienteTema.Texto,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Monto y concepto del pago
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClienteTema.Fondo, RoundedCornerShape(10.dp))
                    .border(1.dp, ClienteTema.AzulSel.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text("Monto a pagar", fontSize = 12.sp, color = ClienteTema.TextoSec)
                Text(
                    text = "$${plan.precio} MXN (${plan.nombre})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClienteTema.AzulSel
                )
                Spacer(Modifier.height(8.dp))
                Text("Concepto", fontSize = 12.sp, color = ClienteTema.TextoSec)
                Text(
                    text = concepto,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClienteTema.Texto
                )
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

            Spacer(modifier = Modifier.height(16.dp))

            // Vista previa del comprobante seleccionado
            if (comprobanteUri != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = ClienteTema.Verde,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = nombreArchivo,
                        fontSize = 12.sp,
                        color = ClienteTema.TextoSec,
                        maxLines = 1
                    )
                }
                miniatura?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Comprobante de pago",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .border(1.dp, ClienteTema.Borde.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            // Botón para subir el comprobante (reemplaza al antiguo "Aceptar")
            Button(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClienteTema.Azul,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (comprobanteUri == null) "Subir comprobante" else "Cambiar comprobante",
                    fontWeight = FontWeight.Bold
                )
            }

            if (comprobanteUri != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        comprobanteUri?.let(onEnviar)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, ClienteTema.AzulSel)
                ) {
                    Text("Enviar comprobante", color = ClienteTema.AzulSel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
