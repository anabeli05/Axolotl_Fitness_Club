package com.example.gymcontrol.ui.encargado.gastos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymcontrol.GymApp
import com.example.gymcontrol.ui.components.ScreenContainer

@Composable
fun GastosScreen() {
    var showForm by remember { mutableStateOf(false) }
    var concept by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    ScreenContainer("Gastos") {
        Button(onClick = { showForm = !showForm }) { Text("Agregar gasto") }
        if (showForm) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(concept, { concept = it }, label = { Text("Concepto") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(amount, { amount = it }, label = { Text("Monto") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(date, { date = it }, label = { Text("Fecha") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { showForm = false }) { Text("Guardar") }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GymApp.repository.expenses()) { expense ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(expense.concept, style = MaterialTheme.typography.titleMedium)
                        Text("Categoría: ${expense.category}")
                        Text("Monto: $${expense.amount}")
                        Text("Fecha: ${expense.date}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {}) { Text("Editar") }
                            OutlinedButton(onClick = {}) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }
}
