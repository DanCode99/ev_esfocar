package com.example.esfocar

import com.example.esfocar.data.model.User
import com.example.esfocar.data.repository.UserRepository
import com.example.esfocar.ui.viewmodel.AuthState
import com.example.esfocar.ui.viewmodel.AuthViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para AuthViewModel utilizando MockK y Coroutines Test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var userRepository: UserRepository
    private lateinit var authViewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk(relaxed = true)
        authViewModel = AuthViewModel(userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login con correo vacio emite estado Error`() = runTest {
        authViewModel.login("", "admin123")

        val state = authViewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Por favor, ingrese su correo institucional.", (state as AuthState.Error).message)
    }

    @Test
    fun `login con credenciales invalidas emite estado Error`() = runTest {
        coEvery { userRepository.authenticate(any(), "invalido@gmail.com", "123") } returns null

        authViewModel.login("invalido@gmail.com", "123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = authViewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Credenciales incorrectas. Verifique su correo y contraseña.", (state as AuthState.Error).message)
    }

    @Test
    fun `login con credenciales validas e isFirstLogin 1 emite RequiresPasswordChange`() = runTest {
        val testUser = User(
            id = 1,
            email = "admin3@gmail.com",
            password = "123",
            fullName = "Admin 3",
            rut = "33.333.333-3",
            role = User.ROLE_ADMIN,
            isFirstLogin = 1
        )
        coEvery { userRepository.authenticate(any(), "admin3@gmail.com", "123") } returns testUser

        authViewModel.login("admin3@gmail.com", "123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = authViewModel.authState.value
        assertTrue(state is AuthState.RequiresPasswordChange)
        assertEquals(testUser, (state as AuthState.RequiresPasswordChange).user)
    }

    @Test
    fun `login con credenciales validas e isFirstLogin 0 emite Authenticated`() = runTest {
        val testUser = User(
            id = 1,
            email = "admin1@gmail.com",
            password = "123",
            fullName = "Admin 1",
            rut = "11.111.111-1",
            role = User.ROLE_ADMIN,
            isFirstLogin = 0
        )
        coEvery { userRepository.authenticate(any(), "admin1@gmail.com", "123") } returns testUser

        authViewModel.login("admin1@gmail.com", "123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = authViewModel.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals(testUser, (state as AuthState.Authenticated).user)
    }

    @Test
    fun `logout resetea el usuario activo y emite Idle`() = runTest {
        authViewModel.logout()

        assertEquals(null, authViewModel.currentUser.value)
        assertTrue(authViewModel.authState.value is AuthState.Idle)
    }
}
