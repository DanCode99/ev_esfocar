package com.example.esfocar.data.repository

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.esfocar.data.db.DatabaseHelper
import com.example.esfocar.data.model.User

/**
 * Repositorio de Usuarios para operaciones SQLite.
 */
class UserRepository(private val dbHelper: DatabaseHelper) {

    /**
     * Autentica un usuario verificando credenciales en la base de datos SQLite.
     */
    fun authenticate(usernameInput: String, passwordInput: String): User? {
        val db = dbHelper.readableDatabase
        val selection = "${DatabaseHelper.COLUMN_USER_USERNAME} = ? AND ${DatabaseHelper.COLUMN_USER_PASSWORD} = ?"
        val selectionArgs = arrayOf(usernameInput.trim(), passwordInput.trim())

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
     */
    fun createUser(user: User): Result<Long> {
        val db = dbHelper.writableDatabase

        // Verificar si el username ya existe
        if (getUserByUsername(user.username) != null) {
            return Result.failure(Exception("El nombre de usuario '${user.username}' ya está registrado."))
        }

        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USER_USERNAME, user.username.trim())
            put(DatabaseHelper.COLUMN_USER_PASSWORD, user.password.trim())
            put(DatabaseHelper.COLUMN_USER_FULL_NAME, user.fullName.trim())
            put(DatabaseHelper.COLUMN_USER_RUT, user.rut.trim())
            put(DatabaseHelper.COLUMN_USER_EMAIL, user.email.trim())
            put(DatabaseHelper.COLUMN_USER_ROLE, user.role)
            put(DatabaseHelper.COLUMN_USER_IS_FIRST_LOGIN, 1) // Forzar cambio de clave en primer ingreso
        }

        val id = db.insert(DatabaseHelper.TABLE_USERS, null, values)
        return if (id != -1L) {
            Result.success(id)
        } else {
            Result.failure(Exception("Error al registrar usuario en la base de datos."))
        }
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
     * Busca usuario por su username.
     */
    fun getUserByUsername(username: String): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            null,
            "${DatabaseHelper.COLUMN_USER_USERNAME} = ?",
            arrayOf(username.trim()),
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
        val usernameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_USERNAME)
        val passwordIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PASSWORD)
        val fullNameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_FULL_NAME)
        val rutIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_RUT)
        val emailIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_EMAIL)
        val roleIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ROLE)
        val isFirstLoginIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_IS_FIRST_LOGIN)
        val createdAtIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_CREATED_AT)

        return User(
            id = cursor.getLong(idIndex),
            username = cursor.getString(usernameIndex),
            password = cursor.getString(passwordIndex),
            fullName = cursor.getString(fullNameIndex),
            rut = cursor.getString(rutIndex),
            email = cursor.getString(emailIndex),
            role = cursor.getString(roleIndex),
            isFirstLogin = cursor.getInt(isFirstLoginIndex),
            createdAt = cursor.getString(createdAtIndex)
        )
    }
}
