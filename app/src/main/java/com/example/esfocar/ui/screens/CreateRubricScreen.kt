package com.example.esfocar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esfocar.data.model.CriterioRubrica
import com.example.esfocar.data.model.User
import com.example.esfocar.ui.viewmodel.RubricaState
import com.example.esfocar.ui.viewmodel.RubricaViewModel
import kotlin.math.abs

data class DraftCriterio(
    var nombre: String = "",
    var descripcion: String = "",
    var puntajeMaximoStr: String = "10.0",
    var porcentajeStr: String = "25.0"
)

@Composable
fun CreateRubricScreen(
    currentUser: User?,
    rubricaViewModel: RubricaViewModel
) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    val borradorCriterios = remember {
        mutableStateListOf(
            DraftCriterio("Presentación Personal", "Uniforme e higiene postural", "10.0", "25.0"),
            DraftCriterio("Ejecución Técnica", "Dominio del procedimiento estándar", "10.0", "50.0"),
            DraftCriterio("Disciplina y Seguridad", "Cumplimiento de protocolos de seguridad", "10.0", "25.0")
        )
    }

    val rubricaState by rubricaViewModel.rubricaState.collectAsState()
    val scrollState = rememberScrollState()

    val customFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color(0xFFA6BCB0),
        unfocusedLabelColor = Color(0xFF405247),
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedTextColor = Color(0xFF111C16),
        unfocusedTextColor = Color(0xFF111C16)
    )

    // Suma de Porcentajes
    val sumaPorcentajes = borradorCriterios.sumOf { it.porcentajeStr.toDoubleOrNull() ?: 0.0 }
    val esSumaValida = abs(sumaPorcentajes - 100.0) < 0.01

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Header Verde Institucional
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Crear Rúbrica de Evaluación",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Criterios con ponderación porcentual total 100%",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Card Formulario Rúbrica
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F8F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Banner de Feedback
                when (rubricaState) {
                    is RubricaState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = (rubricaState as RubricaState.Error).message,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    is RubricaState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = (rubricaState as RubricaState.Success).message,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    else -> {}
                }

                // Campo Título Rúbrica
                OutlinedTextField(
                    value = titulo,
                    onValueChange = {
                        titulo = it
                        rubricaViewModel.resetState()
                    },
                    label = { Text("Título de la Rúbrica *") },
                    placeholder = { Text("Ej: Pauta Práctica Tiro Policial 2026") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Title, contentDescription = null) },
                    singleLine = true,
                    colors = customFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                // Campo Descripción Rúbrica
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        rubricaViewModel.resetState()
                    },
                    label = { Text("Descripción / Instrucciones Generales") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Description, contentDescription = null) },
                    minLines = 2,
                    maxLines = 3,
                    colors = customFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = Color(0xFFD0E2D7))

                // Encabezado de Criterios y Contador de Porcentaje Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Criterios Evaluativos (${borradorCriterios.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (esSumaValida) MaterialTheme.colorScheme.primary else Color(0xFFC62828),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format("Suma: %.1f%%", sumaPorcentajes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Lista Dinámica de Criterios
                borradorCriterios.forEachIndexed { index, crit ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Criterio N° ${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (borradorCriterios.size > 1) {
                                    IconButton(
                                        onClick = { borradorCriterios.removeAt(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar criterio",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = crit.nombre,
                                onValueChange = {
                                    borradorCriterios[index] = crit.copy(nombre = it)
                                },
                                label = { Text("Nombre Criterio (ej: Técnica)") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Score, contentDescription = null) },
                                singleLine = true,
                                colors = customFieldColors,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = crit.puntajeMaximoStr,
                                    onValueChange = {
                                        borradorCriterios[index] = crit.copy(puntajeMaximoStr = it)
                                    },
                                    label = { Text("Puntaje Máx.") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Score, contentDescription = null) },
                                    singleLine = true,
                                    colors = customFieldColors,
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = crit.porcentajeStr,
                                    onValueChange = {
                                        borradorCriterios[index] = crit.copy(porcentajeStr = it)
                                    },
                                    label = { Text("Peso (%)") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Percent, contentDescription = null) },
                                    singleLine = true,
                                    colors = customFieldColors,
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Botón Agregar Criterio
                OutlinedButton(
                    onClick = {
                        borradorCriterios.add(DraftCriterio())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "AGREGAR OTRO CRITERIO", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Botón Crear Rúbrica
                Button(
                    onClick = {
                        val criteriosFinales = borradorCriterios.map { draft ->
                            CriterioRubrica(
                                nombre = draft.nombre.trim(),
                                descripcion = draft.descripcion.trim(),
                                puntajeMaximo = draft.puntajeMaximoStr.toDoubleOrNull() ?: 0.0,
                                porcentaje = draft.porcentajeStr.toDoubleOrNull() ?: 0.0
                            )
                        }
                        val creadorId = currentUser?.id ?: 1L
                        rubricaViewModel.crearRubrica(context, titulo, descripcion, criteriosFinales, creadorId)
                    },
                    enabled = rubricaState !is RubricaState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    if (rubricaState is RubricaState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text = "GUARDAR RÚBRICA EN SISTEMA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
