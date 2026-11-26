package com.example.esfocar

import com.example.esfocar.data.model.User
import com.example.esfocar.data.repository.UserRepository
import com.example.esfocar.ui.viewmodel.AuthState
import com.example.esfocar.ui.viewmodel.AuthViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para AuthViewModel utilizando MockK.
 */
class AuthViewModelTest {

    private lateinit var userRepository: UserRepository
    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setUp() {
        userRepository = mockk(relaxed = true)
        authViewModel = AuthViewModel(userRepository)
    }

    @Test
    fun `login con correo vacio emite estado Error`() {
        authViewModel.login("", "admin123")

        val state = authViewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Por favor, ingrese su correo institucional.", (state as AuthState.Error).message)
    }

    @Test
    fun `login con credenciales invalidas emite estado Error`() {
        every { userRepository.authenticate("invalido@gmail.com", "123") } returns null

        authViewModel.login("invalido@gmail.com", "123")

        val state = authViewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Credenciales incorrectas. Verifique su correo y contraseña.", (state as AuthState.Error).message)
    }

    @Test
    fun `login con credenciales validas e isFirstLogin 1 emite RequiresPasswordChange`() {
        val testUser = User(
            id = 1,
            email = "admin3@gmail.com",
            password = "123",
            fullName = "Admin 3",
            rut = "33.333.333-3",
            role = User.ROLE_ADMIN,
            isFirstLogin = 1
        )
        every { userRepository.authenticate("admin3@gmail.com", "123") } returns testUser

        authViewModel.login("admin3@gmail.com", "123")

        val state = authViewModel.authState.value
        assertTrue(state is AuthState.RequiresPasswordChange)
        assertEquals(testUser, (state as AuthState.RequiresPasswordChange).user)
    }

    @Test
    fun `login con credenciales validas e isFirstLogin 0 emite Authenticated`() {
        val testUser = User(
            id = 1,
            email = "admin1@gmail.com",
            password = "123",
            fullName = "Admin 1",
            rut = "11.111.111-1",
            role = User.ROLE_ADMIN,
            isFirstLogin = 0
        )
        every { userRepository.authenticate("admin1@gmail.com", "123") } returns testUser

        authViewModel.login("admin1@gmail.com", "123")

        val state = authViewModel.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals(testUser, (state as AuthState.Authenticated).user)
    }

    @Test
    fun `logout resetea el usuario activo y emite Idle`() {
        authViewModel.logout()

        assertEquals(null, authViewModel.currentUser.value)
        assertTrue(authViewModel.authState.value is AuthState.Idle)
    }
}
