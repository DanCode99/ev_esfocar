package com.example.esfocar.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.esfocar.data.model.User

/**
 * Gestor de Base de Datos SQLite para Esfocar (Fase 2 - Offline First & Spring Boot Sync).
 * Administra usuarios, documentos, rúbricas, criterios, evaluaciones y banderas de sincronización.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "esfocar.db"
        const val DATABASE_VERSION = 4

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
        const val COLUMN_DOC_SINCRONIZADO = "sincronizado"

        // Tabla Rúbricas
        const val TABLE_RUBRICAS = "rubricas"
        const val COLUMN_RUBRICA_ID = "id"
        const val COLUMN_RUBRICA_TITULO = "titulo"
        const val COLUMN_RUBRICA_DESCRIPCION = "descripcion"
        const val COLUMN_RUBRICA_CREADOR_ID = "creador_user_id"
        const val COLUMN_RUBRICA_SINCRONIZADO = "sincronizado"
        const val COLUMN_RUBRICA_CREATED_AT = "created_at"

        // Tabla Criterios de Rúbrica
        const val TABLE_CRITERIOS_RUBRICA = "criterios_rubrica"
        const val COLUMN_CRITERIO_ID = "id"
        const val COLUMN_CRITERIO_RUBRICA_ID = "rubrica_id"
        const val COLUMN_CRITERIO_NOMBRE = "nombre"
        const val COLUMN_CRITERIO_DESCRIPCION = "descripcion"
        const val COLUMN_CRITERIO_PUNTAJE_MAXIMO = "puntaje_maximo"
        const val COLUMN_CRITERIO_PORCENTAJE = "porcentaje"

        // Tabla Evaluaciones
        const val TABLE_EVALUACIONES = "evaluaciones"
        const val COLUMN_EVAL_ID = "id"
        const val COLUMN_EVAL_RUBRICA_ID = "rubrica_id"
        const val COLUMN_EVAL_ESTUDIANTE_ID = "estudiante_user_id"
        const val COLUMN_EVAL_EVALUADOR_ID = "evaluador_user_id"
        const val COLUMN_EVAL_NOTA_FINAL = "nota_final"
        const val COLUMN_EVAL_OBSERVACION = "observacion"
        const val COLUMN_EVAL_SINCRONIZADO = "sincronizado"
        const val COLUMN_EVAL_FECHA = "fecha_evaluacion"

        // Tabla Detalles de Evaluación
        const val TABLE_DETALLES_EVALUACION = "detalles_evaluacion"
        const val COLUMN_DETALLE_ID = "id"
        const val COLUMN_DETALLE_EVALUACION_ID = "evaluacion_id"
        const val COLUMN_DETALLE_CRITERIO_ID = "criterio_id"
        const val COLUMN_DETALLE_PUNTAJE_OBTENIDO = "puntaje_obtenido"
        const val COLUMN_DETALLE_PUNTAJE_MAXIMO = "puntaje_maximo"
        const val COLUMN_DETALLE_PORCENTAJE = "porcentaje"

        // DDL Tabla Usuarios
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

        // DDL Tabla Documentos (con campo sincronizado)
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
                $COLUMN_DOC_SINCRONIZADO INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY ($COLUMN_DOC_UPLOADED_BY) REFERENCES $TABLE_USERS($COLUMN_USER_ID) ON DELETE CASCADE
            );
        """.trimIndent()

        // DDL Tabla Rúbricas
        val CREATE_TABLE_RUBRICAS = """
            CREATE TABLE $TABLE_RUBRICAS (
                $COLUMN_RUBRICA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_RUBRICA_TITULO TEXT NOT NULL,
                $COLUMN_RUBRICA_DESCRIPCION TEXT NOT NULL,
                $COLUMN_RUBRICA_CREADOR_ID INTEGER NOT NULL,
                $COLUMN_RUBRICA_SINCRONIZADO INTEGER NOT NULL DEFAULT 1,
                $COLUMN_RUBRICA_CREATED_AT TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                FOREIGN KEY ($COLUMN_RUBRICA_CREADOR_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID) ON DELETE CASCADE
            );
        """.trimIndent()

        // DDL Tabla Criterios de Rúbrica
        val CREATE_TABLE_CRITERIOS_RUBRICA = """
            CREATE TABLE $TABLE_CRITERIOS_RUBRICA (
                $COLUMN_CRITERIO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CRITERIO_RUBRICA_ID INTEGER NOT NULL,
                $COLUMN_CRITERIO_NOMBRE TEXT NOT NULL,
                $COLUMN_CRITERIO_DESCRIPCION TEXT NOT NULL,
                $COLUMN_CRITERIO_PUNTAJE_MAXIMO REAL NOT NULL,
                $COLUMN_CRITERIO_PORCENTAJE REAL NOT NULL,
                FOREIGN KEY ($COLUMN_CRITERIO_RUBRICA_ID) REFERENCES $TABLE_RUBRICAS($COLUMN_RUBRICA_ID) ON DELETE CASCADE
            );
        """.trimIndent()

        // DDL Tabla Evaluaciones
        val CREATE_TABLE_EVALUACIONES = """
            CREATE TABLE $TABLE_EVALUACIONES (
                $COLUMN_EVAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EVAL_RUBRICA_ID INTEGER NOT NULL,
                $COLUMN_EVAL_ESTUDIANTE_ID INTEGER NOT NULL,
                $COLUMN_EVAL_EVALUADOR_ID INTEGER NOT NULL,
                $COLUMN_EVAL_NOTA_FINAL REAL NOT NULL,
                $COLUMN_EVAL_OBSERVACION TEXT NOT NULL DEFAULT '',
                $COLUMN_EVAL_SINCRONIZADO INTEGER NOT NULL DEFAULT 1,
                $COLUMN_EVAL_FECHA TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                FOREIGN KEY ($COLUMN_EVAL_RUBRICA_ID) REFERENCES $TABLE_RUBRICAS($COLUMN_RUBRICA_ID) ON DELETE CASCADE,
                FOREIGN KEY ($COLUMN_EVAL_ESTUDIANTE_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID) ON DELETE CASCADE,
                FOREIGN KEY ($COLUMN_EVAL_EVALUADOR_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID) ON DELETE CASCADE
            );
        """.trimIndent()

        // DDL Tabla Detalles de Evaluación
        val CREATE_TABLE_DETALLES_EVALUACION = """
            CREATE TABLE $TABLE_DETALLES_EVALUACION (
                $COLUMN_DETALLE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DETALLE_EVALUACION_ID INTEGER NOT NULL,
                $COLUMN_DETALLE_CRITERIO_ID INTEGER NOT NULL,
                $COLUMN_DETALLE_PUNTAJE_OBTENIDO REAL NOT NULL,
                $COLUMN_DETALLE_PUNTAJE_MAXIMO REAL NOT NULL,
                $COLUMN_DETALLE_PORCENTAJE REAL NOT NULL,
                FOREIGN KEY ($COLUMN_DETALLE_EVALUACION_ID) REFERENCES $TABLE_EVALUACIONES($COLUMN_EVAL_ID) ON DELETE CASCADE,
                FOREIGN KEY ($COLUMN_DETALLE_CRITERIO_ID) REFERENCES $TABLE_CRITERIOS_RUBRICA($COLUMN_CRITERIO_ID) ON DELETE CASCADE
            );
        """.trimIndent()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=ON;")
        db.execSQL(CREATE_TABLE_USERS)
        db.execSQL(CREATE_TABLE_DOCUMENTS)
        db.execSQL(CREATE_TABLE_RUBRICAS)
        db.execSQL(CREATE_TABLE_CRITERIOS_RUBRICA)
        db.execSQL(CREATE_TABLE_EVALUACIONES)
        db.execSQL(CREATE_TABLE_DETALLES_EVALUACION)
        insertDefaultUsers(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DETALLES_EVALUACION")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVALUACIONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CRITERIOS_RUBRICA")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RUBRICAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOCUMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    /**
     * Inserta los usuarios por defecto: Administradores, Profesor y Estudiante de prueba.
     */
    private fun insertDefaultUsers(db: SQLiteDatabase) {
        val defaultUsers = listOf(
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
                put(COLUMN_USER_IS_FIRST_LOGIN, 1)
            },
            ContentValues().apply {
                put(COLUMN_USER_EMAIL, "profesor1@gmail.com")
                put(COLUMN_USER_PASSWORD, "admin123")
                put(COLUMN_USER_FULL_NAME, "Profesor Mayor Luis Tapia")
                put(COLUMN_USER_RUT, "55.555.555-5")
                put(COLUMN_USER_ROLE, User.ROLE_PROFESOR)
                put(COLUMN_USER_IS_FIRST_LOGIN, 0)
            },
            ContentValues().apply {
                put(COLUMN_USER_EMAIL, "estudiante1@gmail.com")
                put(COLUMN_USER_PASSWORD, "admin123")
                put(COLUMN_USER_FULL_NAME, "Carabinero Alumno Pedro Soto")
                put(COLUMN_USER_RUT, "44.444.444-4")
                put(COLUMN_USER_ROLE, User.ROLE_ESTUDIANTE)
                put(COLUMN_USER_IS_FIRST_LOGIN, 0)
            }
        )

        for (user in defaultUsers) {
            db.insert(TABLE_USERS, null, user)
        }
    }
}
