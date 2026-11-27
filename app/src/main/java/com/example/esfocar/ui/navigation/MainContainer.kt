package com.example.esfocar.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esfocar.data.db.DatabaseHelper
import com.example.esfocar.data.remote.NetworkUtils
import com.example.esfocar.data.repository.DocumentRepository
import com.example.esfocar.data.repository.EvaluacionRepository
import com.example.esfocar.data.repository.RubricaRepository
import com.example.esfocar.data.repository.SyncManager
import com.example.esfocar.data.repository.UserRepository
import com.example.esfocar.ui.screens.CreateRubricScreen
import com.example.esfocar.ui.screens.DashboardScreen
import com.example.esfocar.ui.screens.DocumentsScreen
import com.example.esfocar.ui.screens.EditPasswordScreen
import com.example.esfocar.ui.screens.EvaluateStudentScreen
import com.example.esfocar.ui.screens.LoginScreen
import com.example.esfocar.ui.screens.MisNotasScreen
import com.example.esfocar.ui.screens.RegisterUserScreen
import com.example.esfocar.ui.screens.SplashScreen
import com.example.esfocar.ui.screens.StudentDashboardScreen
import com.example.esfocar.ui.viewmodel.AuthState
import com.example.esfocar.ui.viewmodel.AuthViewModel
import com.example.esfocar.ui.viewmodel.DocumentViewModel
import com.example.esfocar.ui.viewmodel.EvaluacionViewModel
import com.example.esfocar.ui.viewmodel.RubricaViewModel
import com.example.esfocar.ui.viewmodel.UserViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class Screen {
    SPLASH,
    LOGIN,
    EDIT_PASSWORD,
    DASHBOARD,
    STUDENT_DASHBOARD,
    DOCUMENTS,
    REGISTER_USER,
    CREATE_RUBRIC,
    EVALUATE_STUDENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer() {
    val context = LocalContext.current

    // Inicialización de SQLiteHelper y Repositorios
    val dbHelper = remember { DatabaseHelper(context) }
    val userRepository = remember { UserRepository(dbHelper) }
    val documentRepository = remember { DocumentRepository(dbHelper) }
    val rubricaRepository = remember { RubricaRepository(dbHelper) }
    val evaluacionRepository = remember { EvaluacionRepository(dbHelper) }
    val syncManager = remember { SyncManager(documentRepository, rubricaRepository, evaluacionRepository) }

    // ViewModels
    val authViewModel = remember { AuthViewModel(userRepository) }
    val userViewModel = remember { UserViewModel(userRepository) }
    val documentViewModel = remember { DocumentViewModel(documentRepository) }
    val rubricaViewModel = remember { RubricaViewModel(rubricaRepository) }
    val evaluacionViewModel = remember { EvaluacionViewModel(evaluacionRepository) }

    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }

    // Tarea Periódica Silenciosa de Sincronización Background (Sin sobrecargar servidor)
    LaunchedEffect(authState) {
        while (isActive) {
            delay(30000) // Verifica cada 30 segundos
            if (NetworkUtils.isNetworkAvailable(context) && syncManager.getPendingSyncCount() > 0) {
                try {
                    syncManager.syncAll(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Reactividad para cambio de pantalla según estado de autenticación y rol
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.RequiresPasswordChange -> {
                currentScreen = Screen.EDIT_PASSWORD
            }
            is AuthState.Authenticated -> {
                val user = (authState as AuthState.Authenticated).user
                if (currentScreen == Screen.SPLASH || currentScreen == Screen.LOGIN || currentScreen == Screen.EDIT_PASSWORD) {
                    currentScreen = if (user.isEstudiante) Screen.STUDENT_DASHBOARD else Screen.DASHBOARD
                }
            }
            is AuthState.Idle -> {
                if (currentScreen != Screen.SPLASH) {
                    currentScreen = Screen.LOGIN
                }
            }
            else -> {}
        }
    }

    val showBottomBar = currentScreen in listOf(
        Screen.DASHBOARD,
        Screen.STUDENT_DASHBOARD,
        Screen.DOCUMENTS,
        Screen.REGISTER_USER,
        Screen.CREATE_RUBRIC,
        Screen.EVALUATE_STUDENT
    )

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = Color(0xFF5A6F64),
        unselectedTextColor = Color(0xFF5A6F64),
        indicatorColor = Color(0xFFE0F2E9)
    )

    Scaffold(
        containerColor = Color.White,
        topBar = {
            if (showBottomBar || currentScreen == Screen.EDIT_PASSWORD) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                Screen.EDIT_PASSWORD -> "Configuración Inicial de Clave"
                                Screen.DASHBOARD -> "Esfocar - Menú Principal"
                                Screen.STUDENT_DASHBOARD -> "Portal Carabinero Alumno"
                                Screen.CREATE_RUBRIC -> "Crear Rúbrica"
                                Screen.EVALUATE_STUDENT -> "Evaluar Estudiante"
                                Screen.DOCUMENTS -> "Documentos Institucionales"
                                Screen.REGISTER_USER -> "Registrar Usuario"
                                else -> "Esfocar"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    actions = {
                        if (currentUser != null) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = currentUser?.role ?: "DOCENTE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { authViewModel.logout() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Cerrar Sesión",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary,
                    tonalElevation = 6.dp
                ) {
                    if (currentUser?.isEstudiante == true) {
                        // Pestañas para Rol ESTUDIANTE
                        NavigationBarItem(
                            selected = currentScreen == Screen.STUDENT_DASHBOARD,
                            onClick = { currentScreen = Screen.STUDENT_DASHBOARD },
                            icon = { Icon(imageVector = Icons.Default.School, contentDescription = "Mis Notas") },
                            label = { Text("Mis Notas", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
                            colors = navItemColors
                        )

                        NavigationBarItem(
                            selected = currentScreen == Screen.DOCUMENTS,
                            onClick = { currentScreen = Screen.DOCUMENTS },
                            icon = { Icon(imageVector = Icons.Default.Description, contentDescription = "Documentos") },
                            label = { Text("Documentos", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = navItemColors
                        )
                    } else {
                        // Pestañas para Rol PROFESOR / ADMIN
                        NavigationBarItem(
                            selected = currentScreen == Screen.DASHBOARD,
                            onClick = { currentScreen = Screen.DASHBOARD },
                            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Inicio") },
                            label = { Text("Inicio", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
                            colors = navItemColors
                        )

                        NavigationBarItem(
                            selected = currentScreen == Screen.EVALUATE_STUDENT,
                            onClick = { currentScreen = Screen.EVALUATE_STUDENT },
                            icon = { Icon(imageVector = Icons.Default.Grade, contentDescription = "Evaluar") },
                            label = { Text("Evaluar", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
                            colors = navItemColors
                        )

                        NavigationBarItem(
                            selected = currentScreen == Screen.DOCUMENTS,
                            onClick = { currentScreen = Screen.DOCUMENTS },
                            icon = { Icon(imageVector = Icons.Default.Description, contentDescription = "Documentos") },
                            label = { Text("Documentos", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = navItemColors
                        )

                        if (currentUser?.isAdmin == true) {
                            NavigationBarItem(
                                selected = currentScreen == Screen.REGISTER_USER,
                                onClick = { currentScreen = Screen.REGISTER_USER },
                                icon = { Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Usuarios") },
                                label = { Text("Usuarios", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
                                colors = navItemColors
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .background(Color.White)
        ) {
            when (currentScreen) {
                Screen.SPLASH -> {
                    SplashScreen(
                        onSplashFinished = {
                            currentScreen = Screen.LOGIN
                        }
                    )
                }
                Screen.LOGIN -> {
                    LoginScreen(authViewModel = authViewModel)
                }
                Screen.EDIT_PASSWORD -> {
                    EditPasswordScreen(authViewModel = authViewModel)
                }
                Screen.DASHBOARD -> {
                    DashboardScreen(
                        currentUser = currentUser,
                        onNavigateToDocuments = { currentScreen = Screen.DOCUMENTS },
                        onNavigateToCreateRubric = { currentScreen = Screen.CREATE_RUBRIC },
                        onNavigateToEvaluateStudent = { currentScreen = Screen.EVALUATE_STUDENT }
                    )
                }
                Screen.STUDENT_DASHBOARD -> {
                    StudentDashboardScreen(
                        currentUser = currentUser,
                        evaluacionViewModel = evaluacionViewModel,
                        documentViewModel = documentViewModel,
                        rubricaViewModel = rubricaViewModel
                    )
                }
                Screen.CREATE_RUBRIC -> {
                    CreateRubricScreen(
                        currentUser = currentUser,
                        rubricaViewModel = rubricaViewModel
                    )
                }
                Screen.EVALUATE_STUDENT -> {
                    EvaluateStudentScreen(
                        currentUser = currentUser,
                        rubricaViewModel = rubricaViewModel,
                        userViewModel = userViewModel,
                        evaluacionViewModel = evaluacionViewModel
                    )
                }
                Screen.DOCUMENTS -> {
                    DocumentsScreen(
                        currentUser = currentUser,
                        documentViewModel = documentViewModel
                    )
                }
                Screen.REGISTER_USER -> {
                    RegisterUserScreen(
                        userViewModel = userViewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}
