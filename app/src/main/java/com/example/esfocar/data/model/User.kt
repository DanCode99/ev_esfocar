package com.example.esfocar.data.model

/**
 * Modelo de Usuario para la aplicación Esfocar.
 *
 * @property id Identificador único en SQLite.
 * @property username Nombre de usuario para autenticación (RUT o alias corporativo).
 * @property password Contraseña del usuario.
 * @property fullName Nombre completo del docente o instructor.
 * @property rut RUT del usuario.
 * @property email Correo electrónico institucional.
 * @property role Rol del usuario: "ADMIN", "DOCENTE", "INSTRUCTOR".
 * @property isFirstLogin Flag de primer inicio de sesión (1: Forzar cambio de clave, 0: Normal).
 * @property createdAt Fecha de creación del registro.
 */
data class User(
    val id: Long = 0,
    val username: String,
    val password: String,
    val fullName: String,
    val rut: String,
    val email: String,
    val role: String = ROLE_DOCENTE,
    val isFirstLogin: Int = 1,
    val createdAt: String = ""
) {
    val isAdmin: Boolean
        get() = role == ROLE_ADMIN

    val requiresPasswordChange: Boolean
        get() = isFirstLogin == 1

    companion object {
        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_DOCENTE = "DOCENTE"
        const val ROLE_INSTRUCTOR = "INSTRUCTOR"
    }
}
