package com.example.esfocar.data.model

/**
 * Modelo de Usuario para la aplicación Esfocar (Fase 2 - Roles Ampliados).
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
    val id: Long = 0,
    val email: String,
    val password: String,
    val fullName: String,
    val rut: String,
    val role: String = ROLE_PROFESOR,
    val isFirstLogin: Int = 1,
    val createdAt: String = ""
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
