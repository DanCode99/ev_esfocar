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

sealed class UpdateUserState {
    object Idle : UpdateUserState()
    object Loading : UpdateUserState()
    data class Success(val message: String) : UpdateUserState()
    data class Error(val message: String) : UpdateUserState()
}

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterUserState>(RegisterUserState.Idle)
    val registerState: StateFlow<RegisterUserState> = _registerState.asStateFlow()

    private val _usersList = MutableStateFlow<List<User>>(emptyList())
    val usersList: StateFlow<List<User>> = _usersList.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateUserState>(UpdateUserState.Idle)
    val updateState: StateFlow<UpdateUserState> = _updateState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        _usersList.value = userRepository.getAllUsers()
    }

    fun registerUser(
        tempPassword: String,
        fullName: String,
        rut: String,
        email: String,
        role: String
    ): Boolean {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _registerState.value = RegisterUserState.Error("Ingrese un correo electrónico institucional válido.")
            return false
        }
        if (tempPassword.isBlank() || tempPassword.length < 6) {
            _registerState.value = RegisterUserState.Error("La contraseña temporal debe tener al menos 6 caracteres.")
            return false
        }
        if (fullName.isBlank()) {
            _registerState.value = RegisterUserState.Error("El nombre completo es obligatorio.")
            return false
        }
        if (rut.isBlank()) {
            _registerState.value = RegisterUserState.Error("El RUT es obligatorio.")
            return false
        }

        _registerState.value = RegisterUserState.Loading

        val newUser = User(
            email = email.trim().lowercase(),
            password = tempPassword,
            fullName = fullName,
            rut = rut.trim().uppercase(),
            role = role,
            isFirstLogin = 1 // Debe cambiar su clave en primer inicio
        )

        val result = userRepository.createUser(newUser)
        return result.fold(
            onSuccess = {
                _registerState.value = RegisterUserState.Success("Usuario '${newUser.fullName}' creado con éxito.")
                loadUsers()
                true
            },
            onFailure = { ex ->
                _registerState.value = RegisterUserState.Error(ex.message ?: "Error al registrar el usuario.")
                false
            }
        )
    }

    fun updateUser(userId: Long, fullName: String, rut: String, email: String, role: String): Boolean {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _updateState.value = UpdateUserState.Error("Ingrese un correo electrónico válido.")
            return false
        }
        if (fullName.isBlank()) {
            _updateState.value = UpdateUserState.Error("El nombre completo es obligatorio.")
            return false
        }
        if (rut.isBlank()) {
            _updateState.value = UpdateUserState.Error("El RUT es obligatorio.")
            return false
        }

        _updateState.value = UpdateUserState.Loading
        val result = userRepository.updateUser(userId, fullName, rut, email, role)

        return result.fold(
            onSuccess = {
                _updateState.value = UpdateUserState.Success("Usuario actualizado con éxito.")
                loadUsers()
                true
            },
            onFailure = { ex ->
                _updateState.value = UpdateUserState.Error(ex.message ?: "Error al actualizar.")
                false
            }
        )
    }

    fun resetUpdateState() {
        _updateState.value = UpdateUserState.Idle
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
