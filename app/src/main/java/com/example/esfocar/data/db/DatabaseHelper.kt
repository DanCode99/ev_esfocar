package com.example.esfocar.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.esfocar.data.model.User

/**
 * Gestor de Base de Datos SQLite para Esfocar.
 * Administra la creación de tablas, migraciones e inserción inicial de administradores por defecto.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "esfocar.db"
        const val DATABASE_VERSION = 2

        // Tabla Usuarios
        const val TABLE_USERS = "users"
        const val COLUMN_USER_ID = "id"
        const val COLUMN_USER_PASSWORD = "password"
        const val COLUMN_USER_FULL_NAME = "full_name"
        const val COLUMN_USER_RUT = "rut"
        const val COLUMN_USER_EMAIL = "email"
        const val COLUMN_USER_ROLE = "role"
        const val COLUMN_USER_IS_FIRST_LOGIN = "is_first_login"
        const val COLUMN_USER_CREATED_AT = "created_at"

        // Tabla Documentos
        const val TABLE_DOCUMENTS = "documents"
        const val COLUMN_DOC_ID = "id"
        const val COLUMN_DOC_TITLE = "title"
        const val COLUMN_DOC_FILE_NAME = "file_name"
        const val COLUMN_DOC_FILE_PATH = "file_path"
        const val COLUMN_DOC_FILE_TYPE = "file_type"
        const val COLUMN_DOC_FILE_SIZE = "file_size"
        const val COLUMN_DOC_UPLOAD_DATE = "upload_date"
        const val COLUMN_DOC_UPLOADED_BY = "uploaded_by_user_id"

        // Script SQL Creación Tabla Usuarios (Correo y RUT como campos ÚNICOS)
        val CREATE_TABLE_USERS = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_USER_PASSWORD TEXT NOT NULL,
                $COLUMN_USER_FULL_NAME TEXT NOT NULL,
                $COLUMN_USER_RUT TEXT UNIQUE NOT NULL,
                $COLUMN_USER_ROLE TEXT NOT NULL DEFAULT '${User.ROLE_DOCENTE}',
                $COLUMN_USER_IS_FIRST_LOGIN INTEGER NOT NULL DEFAULT 1,
                $COLUMN_USER_CREATED_AT TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
            );
        """.trimIndent()

        // Script SQL Creación Tabla Documentos
        val CREATE_TABLE_DOCUMENTS = """
            CREATE TABLE $TABLE_DOCUMENTS (
                $COLUMN_DOC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DOC_TITLE TEXT NOT NULL,
                $COLUMN_DOC_FILE_NAME TEXT NOT NULL,
                $COLUMN_DOC_FILE_PATH TEXT NOT NULL,
                $COLUMN_DOC_FILE_TYPE TEXT NOT NULL,
                $COLUMN_DOC_FILE_SIZE INTEGER NOT NULL,
                $COLUMN_DOC_UPLOAD_DATE TEXT NOT NULL,
                $COLUMN_DOC_UPLOADED_BY INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_DOC_UPLOADED_BY) REFERENCES $TABLE_USERS($COLUMN_USER_ID) ON DELETE CASCADE
            );
        """.trimIndent()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=ON;")
        db.execSQL(CREATE_TABLE_USERS)
        db.execSQL(CREATE_TABLE_DOCUMENTS)
        insertDefaultAdmins(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOCUMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    /**
     * Inserta los 3 usuarios administradores obligatorios por defecto.
     */
    private fun insertDefaultAdmins(db: SQLiteDatabase) {
        val defaultAdmins = listOf(
            ContentValues().apply {
                put(COLUMN_USER_EMAIL, "admin1@gmail.com")
                put(COLUMN_USER_PASSWORD, "admin123")
                put(COLUMN_USER_FULL_NAME, "Cmdte. Juan Pérez (Admin 1)")
                put(COLUMN_USER_RUT, "11.111.111-1")
                put(COLUMN_USER_ROLE, User.ROLE_ADMIN)
                put(COLUMN_USER_IS_FIRST_LOGIN, 0)
            },
            ContentValues().apply {
                put(COLUMN_USER_EMAIL, "admin2@gmail.com")
                put(COLUMN_USER_PASSWORD, "admin123")
                put(COLUMN_USER_FULL_NAME, "Capitán María González (Admin 2)")
                put(COLUMN_USER_RUT, "22.222.222-2")
                put(COLUMN_USER_ROLE, User.ROLE_ADMIN)
                put(COLUMN_USER_IS_FIRST_LOGIN, 0)
            },
            ContentValues().apply {
                put(COLUMN_USER_EMAIL, "admin3@gmail.com")
                put(COLUMN_USER_PASSWORD, "admin123")
                put(COLUMN_USER_FULL_NAME, "Teniente Carlos Rojas (Admin 3)")
                put(COLUMN_USER_RUT, "33.333.333-3")
                put(COLUMN_USER_ROLE, User.ROLE_ADMIN)
                put(COLUMN_USER_IS_FIRST_LOGIN, 1) // Para probar flujo de primer cambio de clave
            }
        )

        for (admin in defaultAdmins) {
            db.insert(TABLE_USERS, null, admin)
        }
    }
}
