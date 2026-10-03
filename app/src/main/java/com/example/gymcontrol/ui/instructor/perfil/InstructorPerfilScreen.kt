package com.example.gymcontrol.ui.instructor.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =========================
// COLORES DEL DISEÑO
// =========================

private val Negro = Color(0xFF000000)
private val GrisOscuro = Color(0xFF202121)
private val GrisBorde = Color(0xFF494A4A)
private val Morado = Color(0xFFA655E5)
private val Dorado = Color(0xFFA68A00)
private val Blanco = Color(0xFFFFFFFF)
private val GrisTexto = Color(0xFFBDBDBD)
private val Verde = Color(0xFF5DBB63)
private val Rojo = Color(0xFFE53935)

// =========================
// TAMAÑOS DE ICONOS (cámbialos aquí)
// =========================

private val IconoFoto = 68.dp
private val IconoEditar = 28.dp
private val IconoFlecha = 30.dp
private val IconoDato = 28.dp
private val IconoBarra = 40.dp

@Composable
fun InstructorPerfilScreen() {

    var capacity by remember { mutableStateOf("3") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Negro)
    ) {

        // ENCABEZADO
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp)
        ) {
            Text(
                text = "AXOLOTL",
                color = Dorado,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Dorado, thickness = 1.dp)
        }

        // CONTENIDO
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        ) {

            Text(
                text = "Mi Perfil",
                color = Blanco,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // TARJETA PRINCIPAL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = Morado,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // FOTO
                Box(
                    modifier = Modifier
                        .size(115.dp)
                        .background(color = GrisOscuro, shape = CircleShape)
                        .border(width = 2.dp, color = Blanco, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Foto de perfil",
                        tint = GrisTexto,
                        modifier = Modifier.size(IconoFoto)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // NOMBRE
                Text(
                    text = "Maclovin Ramiez",
                    color = Blanco,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ESTADO
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Text(text = "Estado:", color = GrisTexto, fontSize = 15.sp)

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Activo",
                        color = Blanco,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .background(color = Verde, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // BOTÓN EDITAR
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(9.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = Blanco,
                            modifier = Modifier.size(IconoEditar)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Editar datos",
                            color = Blanco,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Blanco,
                            modifier = Modifier.size(IconoFlecha)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DATOS PERSONALES
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color.Black, shape = RoundedCornerShape(9.dp))
                        .border(
                            width = 1.dp,
                            color = GrisBorde,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .padding(16.dp)
                ) {

                    Text(
                        text = "Datos Personales",
                        color = Blanco,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(
                        color = GrisBorde,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 9.dp)
                    )

                    DatoFila(Icons.Outlined.Person, "Nombre:", "Maclovin Ramiez")

                    Spacer(modifier = Modifier.height(13.dp))

                    DatoFila(Icons.Outlined.Phone, "Teléfono:", "+52 248 769 3720")

                    Spacer(modifier = Modifier.height(13.dp))

                    DatoFila(Icons.Outlined.Email, "Correo:", "loxd@hotmail.com")

                    Spacer(modifier = Modifier.height(13.dp))

                    // Capacidad
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = GrisTexto,
                            modifier = Modifier.size(IconoDato)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(text = "Capacidad máx:", color = GrisTexto, fontSize = 15.sp)

                        Spacer(modifier = Modifier.width(7.dp))

                        Text(text = capacity, color = Blanco, fontSize = 15.sp)

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Lleno",
                            color = Blanco,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .background(color = Rojo, shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // BARRA INFERIOR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .border(width = 1.dp, color = GrisBorde)
                .background(Negro),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // MIS CLIENTES
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Mis clientes",
                    tint = GrisTexto,
                    modifier = Modifier.size(IconoBarra)
                )
                Text(text = "Mis clientes", color = GrisTexto, fontSize = 15.sp)
            }

            // PERFIL
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Perfil",
                    tint = Morado,
                    modifier = Modifier.size(IconoBarra)
                )
                Text(text = "Perfil", color = Morado, fontSize = 15.sp)
            }
        }
    }
}

// =========================
// FILA DE DATO CON ICONO
// =========================

@Composable
private fun DatoFila(icono: ImageVector, etiqueta: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = GrisTexto,
            modifier = Modifier.size(IconoDato)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = etiqueta,
            color = GrisTexto,
            fontSize = 15.sp,
            modifier = Modifier.width(90.dp)
        )
        Text(text = valor, color = Blanco, fontSize = 15.sp)
    }
}