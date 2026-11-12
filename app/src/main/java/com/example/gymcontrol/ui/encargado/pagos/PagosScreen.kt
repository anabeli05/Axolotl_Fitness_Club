package com.example.gymcontrol.ui.encargado.pagos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.ui.components.ClienteTema
import com.example.gymcontrol.ui.components.GymScaffold

private val ADMIN_SECTIONS = listOf("Dashboard", "Clientes", "Personal", "Servicios", "Pagos", "Gastos", "Reportes")

/** Forma en que el cliente realizó el pago. */
enum class MetodoPago(val etiqueta: String) {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia"),
    DEPOSITO("Depósito")
}

/**
 * Un pago de cliente.
 * [membresia] es el número de membresía del cliente.
 * [plan] es lo que está pagando (Semana, Mes o Trimestre).
 * [imagen] es el comprobante que subió; si es null se muestra un recuadro vacío.
 */
data class PagoItem(
    val id: String,
    val cliente: String,
    val membresia: String,
    val plan: String,
    val metodoPago: MetodoPago,
    val imagen: ImageBitmap? = null
)

@Composable
fun PagosScreen(onNavigate: (String) -> Unit = {}) {
    // TODO: reemplazar por los comprobantes reales cuando se conecte la base de datos
    val pagos = remember {
        listOf(
            PagoItem("1", "Ana López", "M-1001", "Mes", MetodoPago.TRANSFERENCIA),
            PagoItem("2", "Carlos Gómez", "M-1002", "Semana", MetodoPago.EFECTIVO),
            PagoItem("3", "María Rodríguez", "M-1003", "Trimestre", MetodoPago.DEPOSITO)
        )
    }
    var imagenAmpliada by remember { mutableStateOf<ImageBitmap?>(null) }

    GymScaffold(
        currentSection = "Pagos",
        sections = ADMIN_SECTIONS,
        onNavigate = onNavigate
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Pagos de Clientes",
                    color = ClienteTema.Texto,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(pagos, key = { it.id }) { pago ->
                PagoCard(pago = pago, onAmpliar = { imagenAmpliada = it })
            }
        }
    }

    // Comprobante a pantalla completa
    imagenAmpliada?.let { img ->
        Dialog(onDismissRequest = { imagenAmpliada = null }) {
            Image(
                bitmap = img,
                contentDescription = "Comprobante de pago",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .clickable { imagenAmpliada = null }
            )
        }
    }
}

/** Recuadro con: nombre, número de membresía, plan, tipo de pago y la imagen que subió. */
@Composable
private fun PagoCard(pago: PagoItem, onAmpliar: (ImageBitmap) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteTema.Beige),
        border = BorderStroke(1.5.dp, ClienteTema.Borde)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = pago.cliente,
                color = ClienteTema.Texto,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Membresía: ${pago.membresia}",
                color = ClienteTema.AzulSel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Plan: ${pago.plan}",
                color = ClienteTema.Texto,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Tipo de pago: ${pago.metodoPago.etiqueta}",
                color = ClienteTema.TextoSec,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 260.dp)
                    .background(ClienteTema.Fondo, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val img = pago.imagen
                if (img != null) {
                    Image(
                        bitmap = img,
                        contentDescription = "Comprobante de ${pago.cliente}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onAmpliar(img) }
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = ClienteTema.TextoSec,
                            modifier = Modifier.size(40.dp)
                        )
                        Text("Comprobante", color = ClienteTema.TextoSec, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}