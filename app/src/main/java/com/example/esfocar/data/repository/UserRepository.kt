package com.example.esfocar.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.example.esfocar.data.db.DatabaseHelper
import com.example.esfocar.data.model.User

/**
 * Repositorio de Usuarios para operaciones SQLite.
 */
class UserRepository(private val dbHelper: DatabaseHelper) {

    /**
     * Autentica un usuario verificando correo y contraseña en la base de datos SQLite.
     */
    fun authenticate(emailInput: String, passwordInput: String): User? {
        val db = dbHelper.readableDatabase
        val selection = "${DatabaseHelper.COLUMN_USER_EMAIL} = ? AND ${DatabaseHelper.COLUMN_USER_PASSWORD} = ?"
        val selectionArgs = arrayOf(emailInput.trim().lowercase(), passwordInput.trim())

        val cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToUser(it)
            }
        }
        return null
    }

    /**
     * Obtiene un usuario por su ID.
     */
    fun getUserById(userId: Long): User? {
        val db = dbHelper.readableDatabase
        val selection = "${DatabaseHelper.COLUMN_USER_ID} = ?"
        val selectionArgs = arrayOf(userId.toString())

        val cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToUser(it)
            }
        }
        return null
    }

    /**
     * Registra un nuevo usuario en la base de datos (Exclusivo Administradores).
     * Valida que el Correo y el RUT sean ÚNICOS en la base de datos.
     */
    fun createUser(user: User): Result<Long> {
        val db = dbHelper.writableDatabase

        val normalizedEmail = user.email.trim().lowercase()
        val normalizedRut = user.rut.trim().uppercase()

        // 1. Validar que el Correo no esté registrado
        if (getUserByEmail(normalizedEmail) != null) {
            return Result.failure(Exception("El correo '$normalizedEmail' ya se encuentra registrado en el sistema."))
        }

        // 2. Validar que el RUT no esté registrado
        if (getUserByRut(normalizedRut) != null) {
            return Result.failure(Exception("El RUT '$normalizedRut' ya se encuentra registrado en el sistema."))
        }

        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USER_EMAIL, normalizedEmail)
            put(DatabaseHelper.COLUMN_USER_PASSWORD, user.password.trim())
            put(DatabaseHelper.COLUMN_USER_FULL_NAME, user.fullName.trim())
            put(DatabaseHelper.COLUMN_USER_RUT, normalizedRut)
            put(DatabaseHelper.COLUMN_USER_ROLE, user.role)
            put(DatabaseHelper.COLUMN_USER_IS_FIRST_LOGIN, 1) // Forzar cambio de clave en primer ingreso
        }

        val id = db.insert(DatabaseHelper.TABLE_USERS, null, values)
        return if (id != -1L) {
            Result.success(id)
        } else {
            Result.failure(Exception("Error al registrar usuario en la base de datos SQLite."))
        }
    }

    /**
     * Actualiza los datos de un usuario existente.
     * Valida que el nuevo RUT o Correo no pertenezcan a otra cuenta.
     */
    fun updateUser(userId: Long, fullName: String, rut: String, email: String, role: String): Result<Boolean> {
        val db = dbHelper.writableDatabase
        val normalizedEmail = email.trim().lowercase()
        val normalizedRut = rut.trim().uppercase()

        val existingEmailUser = getUserByEmail(normalizedEmail)
        if (existingEmailUser != null && existingEmailUser.id != userId) {
            return Result.failure(Exception("El correo '$normalizedEmail' ya está en uso por otro usuario."))
        }

        val existingRutUser = getUserByRut(normalizedRut)
        if (existingRutUser != null && existingRutUser.id != userId) {
            return Result.failure(Exception("El RUT '$normalizedRut' ya está en uso por otro usuario."))
        }

        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USER_EMAIL, normalizedEmail)
            put(DatabaseHelper.COLUMN_USER_FULL_NAME, fullName.trim())
            put(DatabaseHelper.COLUMN_USER_RUT, normalizedRut)
            put(DatabaseHelper.COLUMN_USER_ROLE, role)
        }

        val rowsUpdated = db.update(
            DatabaseHelper.TABLE_USERS,
            values,
            "${DatabaseHelper.COLUMN_USER_ID} = ?",
            arrayOf(userId.toString())
        )

        return if (rowsUpdated > 0) Result.success(true)
        else Result.failure(Exception("No se encontró el usuario para actualizar."))
    }

    /**
     * Actualiza la contraseña del usuario y marca is_first_login = 0.
     */
    fun updatePassword(userId: Long, newPassword: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USER_PASSWORD, newPassword.trim())
            put(DatabaseHelper.COLUMN_USER_IS_FIRST_LOGIN, 0)
        }

        val rowsUpdated = db.update(
            DatabaseHelper.TABLE_USERS,
            values,
            "${DatabaseHelper.COLUMN_USER_ID} = ?",
            arrayOf(userId.toString())
        )
        return rowsUpdated > 0
    }

    /**
     * Busca usuario por su correo electrónico.
     */
    fun getUserByEmail(email: String): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            null,
            "${DatabaseHelper.COLUMN_USER_EMAIL} = ?",
            arrayOf(email.trim().lowercase()),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return cursorToUser(it)
            }
        }
        return null
    }

    /**
     * Busca usuario por su RUT.
     */
    fun getUserByRut(rut: String): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            null,
            "${DatabaseHelper.COLUMN_USER_RUT} = ?",
            arrayOf(rut.trim().uppercase()),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return cursorToUser(it)
            }
        }
        return null
    }

    /**
     * Obtiene la lista completa de usuarios registrados.
     */
    fun getAllUsers(): List<User> {
        val usersList = mutableListOf<User>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            null,
            null,
            null,
            null,
            null,
            "${DatabaseHelper.COLUMN_USER_FULL_NAME} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                usersList.add(cursorToUser(it))
            }
        }
        return usersList
    }

    /**
     * Convierte una fila de Cursor a un objeto User.
     */
    private fun cursorToUser(cursor: Cursor): User {
        val idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)
        val emailIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_EMAIL)
        val passwordIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PASSWORD)
        val fullNameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_FULL_NAME)
        val rutIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_RUT)
        val roleIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ROLE)
        val isFirstLoginIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_IS_FIRST_LOGIN)
        val createdAtIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_CREATED_AT)

        return User(
            id = cursor.getLong(idIndex),
            email = cursor.getString(emailIndex),
            password = cursor.getString(passwordIndex),
            fullName = cursor.getString(fullNameIndex),
            rut = cursor.getString(rutIndex),
            role = cursor.getString(roleIndex),
            isFirstLogin = cursor.getInt(isFirstLoginIndex),
            createdAt = cursor.getString(createdAtIndex)
        )
    }
}
