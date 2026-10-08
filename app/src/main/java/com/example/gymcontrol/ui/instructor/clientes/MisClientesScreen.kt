package com.example.gymcontrol.ui.instructor.clientes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gymcontrol.R
import com.example.gymcontrol.data.remote.InstructorRepository
import com.example.gymcontrol.ui.components.SesionActual
import kotlinx.coroutines.launch
import com.example.gymcontrol.ui.components.ClienteScaffold
import com.example.gymcontrol.ui.components.ScaffoldTab
import com.example.gymcontrol.ui.theme.GymColors

private val CardBorder = Brush.linearGradient(listOf(GymColors.Purple, GymColors.Gold))

private val INSTRUCTOR_TABS = listOf(
    ScaffoldTab("Mis clientes", Icons.Filled.Group),
    ScaffoldTab("Perfil", Icons.Filled.Person)
)

// Días que se pueden elegir (de lunes a sábado)
private val DIAS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")

private data class ClienteAsignado(
    val id: Long,
    val nombre: String,
    val dias: String,
    val objetivo: String
)

@Composable
fun MisClientesScreen(onNavigate: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var clientes by remember { mutableStateOf<List<ClienteAsignado>>(emptyList()) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    val correo = SesionActual.correo ?: ""

    suspend fun recargar() {
        try {
            clientes = InstructorRepository.misClientes(correo)
                .map { ClienteAsignado(it.id, it.nombre, it.dias, it.objetivo) }
            mensaje = null
        } catch (e: Exception) {
            mensaje = "No se pudo cargar: ${e.message?.substringBefore("\nCode:")}"
        }
    }

    LaunchedEffect(Unit) { recargar() }

    // null = formulario cerrado, número = índice del cliente que se edita
    var indiceEditando by remember { mutableStateOf<Int?>(null) }

    ClienteScaffold(
        currentSection = "Mis clientes",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_axolotl,
        tabs = INSTRUCTOR_TABS
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp)
        ) {
            item {
                Text(
                    text = "Usuarios asignados",
                    color = GymColors.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            mensaje?.let { msg ->
                item { Text(msg, color = GymColors.Red, fontSize = 14.sp) }
            }

            itemsIndexed(clientes) { index, cliente ->
                ClienteCard(
                    cliente = cliente,
                    onEditar = { indiceEditando = index }
                )
            }

            if (clientes.isEmpty()) {
                item {
                    Text(
                        text = "No tienes clientes asignados",
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
    }

    indiceEditando?.let { indice ->
        val cliente = clientes[indice]

        EditarClienteDialog(
            diasInicial = cliente.dias,
            objetivoInicial = cliente.objetivo,
            onDismiss = { indiceEditando = null },
            onGuardar = { nuevosDias, nuevoObjetivo ->
                indiceEditando = null
                scope.launch {
                    try {
                        InstructorRepository.guardar(correo, cliente.id, nuevosDias, nuevoObjetivo)
                        mensaje = null
                    } catch (e: Exception) {
                        mensaje = e.message?.substringBefore("\nCode:") ?: "No se pudo guardar"
                    }
                    recargar()
                }
            }
        )
    }
}

@Composable
private fun ClienteCard(cliente: ClienteAsignado, onEditar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GymColors.Surface, RoundedCornerShape(14.dp))
            .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Círculo con iniciales (luego se puede reemplazar por la foto del cliente)
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(GymColors.Purple.copy(alpha = 0.18f), CircleShape)
                .border(2.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iniciales(cliente.nombre),
                color = GymColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cliente.nombre,
                color = GymColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = cliente.dias.ifBlank { "Sin días asignados" },
                color = GymColors.TextSecondary,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Objetivo: ${cliente.objetivo.ifBlank { "—" }}",
                color = GymColors.TextSecondary,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.width(10.dp))

        Button(
            onClick = onEditar,
            modifier = Modifier.size(width = 88.dp, height = 40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GymColors.Purple,
                contentColor = GymColors.TextPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Editar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun iniciales(nombre: String): String =
    nombre.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

// "Lun, Mié, Vie" -> {Lun, Mié, Vie} | "Lun a Vie" -> {Lun, Mar, Mié, Jue, Vie}
private fun parsearDias(texto: String): Set<String> {
    val limpio = texto.trim()
    if (" a " in limpio) {
        val partes = limpio.split(" a ", limit = 2).map { it.trim() }
        val desde = DIAS.indexOf(partes[0])
        val hasta = DIAS.indexOf(partes[1])
        if (desde >= 0 && hasta >= desde) return DIAS.subList(desde, hasta + 1).toSet()
    }
    return limpio.split(",").map { it.trim() }.filter { it in DIAS }.toSet()
}

// {Lun, Mar, Mié, Jue, Vie} -> "Lun a Vie" | {Lun, Mié, Vie} -> "Lun, Mié, Vie"
private fun formatearDias(seleccionados: Set<String>): String {
    val ordenados = DIAS.filter { it in seleccionados }
    val indices = ordenados.map { DIAS.indexOf(it) }
    val consecutivos = indices.zipWithNext().all { (a, b) -> b == a + 1 }
    return if (ordenados.size >= 3 && consecutivos) {
        "${ordenados.first()} a ${ordenados.last()}"
    } else {
        ordenados.joinToString(", ")
    }
}

@Composable
private fun EditarClienteDialog(
    diasInicial: String,
    objetivoInicial: String,
    onDismiss: () -> Unit,
    onGuardar: (dias: String, objetivo: String) -> Unit
) {
    var seleccionados by remember { mutableStateOf(parsearDias(diasInicial)) }
    var objetivo by remember { mutableStateOf(objetivoInicial) }
    val esValido = seleccionados.isNotEmpty() && objetivo.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GymColors.Surface, RoundedCornerShape(14.dp))
                .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 44.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Días que asistirá",
                    color = GymColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                SelectorDias(
                    seleccionados = seleccionados,
                    onToggle = { dia ->
                        seleccionados = if (dia in seleccionados) seleccionados - dia else seleccionados + dia
                    }
                )

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "Objetivo",
                    color = GymColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                CampoPill(valor = objetivo, onCambio = { objetivo = it })

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = { onGuardar(formatearDias(seleccionados), objetivo.trim()) },
                    enabled = esValido,
                    modifier = Modifier.size(width = 160.dp, height = 44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GymColors.Purple,
                        contentColor = GymColors.TextPrimary,
                        disabledContainerColor = GymColors.Purple.copy(alpha = 0.45f),
                        disabledContentColor = GymColors.TextPrimary.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Guardar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // X para cerrar el formulario
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = GymColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Botones Lun a Sáb en dos filas de tres; tocar uno lo marca o lo desmarca
@Composable
private fun SelectorDias(seleccionados: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DIAS.chunked(3).forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fila.forEach { dia ->
                    val activo = dia in seleccionados
                    val forma = RoundedCornerShape(50)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(forma)
                            .background(if (activo) GymColors.Purple else Color.Transparent)
                            .border(1.dp, GymColors.Purple, forma)
                            .clickable { onToggle(dia) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dia,
                            color = if (activo) GymColors.TextPrimary else GymColors.TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoPill(valor: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        singleLine = true,
        shape = RoundedCornerShape(50),
        textStyle = TextStyle(
            color = GymColors.TextPrimary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GymColors.Purple,
            unfocusedBorderColor = GymColors.Purple,
            focusedTextColor = GymColors.TextPrimary,
            unfocusedTextColor = GymColors.TextPrimary,
            cursorColor = GymColors.Gold
        ),
        modifier = Modifier.fillMaxWidth()
    )
}