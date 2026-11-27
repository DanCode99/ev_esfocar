package com.example.esfocar.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
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
 * Conecta las llamadas REST con el microservicio Spring Boot (http://10.0.2.2:8080/)
 * y respalda localmente los usuarios en la base de datos SQLite.
 */
class UserRepository(
    private val dbHelper: DatabaseHelper,
    private val apiService: EsfocarApiService = RetrofitClient.apiService
) {

    /**
     * Autentica un usuario intentando primero contra Spring Boot (POST /api/usuarios/login).
     * Si no hay red o el backend no responde, realiza el login contra la base de datos SQLite local.
     */
    suspend fun authenticate(context: Context?, emailInput: String, passwordInput: String): User? = withContext(Dispatchers.IO) {
        val normalizedEmail = emailInput.trim().lowercase()
        val passwordTrimmed = passwordInput.trim()

        // 1. Intentar autenticar contra el servidor Spring Boot si hay conexión
        if (context != null && NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = apiService.login(LoginRequest(normalizedEmail, passwordTrimmed))
                if (response.isSuccessful && response.body() != null) {
                    val remoteUser = response.body()!!
                    // Respaldar usuario obtenido de Spring Boot en SQLite
                    saveUserToSQLite(remoteUser)
                    return@withContext remoteUser
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fallback: Autenticar localmente en SQLite
        return@withContext authenticateLocal(normalizedEmail, passwordTrimmed)
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
     * Registra un usuario intentando primero POST /api/usuarios en Spring Boot.
     * Si no hay red, lo inserta localmente en SQLite.
     */
    suspend fun createUser(context: Context?, user: User): Result<Long> = withContext(Dispatchers.IO) {
        val normalizedEmail = user.email.trim().lowercase()
        val normalizedRut = user.rut.trim().uppercase()
        val userToCreate = user.copy(email = normalizedEmail, rut = normalizedRut)

        // 1. Intentar registrar en servidor Spring Boot
        if (context != null && NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = apiService.createUsuario(userToCreate)
                if (response.isSuccessful && response.body() != null) {
                    val remoteUser = response.body()!!
                    saveUserToSQLite(remoteUser)
                    return@withContext Result.success(remoteUser.id)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error de validación en servidor Spring Boot (${response.code()})"
                    return@withContext Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                // Si falla la red, continuar al guardado local
            }
        }

        // 2. Fallback: Validar e insertar en SQLite local
        if (getUserByEmail(normalizedEmail) != null) {
            return@withContext Result.failure(Exception("El correo '$normalizedEmail' ya se encuentra registrado."))
        }
        if (getUserByRut(normalizedRut) != null) {
            return@withContext Result.failure(Exception("El RUT '$normalizedRut' ya se encuentra registrado."))
        }

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USER_EMAIL, normalizedEmail)
            put(DatabaseHelper.COLUMN_USER_PASSWORD, userToCreate.password.trim())
            put(DatabaseHelper.COLUMN_USER_FULL_NAME, userToCreate.fullName.trim())
            put(DatabaseHelper.COLUMN_USER_RUT, normalizedRut)
            put(DatabaseHelper.COLUMN_USER_ROLE, userToCreate.role)
            put(DatabaseHelper.COLUMN_USER_IS_FIRST_LOGIN, 1)
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

        if (context != null && NetworkUtils.isNetworkAvailable(context)) {
            try {
                apiService.updatePassword(userId, PasswordUpdateRequest(passwordTrimmed))
            } catch (e: Exception) {
                e.printStackTrace()
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

        if (context != null && NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = apiService.updateUsuario(userId, updatedUser)
                if (response.isSuccessful && response.body() != null) {
                    saveUserToSQLite(response.body()!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
        else Result.failure(Exception("No se encontró el usuario para actualizar."))
    }

    /**
     * Obtiene todos los usuarios desde Spring Boot (GET /api/usuarios) o desde SQLite.
     */
    suspend fun getAllUsers(context: Context? = null): List<User> = withContext(Dispatchers.IO) {
        if (context != null && NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = apiService.getUsuarios()
                if (response.isSuccessful && response.body() != null) {
                    val remoteList = response.body()!!
                    for (u in remoteList) {
                        saveUserToSQLite(u)
                    }
                    return@withContext remoteList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return@withContext getAllUsersLocal()
    }

    /**
     * Obtiene los usuarios con rol ESTUDIANTE desde Spring Boot (GET /api/usuarios/estudiantes) o de SQLite.
     */
    suspend fun getEstudiantes(context: Context? = null): List<User> = withContext(Dispatchers.IO) {
        if (context != null && NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = apiService.getEstudiantes()
                if (response.isSuccessful && response.body() != null) {
                    val remoteList = response.body()!!
                    for (u in remoteList) {
                        saveUserToSQLite(u)
                    }
                    return@withContext remoteList
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
        if (context != null && NetworkUtils.isNetworkAvailable(context)) {
            try {
                val response = apiService.getUsuarioById(userId)
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    saveUserToSQLite(user)
                    return@withContext user
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
     * Inserta o actualiza un registro de usuario en SQLite local.
     */
    fun saveUserToSQLite(user: User) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USER_EMAIL, user.email.trim().lowercase())
            put(DatabaseHelper.COLUMN_USER_PASSWORD, user.password)
            put(DatabaseHelper.COLUMN_USER_FULL_NAME, user.fullName)
            put(DatabaseHelper.COLUMN_USER_RUT, user.rut)
            put(DatabaseHelper.COLUMN_USER_ROLE, user.role)
            put(DatabaseHelper.COLUMN_USER_IS_FIRST_LOGIN, user.isFirstLogin)
        }

        val existing = getUserByEmail(user.email)
        if (existing != null) {
            db.update(
                DatabaseHelper.TABLE_USERS,
                values,
                "${DatabaseHelper.COLUMN_USER_ID} = ?",
                arrayOf(existing.id.toString())
            )
        } else {
            db.insert(DatabaseHelper.TABLE_USERS, null, values)
        }
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
