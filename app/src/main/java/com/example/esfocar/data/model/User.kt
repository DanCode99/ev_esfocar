package com.example.esfocar.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de Usuario para la aplicación Esfocar (Fase 2 - Roles Ampliados y Backend Spring Boot).
 *
 * @property id Identificador único en SQLite / Backend.
 * @property email Correo electrónico institucional (Login).
 * @property password Contraseña del usuario.
 * @property fullName Nombre completo del usuario.
 * @property rut RUT del usuario (Único).
 * @property role Rol del usuario: "ADMIN", "PROFESOR", "DOCENTE", "INSTRUCTOR", "ESTUDIANTE".
 * @property isFirstLogin Flag de primer inicio de sesión (1: Forzar cambio de clave, 0: Normal).
 * @property createdAt Fecha de creación del registro.
 */
data class User(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("fullName", alternate = ["full_name", "nombre", "nombreCompleto"]) val fullName: String,
    @SerializedName("rut") val rut: String,
    @SerializedName("role", alternate = ["rol"]) val role: String = ROLE_PROFESOR,
    @SerializedName("isFirstLogin", alternate = ["is_first_login", "firstLogin"]) val isFirstLogin: Int = 1,
    @SerializedName("createdAt", alternate = ["created_at", "fechaCreacion"]) val createdAt: String = ""
) {
    val isAdmin: Boolean
        get() = role == ROLE_ADMIN

    val isProfesor: Boolean
        get() = role == ROLE_ADMIN || role == ROLE_PROFESOR || role == ROLE_DOCENTE || role == ROLE_INSTRUCTOR

    val isEstudiante: Boolean
        get() = role == ROLE_ESTUDIANTE

    val requiresPasswordChange: Boolean
        get() = isFirstLogin == 1

    companion object {
        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_PROFESOR = "PROFESOR"
        const val ROLE_DOCENTE = "DOCENTE"
        const val ROLE_INSTRUCTOR = "INSTRUCTOR"
        const val ROLE_ESTUDIANTE = "ESTUDIANTE"
    }
}
