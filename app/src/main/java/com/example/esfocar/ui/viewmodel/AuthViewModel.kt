package com.example.esfocar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.esfocar.data.model.User
import com.example.esfocar.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class RequiresPasswordChange(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun login(emailInput: String, passwordInput: String) {
        if (emailInput.isBlank()) {
            _authState.value = AuthState.Error("Por favor, ingrese su correo institucional.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput.trim()).matches()) {
            _authState.value = AuthState.Error("Ingrese un formato de correo electrónico válido.")
            return
        }
        if (passwordInput.isBlank()) {
            _authState.value = AuthState.Error("Por favor, ingrese la contraseña.")
            return
        }

        _authState.value = AuthState.Loading

        val user = userRepository.authenticate(emailInput, passwordInput)
        if (user != null) {
            _currentUser.value = user
            if (user.requiresPasswordChange) {
                _authState.value = AuthState.RequiresPasswordChange(user)
            } else {
                _authState.value = AuthState.Authenticated(user)
            }
        } else {
            _authState.value = AuthState.Error("Credenciales incorrectas. Verifique su correo y contraseña.")
        }
    }

    fun changePassword(newPasswordInput: String, confirmPasswordInput: String) {
        val user = _currentUser.value ?: run {
            _authState.value = AuthState.Error("No hay una sesión activa para cambiar la clave.")
            return
        }

        if (newPasswordInput.isBlank()) {
            _authState.value = AuthState.Error("Ingrese la nueva contraseña.")
            return
        }
        if (newPasswordInput.length < 6) {
            _authState.value = AuthState.Error("La nueva contraseña debe tener al menos 6 caracteres.")
            return
        }
        if (newPasswordInput != confirmPasswordInput) {
            _authState.value = AuthState.Error("Las contraseñas no coinciden.")
            return
        }
        if (newPasswordInput == user.password) {
            _authState.value = AuthState.Error("La nueva contraseña debe ser diferente a la temporal.")
            return
        }

        val success = userRepository.updatePassword(user.id, newPasswordInput)
        if (success) {
            refreshCurrentUser()
        } else {
            _authState.value = AuthState.Error("Error al actualizar la contraseña en la base de datos.")
        }
    }

    fun logout() {
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    /*
    * Refresca los datos del usuario en sesión actual.
    * Útil cuando el administrador modifica su propio perfil.
    */
    fun refreshCurrentUser() {
        val currentId = _currentUser.value?.id ?: return
        val updatedUser = userRepository.getUserById(currentId)
        if (updatedUser != null) {
            _currentUser.value = updatedUser
            val currentState = _authState.value
            if (currentState is AuthState.Authenticated) {
                _authState.value = AuthState.Authenticated(updatedUser)
            } else if (currentState is AuthState.RequiresPasswordChange) {
                _authState.value = AuthState.RequiresPasswordChange(updatedUser)
            }
        }
    }

    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(userRepository) as T
        }
    }
}
