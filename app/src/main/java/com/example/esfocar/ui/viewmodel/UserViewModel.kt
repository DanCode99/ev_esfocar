package com.example.esfocar.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.esfocar.data.model.User
import com.example.esfocar.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun loadUsers(context: Context? = null) {
        viewModelScope.launch {
            _usersList.value = userRepository.getAllUsers(context)
        }
    }

    fun registerUser(
        tempPassword: String,
        fullName: String,
        rut: String,
        email: String,
        role: String,
        context: Context? = null
    ): Boolean {
        val emailTrimmed = email.trim()
        if (emailTrimmed.isBlank() || !emailTrimmed.contains("@") || !emailTrimmed.substringAfter("@").contains(".")) {
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
            isFirstLogin = 1
        )

        viewModelScope.launch {
            val result = userRepository.createUser(context, newUser)
            result.fold(
                onSuccess = {
                    _registerState.value = RegisterUserState.Success("Usuario '${newUser.fullName}' creado con éxito en Spring Boot y respaldado en SQLite.")
                    loadUsers(context)
                },
                onFailure = { ex ->
                    _registerState.value = RegisterUserState.Error(ex.message ?: "Error al registrar el usuario.")
                }
            )
        }
        return true
    }

    fun updateUser(
        userId: Long,
        fullName: String,
        rut: String,
        email: String,
        role: String,
        context: Context? = null
    ): Boolean {
        val emailTrimmed = email.trim()
        if (emailTrimmed.isBlank() || !emailTrimmed.contains("@") || !emailTrimmed.substringAfter("@").contains(".")) {
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

        viewModelScope.launch {
            val result = userRepository.updateUser(context, userId, fullName, rut, email, role)
            result.fold(
                onSuccess = {
                    _updateState.value = UpdateUserState.Success("Usuario actualizado con éxito.")
                    loadUsers(context)
                },
                onFailure = { ex ->
                    _updateState.value = UpdateUserState.Error(ex.message ?: "Error al actualizar.")
                }
            )
        }
        return true
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
