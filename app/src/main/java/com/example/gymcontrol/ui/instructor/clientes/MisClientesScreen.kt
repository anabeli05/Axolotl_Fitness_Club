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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// =========================
// COLORES
// =========================

private val McNegro = Color(0xFF000000)
private val McGrisOscuro = Color(0xFF202121)
private val McGrisBorde = Color(0xFF494A4A)
private val McMorado = Color(0xFFA655E5)
private val McDorado = Color(0xFFA68A00)
private val McBlanco = Color(0xFFFFFFFF)
private val McGrisTexto = Color(0xFFBDBDBD)

// =========================
// MODELO
// =========================

private data class McCliente(
    val nombre: String,
    val dias: String,
    val objetivo: String
)

// =========================
// PANTALLA
// (el nombre MisClientesScreen es el que ya usa GymNavGraph)
// =========================

@Composable
fun MisClientesScreen(
    onIrAPerfil: () -> Unit = {}   // opcional: para navegar a Perfil desde la barra inferior
) {

    val clientes = remember {
        mutableStateListOf(
            McCliente("Ana López", "Lun, Mié, Vie", "Ganar fuerza"),
            McCliente("Luis Gómez", "Mar, Jue, Sáb", "Bajar grasa"),
            McCliente("Mariana Silva", "Lun a Vie", "Acondicionamiento")
        )
    }

    // null = modal cerrado, número = índice del cliente que se edita
    var indiceEditando by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(McNegro)
    ) {

        // ENCABEZADO
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
        ) {
            Text(
                text = "AXOLOTL",
                color = McDorado,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = McDorado, thickness = 1.dp)
        }

        // LISTA
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = "Usuarios asignados",
                color = McBlanco,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(clientes) { index, cliente ->
                    McClienteCard(
                        cliente = cliente,
                        onEditar = { indiceEditando = index }
                    )
                }
            }
        }

        // BARRA INFERIOR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .border(width = 1.dp, color = McGrisBorde)
                .background(McNegro),
            verticalAlignment = Alignment.CenterVertically
        ) {
            McBarraItem(
                modifier = Modifier.weight(1f),
                icono = Icons.Filled.Person,
                texto = "Mis clientes",
                color = McMorado,
                onClick = {}
            )
            McBarraItem(
                modifier = Modifier.weight(1f),
                icono = Icons.Outlined.Person,
                texto = "Perfil",
                color = McGrisTexto,
                onClick = onIrAPerfil
            )
        }
    }

    // MODAL DE EDICIÓN
    indiceEditando?.let { indice ->
        val cliente = clientes[indice]

        McEditarDialog(
            diasInicial = cliente.dias,
            objetivoInicial = cliente.objetivo,
            onDismiss = { indiceEditando = null },
            onGuardar = { nuevosDias, nuevoObjetivo ->
                clientes[indice] = cliente.copy(
                    dias = nuevosDias,
                    objetivo = nuevoObjetivo
                )
                indiceEditando = null
            }
        )
    }
}

// =========================
// TARJETA
// =========================

@Composable
private fun McClienteCard(cliente: McCliente, onEditar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(McGrisOscuro, RoundedCornerShape(16.dp))
            .border(2.dp, McGrisBorde, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(McNegro, CircleShape)
                .border(1.dp, McGrisBorde, CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cliente.nombre,
                color = McBlanco,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = cliente.dias, color = McGrisTexto, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Objetivo: ${cliente.objetivo}",
                color = McGrisTexto,
                fontSize = 12.sp
            )
        }

        Button(
            onClick = onEditar,
            colors = ButtonDefaults.buttonColors(containerColor = McMorado),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = "Editar", color = McNegro, fontSize = 13.sp)
        }
    }
}

// =========================
// MODAL
// =========================

@Composable
private fun McEditarDialog(
    diasInicial: String,
    objetivoInicial: String,
    onDismiss: () -> Unit,
    onGuardar: (dias: String, objetivo: String) -> Unit
) {
    var dias by remember { mutableStateOf(diasInicial) }
    var objetivo by remember { mutableStateOf(objetivoInicial) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(McGrisBorde, RoundedCornerShape(8.dp))
                .border(2.dp, McDorado, RoundedCornerShape(8.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Días que asistirá",
                color = McBlanco,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            McCampo(valor = dias, onCambio = { dias = it })

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Objetivo",
                color = McBlanco,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            McCampo(valor = objetivo, onCambio = { objetivo = it })

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { onGuardar(dias.trim(), objetivo.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = McMorado),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 10.dp)
            ) {
                Text(text = "Guardar", color = McNegro, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun McCampo(valor: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        singleLine = true,
        shape = RoundedCornerShape(50),
        textStyle = TextStyle(color = McBlanco, fontSize = 15.sp, textAlign = TextAlign.Center),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = McMorado,
            unfocusedBorderColor = McMorado,
            focusedTextColor = McBlanco,
            unfocusedTextColor = McBlanco,
            cursorColor = McMorado,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// =========================
// ITEM BARRA INFERIOR
// =========================

@Composable
private fun McBarraItem(
    modifier: Modifier,
    icono: ImageVector,
    texto: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = texto,
            tint = color,
            modifier = Modifier.size(40.dp)
        )
        Text(text = texto, color = color, fontSize = 15.sp)
    }
}