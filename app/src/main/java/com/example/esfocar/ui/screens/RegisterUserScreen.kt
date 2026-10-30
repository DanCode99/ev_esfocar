package com.example.esfocar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esfocar.data.model.User
import com.example.esfocar.ui.viewmodel.AuthViewModel
import com.example.esfocar.ui.viewmodel.RegisterUserState
import com.example.esfocar.ui.viewmodel.UpdateUserState
import com.example.esfocar.ui.viewmodel.UserViewModel

/**
 * Formatea automáticamente el número ingresado al estándar de RUT Chileno (XX.XXX.XXX-X).
 */
fun formatRutChilean(rawInput: String): String {
    val cleaned = rawInput.filter { it.isDigit() || it == 'k' || it == 'K' }.take(9)
    if (cleaned.isEmpty()) return ""

    if (cleaned.length == 1) return cleaned.uppercase()

    val dv = cleaned.last().uppercaseChar()
    val body = cleaned.dropLast(1)

    val formattedBody = StringBuilder()
    var count = 0
    for (i in body.length - 1 downTo 0) {
        if (count > 0 && count % 3 == 0) {
            formattedBody.insert(0, '.')
        }
        formattedBody.insert(0, body[i])
        count++
    }

    return "$formattedBody-$dv"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterUserScreen(userViewModel: UserViewModel, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var rut by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(User.ROLE_DOCENTE) }

    var expandedRoleMenu by remember { mutableStateOf(false) }

    // Estados de acordeón / desplegables (CERRADOS POR DEFECTO)
    var isRegisterFormExpanded by remember { mutableStateOf(false) }
    var isUserListExpanded by remember { mutableStateOf(false) }
    val updateState by userViewModel.updateState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    var userToEdit by remember { mutableStateOf<User?>(null) }

    val registerState by userViewModel.registerState.collectAsState()
    val usersList by userViewModel.usersList.collectAsState()

    val rolesList = listOf(User.ROLE_DOCENTE, User.ROLE_INSTRUCTOR, User.ROLE_ADMIN)
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

    // DIÁLOGO DE EDICIÓN
    if (userToEdit != null) {
        var editEmail by remember { mutableStateOf(userToEdit!!.email) }
        var editFullName by remember { mutableStateOf(userToEdit!!.fullName) }
        var editRut by remember { mutableStateOf(userToEdit!!.rut) }
        var editRole by remember { mutableStateOf(userToEdit!!.role) }
        var editExpandedRoleMenu by remember { mutableStateOf(false) }

        // Cerrar dialog y limpiar estado al tener éxito
        LaunchedEffect(updateState) {
            if (updateState is UpdateUserState.Success) {
                if (userToEdit?.id == currentUser?.id) {
                    authViewModel.refreshCurrentUser()
                }
                userToEdit = null
                userViewModel.resetUpdateState()
            }
        }

        AlertDialog(
            onDismissRequest = {
                userToEdit = null
                userViewModel.resetUpdateState()
            },
            containerColor = Color.White,
            title = { Text("Editar Usuario", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (updateState is UpdateUserState.Error) {
                        Text(
                            text = (updateState as UpdateUserState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(value = editEmail, onValueChange = { editEmail = it; userViewModel.resetUpdateState() }, label = { Text("Correo") }, colors = customFieldColors, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editFullName, onValueChange = { editFullName = it; userViewModel.resetUpdateState() }, label = { Text("Nombre Completo") }, colors = customFieldColors, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editRut, onValueChange = { editRut = formatRutChilean(it); userViewModel.resetUpdateState() }, label = { Text("RUT") }, colors = customFieldColors, singleLine = true, modifier = Modifier.fillMaxWidth())

                    ExposedDropdownMenuBox(expanded = editExpandedRoleMenu, onExpandedChange = { editExpandedRoleMenu = !editExpandedRoleMenu }) {
                        OutlinedTextField(value = editRole, onValueChange = {}, readOnly = true, label = { Text("Rol") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editExpandedRoleMenu) }, colors = customFieldColors, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = editExpandedRoleMenu, onDismissRequest = { editExpandedRoleMenu = false }) {
                            listOf(User.ROLE_DOCENTE, User.ROLE_INSTRUCTOR, User.ROLE_ADMIN).forEach { itemRole ->
                                DropdownMenuItem(text = { Text(itemRole) }, onClick = { editRole = itemRole; editExpandedRoleMenu = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { userViewModel.updateUser(userToEdit!!.id, editFullName, editRut, editEmail, editRole) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (updateState is UpdateUserState.Loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEdit = null; userViewModel.resetUpdateState() }) { Text("Cancelar", color = Color.Gray) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // FONDO BLANCO PURO
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
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gestión de Usuarios",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Panel exclusivo para Administradores Esfocar",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // ==========================================
        // PARTE 1: REGISTRO DE USUARIOS (CERRADO POR DEFECTO)
        // ==========================================
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
            Column {
                // Cabecera Desplegable Parte 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRegisterFormExpanded = !isRegisterFormExpanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "1. Registrar Nuevo Usuario",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Formulario de ingreso con correo y contraseña",
                                fontSize = 11.sp,
                                color = Color(0xFF405247)
                            )
                        }
                    }

                    IconButton(onClick = { isRegisterFormExpanded = !isRegisterFormExpanded }) {
                        Icon(
                            imageVector = if (isRegisterFormExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Desplegar formulario",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Contenido del Formulario (Animado al Desplegar)
                AnimatedVisibility(
                    visible = isRegisterFormExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HorizontalDivider(color = Color(0xFFD0E2D7))

                        // Estado de Éxito o Error
                        when (registerState) {
                            is RegisterUserState.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
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
                                            text = (registerState as RegisterUserState.Error).message,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                            is RegisterUserState.Success -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
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
                                            text = (registerState as RegisterUserState.Success).message,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }

                        // Campo Email (Login)
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                userViewModel.resetRegisterState()
                            },
                            label = { Text("Correo") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            colors = customFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Campo Password Temporal
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                userViewModel.resetRegisterState()
                            },
                            label = { Text("Contraseña Temporal") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                            singleLine = true,
                            colors = customFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Campo Nombre Completo
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                userViewModel.resetRegisterState()
                            },
                            label = { Text("Nombre Completo") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Badge, contentDescription = null) },
                            singleLine = true,
                            colors = customFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Campo RUT (Con AUTO-FORMATO de Puntos y Guión)
                        OutlinedTextField(
                            value = rut,
                            onValueChange = { input ->
                                rut = formatRutChilean(input)
                                userViewModel.resetRegisterState()
                            },
                            label = { Text("RUT (Ej: 12.345.678-K)") },
                            placeholder = { Text("12.345.678-K") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Badge, contentDescription = null) },
                            singleLine = true,
                            colors = customFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Selector de Rol
                        ExposedDropdownMenuBox(
                            expanded = expandedRoleMenu,
                            onExpandedChange = { expandedRoleMenu = !expandedRoleMenu }
                        ) {
                            OutlinedTextField(
                                value = role,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Rol Asignado") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Work, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoleMenu) },
                                colors = customFieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = expandedRoleMenu,
                                onDismissRequest = { expandedRoleMenu = false }
                            ) {
                                rolesList.forEach { itemRole ->
                                    DropdownMenuItem(
                                        text = { Text(itemRole) },
                                        onClick = {
                                            role = itemRole
                                            expandedRoleMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val success = userViewModel.registerUser(
                                    tempPassword = password,
                                    fullName = fullName,
                                    rut = rut,
                                    email = email,
                                    role = role
                                )
                                if (success) {
                                    password = ""
                                    fullName = ""
                                    rut = ""
                                    email = ""
                                }
                            },
                            enabled = registerState !is RegisterUserState.Loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            if (registerState is RegisterUserState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = "CREAR NUEVO USUARIO",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ===============================================
        // PARTE 2: LISTA DE USUARIOS REGISTRADOS (CERRADO POR DEFECTO)
        // ===============================================
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
            Column {
                // Cabecera Desplegable Parte 2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isUserListExpanded = !isUserListExpanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "2. Usuarios Registrados (${usersList.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Docentes, Instructores y Administradores en SQLite",
                                fontSize = 11.sp,
                                color = Color(0xFF405247)
                            )
                        }

                    }

                    IconButton(onClick = { isUserListExpanded = !isUserListExpanded }) {
                        Icon(
                            imageVector = if (isUserListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Desplegar lista de usuarios",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

// Contenido de la Lista (Animado al Desplegar)
                AnimatedVisibility(
                    visible = isUserListExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        HorizontalDivider(color = Color(0xFFD0E2D7))
                        Spacer(modifier = Modifier.height(12.dp))

                        usersList.forEachIndexed { index, user ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF111C16)
                                        )
                                        Text(
                                            text = "${user.email}\nRUT: ${user.rut}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF405247)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp) // <--- Distancia entre ambos
                                    ) {
                                        // Etiqueta de Rol
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (user.isAdmin) MaterialTheme.colorScheme.secondary
                                                    else MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = user.role,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        // Botón de Editar
                                        IconButton(
                                            onClick = { userToEdit = user },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color(0xFFE2F0E8), RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Editar",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                if (index < usersList.size - 1) {
                                    HorizontalDivider(color = Color(0xFFD0E2D7))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
