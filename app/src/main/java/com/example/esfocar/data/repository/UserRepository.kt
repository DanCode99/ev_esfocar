package com.example.esfocar.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.esfocar.data.db.DatabaseHelper
import com.example.esfocar.data.model.User
import com.example.esfocar.data.remote.EsfocarApiService
import com.example.esfocar.data.remote.LoginRequest
import com.example.esfocar.data.remote.NetworkUtils
import com.example.esfocar.data.remote.PasswordUpdateRequest
import com.example.esfocar.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio de Usuarios con arquitectura Offline-First.
 * Conecta las llamadas REST con el microservicio Spring Boot en Render (https://esfocar-backend.onrender.com/)
 * y respalda localmente los usuarios en la base de datos SQLite (DatabaseHelper).
 */
class UserRepository(
    private val dbHelper: DatabaseHelper,
    private val apiService: EsfocarApiService = RetrofitClient.apiService
) {

    companion object {
        private const val TAG = "UserRepository"
    }

    /**
     * Determina si se debe intentar la llamada remota.
     * Si no se provee context, se intenta la llamada de red directamente.
     */
    private fun isOnline(context: Context?): Boolean {
        return context == null || NetworkUtils.isNetworkAvailable(context)
    }

    /**
     * Autentica un usuario intentando primero contra Spring Boot (POST /api/usuarios/login).
     * Si no hay red o el backend no responde, realiza el login contra la base de datos SQLite local.
     */
    suspend fun authenticate(context: Context?, emailInput: String, passwordInput: String): User? = withContext(Dispatchers.IO) {
        val normalizedIdentifier = emailInput.trim().lowercase()
        val passwordTrimmed = passwordInput.trim()

        // 1. Intentar autenticar contra Render si hay conexión o no se indicó context
        if (isOnline(context)) {
            try {
                val response = apiService.login(LoginRequest(identificador = normalizedIdentifier, password = passwordTrimmed))
                if (response.isSuccessful && response.body() != null) {
                    val remoteUser = response.body()!!
                    saveUserToSQLite(remoteUser)
                    return@withContext remoteUser
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    Log.e(TAG, "Error HTTP en login [${response.code()}]: $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo de conexión al autenticar en Render: ${e.message}", e)
            }
        }

        // 2. Fallback: Autenticar localmente en SQLite
        return@withContext authenticateLocal(normalizedIdentifier, passwordTrimmed)
    }

    /**
     * Autenticación local en SQLite.
     */
    fun authenticateLocal(emailInput: String, passwordInput: String): User? {
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
     * Registra un usuario intentando primero POST /api/usuarios en Spring Boot Render.
     * Si la conexión falla, lo inserta localmente en SQLite como respaldo offline.
     */
    suspend fun createUser(context: Context?, user: User): Result<Long> = withContext(Dispatchers.IO) {
        val normalizedEmail = user.email.trim().lowercase()
        val normalizedRut = user.rut.trim().uppercase()
        val userToCreate = user.copy(email = normalizedEmail, rut = normalizedRut)

        // 1. Intentar registrar en backend Spring Boot (Render)
        if (isOnline(context)) {
            try {
                val response = apiService.createUsuario(userToCreate)
                if (response.isSuccessful && response.body() != null) {
                    val remoteUser = response.body()!!
                    saveUserToSQLite(remoteUser)
                    return@withContext Result.success(remoteUser.id)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error HTTP ${response.code()}"
                    Log.e(TAG, "Error HTTP al crear usuario en Render [${response.code()}]: $errorMsg")
                    // Si el servidor respondió con error de validación o conflicto (4xx), propagar el error
                    if (response.code() in 400..499) {
                        return@withContext Result.failure(Exception(errorMsg))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo de conexión al crear usuario en Render: ${e.message}", e)
            }
        }

        // 2. Fallback: Validar e insertar en SQLite local
        if (getUserByEmail(normalizedEmail) != null) {
            return@withContext Result.failure(Exception("El correo '$normalizedEmail' ya se encuentra registrado localmente."))
        }
        if (getUserByRut(normalizedRut) != null) {
            return@withContext Result.failure(Exception("El RUT '$normalizedRut' ya se encuentra registrado localmente."))
        }

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USER_EMAIL, normalizedEmail)
            put(DatabaseHelper.COLUMN_USER_PASSWORD, userToCreate.password.trim())
            put(DatabaseHelper.COLUMN_USER_FULL_NAME, userToCreate.fullName.trim())
            put(DatabaseHelper.COLUMN_USER_RUT, normalizedRut)
            put(DatabaseHelper.COLUMN_USER_ROLE, userToCreate.role)
            put(DatabaseHelper.COLUMN_USER_IS_FIRST_LOGIN, userToCreate.isFirstLogin)
        }

        val id = db.insert(DatabaseHelper.TABLE_USERS, null, values)
        return@withContext if (id != -1L) {
            Result.success(id)
        } else {
            Result.failure(Exception("Error al registrar usuario en SQLite."))
        }
    }

    /**
     * Actualiza la contraseña en Spring Boot (PUT /api/usuarios/{id}/password) y en SQLite local.
     */
    suspend fun updatePassword(context: Context?, userId: Long, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        val passwordTrimmed = newPassword.trim()

        if (isOnline(context)) {
            try {
                val response = apiService.updatePassword(userId, PasswordUpdateRequest(nuevaPassword = passwordTrimmed))
                if (response.isSuccessful && response.body() != null) {
                    saveUserToSQLite(response.body()!!)
                } else {
                    Log.e(TAG, "Error HTTP al actualizar contraseña en Render [${response.code()}]: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo de conexión al actualizar contraseña en Render: ${e.message}", e)
            }
        }

        // Actualizar en SQLite local
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USER_PASSWORD, passwordTrimmed)
            put(DatabaseHelper.COLUMN_USER_IS_FIRST_LOGIN, 0)
        }

        val rowsUpdated = db.update(
            DatabaseHelper.TABLE_USERS,
            values,
            "${DatabaseHelper.COLUMN_USER_ID} = ?",
            arrayOf(userId.toString())
        )
        return@withContext rowsUpdated > 0
    }

    /**
     * Actualiza un usuario existente en Spring Boot (PUT /api/usuarios/{id}) y en SQLite.
     */
    suspend fun updateUser(context: Context?, userId: Long, fullName: String, rut: String, email: String, role: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val normalizedRut = rut.trim().uppercase()

        val updatedUser = User(
            id = userId,
            email = normalizedEmail,
            password = "",
            fullName = fullName.trim(),
            rut = normalizedRut,
            role = role
        )

        if (isOnline(context)) {
            try {
                val response = apiService.updateUsuario(userId, updatedUser)
                if (response.isSuccessful && response.body() != null) {
                    saveUserToSQLite(response.body()!!)
                } else {
                    Log.e(TAG, "Error HTTP al actualizar usuario [${response.code()}]: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo de conexión al actualizar usuario en Render: ${e.message}", e)
            }
        }

        // Actualizar SQLite local
        val db = dbHelper.writableDatabase
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

        return@withContext if (rowsUpdated > 0) Result.success(true)
        else Result.failure(Exception("No se encontró el usuario para actualizar en base de datos local."))
    }

    /**
     * Elimina un usuario tanto en Spring Boot Render como en la base de datos SQLite local.
     */
    suspend fun deleteUser(userId: Long, context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        if (isOnline(context)) {
            try {
                val response = apiService.deleteUsuario(userId)
                if (response.isSuccessful) {
                    Log.d(TAG, "Usuario $userId eliminado exitosamente de Render.")
                } else {
                    Log.e(TAG, "Error HTTP al eliminar usuario $userId en Render [${response.code()}]: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al eliminar usuario $userId en Render: ${e.message}", e)
            }
        }

        val db = dbHelper.writableDatabase
        val rowsDeleted = db.delete(
            DatabaseHelper.TABLE_USERS,
            "${DatabaseHelper.COLUMN_USER_ID} = ?",
            arrayOf(userId.toString())
        )
        return@withContext rowsDeleted > 0
    }

    /**
     * Obtiene todos los usuarios consultando primero a Render (GET /api/usuarios)
     * y actualizando SQLite local como respaldo offline.
     */
    suspend fun getAllUsers(context: Context? = null): List<User> = withContext(Dispatchers.IO) {
        if (isOnline(context)) {
            try {
                val response = apiService.getUsuarios()
                if (response.isSuccessful && response.body() != null) {
                    val remoteList = response.body()!!
                    for (u in remoteList) {
                        saveUserToSQLite(u)
                    }
                    return@withContext remoteList
                } else {
                    Log.e(TAG, "Error HTTP al listar usuarios [${response.code()}]: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo de conexión al obtener usuarios de Render: ${e.message}", e)
            }
        }
        return@withContext getAllUsersLocal()
    }

    /**
     * Obtiene los usuarios con rol ESTUDIANTE consultando primero a Render (GET /api/usuarios/estudiantes)
     * y actualizando SQLite local como respaldo offline.
     */
    suspend fun getEstudiantes(context: Context? = null): List<User> = withContext(Dispatchers.IO) {
        if (isOnline(context)) {
            try {
                val response = apiService.getEstudiantes()
                if (response.isSuccessful && response.body() != null) {
                    val remoteList = response.body()!!
                    for (u in remoteList) {
                        saveUserToSQLite(u)
                    }
                    return@withContext remoteList
                } else {
                    Log.e(TAG, "Error HTTP al listar estudiantes [${response.code()}]: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo de conexión al obtener estudiantes de Render: ${e.message}", e)
            }
        }
        return@withContext getAllUsersLocal().filter { it.isEstudiante }
    }

    /**
     * Obtiene la lista completa de usuarios desde SQLite local.
     */
    fun getAllUsersLocal(): List<User> {
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
     * Busca usuario por su ID en Spring Boot o SQLite.
     */
    suspend fun getUserById(userId: Long, context: Context? = null): User? = withContext(Dispatchers.IO) {
        if (isOnline(context)) {
            try {
                val response = apiService.getUsuarioById(userId)
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    saveUserToSQLite(user)
                    return@withContext user
                } else {
                    Log.e(TAG, "Error HTTP al obtener usuario por ID [${response.code()}]: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo de conexión al obtener usuario $userId de Render: ${e.message}", e)
            }
        }
        return@withContext getUserByIdLocal(userId)
    }

    fun getUserByIdLocal(userId: Long): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            null,
            "${DatabaseHelper.COLUMN_USER_ID} = ?",
            arrayOf(userId.toString()),
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
     * Inserta o actualiza un registro de usuario en SQLite local manteniendo sincronizado el ID.
     */
    fun saveUserToSQLite(user: User) {
        val db = dbHelper.writableDatabase
        val normalizedEmail = user.email.trim().lowercase()
        val values = ContentValues().apply {
            if (user.id > 0) {
                put(DatabaseHelper.COLUMN_USER_ID, user.id)
            }
            put(DatabaseHelper.COLUMN_USER_EMAIL, normalizedEmail)
            put(DatabaseHelper.COLUMN_USER_PASSWORD, user.password)
            put(DatabaseHelper.COLUMN_USER_FULL_NAME, user.fullName)
            put(DatabaseHelper.COLUMN_USER_RUT, user.rut)
            put(DatabaseHelper.COLUMN_USER_ROLE, user.role)
            put(DatabaseHelper.COLUMN_USER_IS_FIRST_LOGIN, user.isFirstLogin)
        }

        val existingByEmail = getUserByEmail(normalizedEmail)
        if (existingByEmail != null && user.id > 0 && existingByEmail.id != user.id) {
            // Eliminar fila local con ID desalineado antes de insertar con el ID real del backend
            db.delete(
                DatabaseHelper.TABLE_USERS,
                "${DatabaseHelper.COLUMN_USER_ID} = ?",
                arrayOf(existingByEmail.id.toString())
            )
        }

        db.insertWithOnConflict(
            DatabaseHelper.TABLE_USERS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun cursorToUser(cursor: Cursor): User {
        val idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)
        val emailIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_EMAIL)
        val passwordIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PASSWORD)
        val fullNameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_FULL_NAME)
        val rutIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_RUT)
        val roleIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ROLE)
        val isFirstLoginIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_IS_FIRST_LOGIN)
        val createdAtIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_CREATED_AT)

        val isFirstLoginInt = cursor.getInt(isFirstLoginIndex)

        return User(
            id = cursor.getLong(idIndex),
            email = cursor.getString(emailIndex),
            password = cursor.getString(passwordIndex),
            fullName = cursor.getString(fullNameIndex),
            rut = cursor.getString(rutIndex),
            role = cursor.getString(roleIndex),
            isFirstLogin = isFirstLoginInt,
            createdAt = cursor.getString(createdAtIndex)
        )
    }
}
