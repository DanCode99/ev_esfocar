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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Score
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.esfocar.data.model.Rubrica
import com.example.esfocar.data.model.User
import com.example.esfocar.ui.viewmodel.EvaluacionState
import com.example.esfocar.ui.viewmodel.EvaluacionViewModel
import com.example.esfocar.ui.viewmodel.RubricaViewModel
import com.example.esfocar.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluateStudentScreen(
    currentUser: User?,
    rubricaViewModel: RubricaViewModel,
    userViewModel: UserViewModel,
    evaluacionViewModel: EvaluacionViewModel
) {
    val context = LocalContext.current

    val rubricasList by rubricaViewModel.rubricasList.collectAsState()
    val usersList by userViewModel.usersList.collectAsState()
    val evalState by evaluacionViewModel.evalState.collectAsState()
    val selectedRubrica by evaluacionViewModel.selectedRubrica.collectAsState()
    val puntajesMap by evaluacionViewModel.puntajesMap.collectAsState()
    val notaCalculada by evaluacionViewModel.notaCalculada.collectAsState()

    var selectedEstudiante by remember { mutableStateOf<User?>(null) }
    var observacion by remember { mutableStateOf("") }

    var expandedRubricaMenu by remember { mutableStateOf(false) }
    var expandedEstudianteMenu by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        rubricaViewModel.loadRubricas()
        userViewModel.loadUsers()
    }

    // Filtrar estudiantes (Rol ESTUDIANTE, o en su defecto docentes para pruebas)
    val estudiantesList = usersList.filter { it.isEstudiante || !it.isAdmin }

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
                        imageVector = Icons.Default.Grade,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Evaluar Estudiante",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Cálculo en tiempo real de nota final (Escala 1.00 a 7.00)",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Formulario de Evaluación
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Estado de Feedback
                when (evalState) {
                    is EvaluacionState.Error -> {
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
                                    text = (evalState as EvaluacionState.Error).message,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    is EvaluacionState.Success -> {
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
                                    text = (evalState as EvaluacionState.Success).message,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    else -> {}
                }

                // 1. Selector de Rúbrica
                ExposedDropdownMenuBox(
                    expanded = expandedRubricaMenu,
                    onExpandedChange = { expandedRubricaMenu = !expandedRubricaMenu }
                ) {
                    OutlinedTextField(
                        value = selectedRubrica?.titulo ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seleccionar Rúbrica de Evaluación *") },
                        placeholder = { Text("Elija una rúbrica creada") },
                        leadingIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRubricaMenu) },
                        colors = customFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedRubricaMenu,
                        onDismissRequest = { expandedRubricaMenu = false }
                    ) {
                        rubricasList.forEach { rub ->
                            DropdownMenuItem(
                                text = { Text("${rub.titulo} (${rub.criterios.size} criterios)") },
                                onClick = {
                                    evaluacionViewModel.selectRubrica(rub)
                                    expandedRubricaMenu = false
                                }
                            )
                        }
                    }
                }

                // 2. Selector de Estudiante
                ExposedDropdownMenuBox(
                    expanded = expandedEstudianteMenu,
                    onExpandedChange = { expandedEstudianteMenu = !expandedEstudianteMenu }
                ) {
                    OutlinedTextField(
                        value = selectedEstudiante?.let { "${it.fullName} (${it.role})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seleccionar Estudiante a Evaluar *") },
                        placeholder = { Text("Elija un estudiante registrado") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEstudianteMenu) },
                        colors = customFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedEstudianteMenu,
                        onDismissRequest = { expandedEstudianteMenu = false }
                    ) {
                        estudiantesList.forEach { est ->
                            DropdownMenuItem(
                                text = { Text("${est.fullName} - RUT: ${est.rut} [${est.role}]") },
                                onClick = {
                                    selectedEstudiante = est
                                    expandedEstudianteMenu = false
                                }
                            )
                        }
                    }
                }

                // TARJETA DE NOTA FINAL EN TIEMPO REAL (Calculada automáticamente)
                if (selectedRubrica != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "NOTA FINAL EN TIEMPO REAL",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "Fórmula Institucional (Escala 1.00 a 7.00)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (notaCalculada >= 4.00) Color.White else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = String.format("%.2f", notaCalculada),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (notaCalculada >= 4.00) MaterialTheme.colorScheme.primary else Color(0xFFC62828)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFD0E2D7))

                    Text(
                        text = "Ingresar Puntajes por Criterio",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Lista de Campos de Puntaje por Criterio
                    selectedRubrica?.criterios?.forEach { crit ->
                        val currentPuntaje = puntajesMap[crit.id] ?: 0.0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = crit.nombre,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE0F2E9), shape = RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = String.format("Peso: %.0f%% | Máx: %.1f", crit.porcentaje, crit.puntajeMaximo),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (crit.descripcion.isNotBlank()) {
                                    Text(
                                        text = crit.descripcion,
                                        fontSize = 12.sp,
                                        color = Color(0xFF405247)
                                    )
                                }

                                OutlinedTextField(
                                    value = if (currentPuntaje == 0.0) "" else currentPuntaje.toString(),
                                    onValueChange = { inputStr ->
                                        val valDouble = inputStr.toDoubleOrNull() ?: 0.0
                                        evaluacionViewModel.setPuntajeObtenido(crit.id, valDouble)
                                    },
                                    label = { Text("Puntaje Obtenido (0 a ${crit.puntajeMaximo})") },
                                    placeholder = { Text("0.0") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Score, contentDescription = null) },
                                    singleLine = true,
                                    colors = customFieldColors,
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Campo Observaciones
                    OutlinedTextField(
                        value = observacion,
                        onValueChange = { observacion = it },
                        label = { Text("Observaciones / Retroalimentación") },
                        placeholder = { Text("Ingrese comentarios o sugerencias para el estudiante...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Comment, contentDescription = null) },
                        minLines = 2,
                        maxLines = 3,
                        colors = customFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Botón Guardar Evaluación
                    Button(
                        onClick = {
                            val rub = selectedRubrica ?: return@Button
                            val estId = selectedEstudiante?.id ?: 0L
                            val evalId = currentUser?.id ?: 1L
                            evaluacionViewModel.guardarEvaluacion(context, rub, estId, evalId, observacion)
                            if (evalState is EvaluacionState.Success) {
                                observacion = ""
                                selectedEstudiante = null
                            }
                        },
                        enabled = evalState !is EvaluacionState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        if (evalState is EvaluacionState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text(
                                text = "GUARDAR EVALUACIÓN CON NOTA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
