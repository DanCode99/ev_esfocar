package com.example.esfocar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.esfocar.data.model.User
import com.example.esfocar.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class RegisterUserState {
    object Idle : RegisterUserState()
    object Loading : RegisterUserState()
    data class Success(val message: String) : RegisterUserState()
    data class Error(val message: String) : RegisterUserState()
}

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterUserState>(RegisterUserState.Idle)
    val registerState: StateFlow<RegisterUserState> = _registerState.asStateFlow()

    private val _usersList = MutableStateFlow<List<User>>(emptyList())
    val usersList: StateFlow<List<User>> = _usersList.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        _usersList.value = userRepository.getAllUsers()
    }

    fun registerUser(
        username: String,
        tempPassword: String,
        fullName: String,
        rut: String,
        email: String,
        role: String
    ) {
        if (username.isBlank() || username.length < 3) {
            _registerState.value = RegisterUserState.Error("El nombre de usuario debe tener al menos 3 caracteres.")
            return
        }
        if (tempPassword.isBlank() || tempPassword.length < 6) {
            _registerState.value = RegisterUserState.Error("La contraseña temporal debe tener al menos 6 caracteres.")
            return
        }
        if (fullName.isBlank()) {
            _registerState.value = RegisterUserState.Error("El nombre completo es obligatorio.")
            return
        }
        if (rut.isBlank()) {
            _registerState.value = RegisterUserState.Error("El RUT es obligatorio.")
            return
        }
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerState.value = RegisterUserState.Error("Ingrese un correo electrónico institucional válido.")
            return
        }

        _registerState.value = RegisterUserState.Loading

        val newUser = User(
            username = username,
            password = tempPassword,
            fullName = fullName,
            rut = rut,
            email = email,
            role = role,
            isFirstLogin = 1 // Debe cambiar su clave en primer inicio
        )

        val result = userRepository.createUser(newUser)
        result.onSuccess {
            _registerState.value = RegisterUserState.Success("Usuario '${newUser.username}' creado con éxito.")
            loadUsers()
        }.onFailure { ex ->
            _registerState.value = RegisterUserState.Error(ex.message ?: "Error al registrar el usuario.")
        }
    }

    fun resetRegisterState() {
        _registerState.value = RegisterUserState.Idle
    }

    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserViewModel(userRepository) as T
        }
    }
}
