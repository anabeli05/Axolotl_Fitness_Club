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
import androidx.compose.ui.res.stringResource
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
import com.example.gymcontrol.ui.components.ClienteTema

private val CardBorder = Brush.linearGradient(listOf(ClienteTema.Borde, ClienteTema.AzulSel))

private val INSTRUCTOR_TABS = listOf(
    ScaffoldTab("Mis clientes", Icons.Filled.Group),
    ScaffoldTab("Perfil", Icons.Filled.Person)
)

private data class ClienteAsignado(
    val id: Long,
    val nombre: String,
    val dias: String,
    val objetivo: String
)

@Composable
private fun getDias(): List<String> = listOf(
    stringResource(R.string.day_mon),
    stringResource(R.string.day_tue),
    stringResource(R.string.day_wed),
    stringResource(R.string.day_thu),
    stringResource(R.string.day_fri),
    stringResource(R.string.day_sat)
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

    var indiceEditando by remember { mutableStateOf<Int?>(null) }
    val diasList = getDias()

    ClienteScaffold(
        currentSection = "Mis clientes",
        onNavigate = onNavigate,
        logoRes = R.drawable.logo_final,
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
                    text = stringResource(R.string.instructor_assigned_users_title),
                    color = ClienteTema.Texto,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            mensaje?.let { msg ->
                item { Text(msg, color = ClienteTema.Rojo, fontSize = 14.sp) }
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
                        text = stringResource(R.string.instructor_no_assigned_clients),
                        color = ClienteTema.TextoSec,
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
            diasList = diasList,
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
            .background(ClienteTema.Beige, RoundedCornerShape(14.dp))
            .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(ClienteTema.AzulSel.copy(alpha = 0.18f), CircleShape)
                .border(2.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iniciales(cliente.nombre),
                color = ClienteTema.Texto,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cliente.nombre,
                color = ClienteTema.Texto,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = cliente.dias.ifBlank { stringResource(R.string.instructor_client_no_days) },
                color = ClienteTema.TextoSec,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.instructor_client_objective, cliente.objetivo.ifBlank { "—" }),
                color = ClienteTema.TextoSec,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.width(10.dp))

        Button(
            onClick = onEditar,
            modifier = Modifier.size(width = 88.dp, height = 40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ClienteTema.Azul,
                contentColor = ClienteTema.Texto
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(stringResource(R.string.common_edit), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun iniciales(nombre: String): String =
    nombre.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

private fun parsearDias(texto: String, dias: List<String>): Set<String> {
    val limpio = texto.trim()
    if (" a " in limpio) {
        val partes = limpio.split(" a ", limit = 2).map { it.trim() }
        val desde = dias.indexOf(partes[0])
        val hasta = dias.indexOf(partes[1])
        if (desde >= 0 && hasta >= desde) return dias.subList(desde, hasta + 1).toSet()
    }
    return limpio.split(",").map { it.trim() }.filter { it in dias }.toSet()
}

private fun formatearDias(seleccionados: Set<String>, dias: List<String>): String {
    val ordenados = dias.filter { it in seleccionados }
    val indices = ordenados.map { dias.indexOf(it) }
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
    diasList: List<String>,
    onDismiss: () -> Unit,
    onGuardar: (dias: String, objetivo: String) -> Unit
) {
    var seleccionados by remember { mutableStateOf(parsearDias(diasInicial, diasList)) }
    var objetivo by remember { mutableStateOf(objetivoInicial) }
    val esValido = seleccionados.isNotEmpty() && objetivo.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ClienteTema.Beige, RoundedCornerShape(14.dp))
                .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 44.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.instructor_dialog_days_title),
                    color = ClienteTema.Texto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                SelectorDias(
                    diasList = diasList,
                    seleccionados = seleccionados,
                    onToggle = { dia ->
                        seleccionados = if (dia in seleccionados) seleccionados - dia else seleccionados + dia
                    }
                )

                Spacer(Modifier.height(22.dp))

                Text(
                    text = stringResource(R.string.instructor_dialog_objective_title),
                    color = ClienteTema.Texto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                CampoPill(valor = objetivo, onCambio = { objetivo = it })

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = { onGuardar(formatearDias(seleccionados, diasList), objetivo.trim()) },
                    enabled = esValido,
                    modifier = Modifier.size(width = 160.dp, height = 44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClienteTema.Azul,
                        contentColor = ClienteTema.Texto,
                        disabledContainerColor = ClienteTema.Azul.copy(alpha = 0.45f),
                        disabledContentColor = ClienteTema.Texto.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = ClienteTema.TextoSec,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectorDias(diasList: List<String>, seleccionados: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        diasList.chunked(3).forEach { fila ->
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
                            .background(if (activo) ClienteTema.AzulSel else Color.Transparent)
                            .border(1.dp, ClienteTema.AzulSel, forma)
                            .clickable { onToggle(dia) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dia,
                            color = if (activo) ClienteTema.Texto else ClienteTema.TextoSec,
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
            color = ClienteTema.Texto,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClienteTema.AzulSel,
            unfocusedBorderColor = ClienteTema.AzulSel,
            focusedTextColor = ClienteTema.Texto,
            unfocusedTextColor = ClienteTema.Texto,
            cursorColor = ClienteTema.AzulSel
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
