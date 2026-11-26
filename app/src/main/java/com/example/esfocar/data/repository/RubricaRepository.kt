package com.example.esfocar.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.example.esfocar.data.db.DatabaseHelper
import com.example.esfocar.data.model.CriterioRubrica
import com.example.esfocar.data.model.Rubrica

/**
 * Repositorio para gestión de Rúbricas y Criterios en SQLite.
 */
class RubricaRepository(private val dbHelper: DatabaseHelper) {

    /**
     * Inserta una rúbrica con sus criterios en SQLite.
     * @param offline Si es true, establece sincronizado = 0; si es false, sincronizado = 1.
     */
    fun insertRubrica(rubrica: Rubrica, offline: Boolean = false): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        var rubricaId = -1L
        try {
            val syncValue = if (offline) 0 else rubrica.sincronizado

            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_RUBRICA_TITULO, rubrica.titulo.trim())
                put(DatabaseHelper.COLUMN_RUBRICA_DESCRIPCION, rubrica.descripcion.trim())
                put(DatabaseHelper.COLUMN_RUBRICA_CREADOR_ID, rubrica.creadorUserId)
                put(DatabaseHelper.COLUMN_RUBRICA_SINCRONIZADO, syncValue)
            }

            rubricaId = db.insert(DatabaseHelper.TABLE_RUBRICAS, null, values)

            if (rubricaId != -1L) {
                for (criterio in rubrica.criterios) {
                    val critValues = ContentValues().apply {
                        put(DatabaseHelper.COLUMN_CRITERIO_RUBRICA_ID, rubricaId)
                        put(DatabaseHelper.COLUMN_CRITERIO_NOMBRE, criterio.nombre.trim())
                        put(DatabaseHelper.COLUMN_CRITERIO_DESCRIPCION, criterio.descripcion.trim())
                        put(DatabaseHelper.COLUMN_CRITERIO_PUNTAJE_MAXIMO, criterio.puntajeMaximo)
                        put(DatabaseHelper.COLUMN_CRITERIO_PORCENTAJE, criterio.porcentaje)
                    }
                    db.insert(DatabaseHelper.TABLE_CRITERIOS_RUBRICA, null, critValues)
                }
                db.setTransactionSuccessful()
            }
        } finally {
            db.endTransaction()
        }
        return rubricaId
    }

    /**
     * Obtiene la lista de todas las rúbricas registradas con sus respectivos criterios.
     */
    fun getAllRubricas(): List<Rubrica> {
        val list = mutableListOf<Rubrica>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT r.*, u.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS creador_nombre
            FROM ${DatabaseHelper.TABLE_RUBRICAS} r
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u ON r.${DatabaseHelper.COLUMN_RUBRICA_CREADOR_ID} = u.${DatabaseHelper.COLUMN_USER_ID}
            ORDER BY r.${DatabaseHelper.COLUMN_RUBRICA_ID} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                val rubrica = cursorToRubrica(it)
                val criterios = getCriteriosForRubrica(db, rubrica.id)
                list.add(rubrica.copy(criterios = criterios))
            }
        }
        return list
    }

    /**
     * Obtiene una rúbrica por su ID con sus criterios.
     */
    fun getRubricaById(id: Long): Rubrica? {
        val db = dbHelper.readableDatabase

        val query = """
            SELECT r.*, u.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS creador_nombre
            FROM ${DatabaseHelper.TABLE_RUBRICAS} r
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u ON r.${DatabaseHelper.COLUMN_RUBRICA_CREADOR_ID} = u.${DatabaseHelper.COLUMN_USER_ID}
            WHERE r.${DatabaseHelper.COLUMN_RUBRICA_ID} = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                val rubrica = cursorToRubrica(it)
                val criterios = getCriteriosForRubrica(db, rubrica.id)
                return rubrica.copy(criterios = criterios)
            }
        }
        return null
    }

    /**
     * Obtiene las rúbricas pendientes de sincronizar (sincronizado = 0).
     */
    fun getUnsyncedRubricas(): List<Rubrica> {
        val list = mutableListOf<Rubrica>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT r.*, u.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS creador_nombre
            FROM ${DatabaseHelper.TABLE_RUBRICAS} r
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u ON r.${DatabaseHelper.COLUMN_RUBRICA_CREADOR_ID} = u.${DatabaseHelper.COLUMN_USER_ID}
            WHERE r.${DatabaseHelper.COLUMN_RUBRICA_SINCRONIZADO} = 0
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                val rubrica = cursorToRubrica(it)
                val criterios = getCriteriosForRubrica(db, rubrica.id)
                list.add(rubrica.copy(criterios = criterios))
            }
        }
        return list
    }

    /**
     * Marca una rúbrica como sincronizada en SQLite (sincronizado = 1).
     */
    fun markAsSynced(rubricaId: Long): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_RUBRICA_SINCRONIZADO, 1)
        }
        val rows = db.update(
            DatabaseHelper.TABLE_RUBRICAS,
            values,
            "${DatabaseHelper.COLUMN_RUBRICA_ID} = ?",
            arrayOf(rubricaId.toString())
        )
        return rows > 0
    }

    private fun getCriteriosForRubrica(db: android.database.sqlite.SQLiteDatabase, rubricaId: Long): List<CriterioRubrica> {
        val list = mutableListOf<CriterioRubrica>()
        val cursor = db.query(
            DatabaseHelper.TABLE_CRITERIOS_RUBRICA,
            null,
            "${DatabaseHelper.COLUMN_CRITERIO_RUBRICA_ID} = ?",
            arrayOf(rubricaId.toString()),
            null,
            null,
            "${DatabaseHelper.COLUMN_CRITERIO_ID} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val idIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CRITERIO_ID)
                val rIdIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CRITERIO_RUBRICA_ID)
                val nombreIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CRITERIO_NOMBRE)
                val descIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CRITERIO_DESCRIPCION)
                val pMaxIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CRITERIO_PUNTAJE_MAXIMO)
                val porcIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CRITERIO_PORCENTAJE)

                list.add(
                    CriterioRubrica(
                        id = it.getLong(idIndex),
                        rubricaId = it.getLong(rIdIndex),
                        nombre = it.getString(nombreIndex),
                        descripcion = it.getString(descIndex),
                        puntajeMaximo = it.getDouble(pMaxIndex),
                        porcentaje = it.getDouble(porcIndex)
                    )
                )
            }
        }
        return list
    }

    private fun cursorToRubrica(cursor: Cursor): Rubrica {
        val idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RUBRICA_ID)
        val tituloIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RUBRICA_TITULO)
        val descIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RUBRICA_DESCRIPCION)
        val creadorIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RUBRICA_CREADOR_ID)
        val syncIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RUBRICA_SINCRONIZADO)
        val dateIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RUBRICA_CREATED_AT)
        val creadorNombreIndex = cursor.getColumnIndex("creador_nombre")

        val creadorNombre = if (creadorNombreIndex != -1 && !cursor.isNull(creadorNombreIndex)) {
            cursor.getString(creadorNombreIndex)
        } else "Profesor"

        return Rubrica(
            id = cursor.getLong(idIndex),
            titulo = cursor.getString(tituloIndex),
            descripcion = cursor.getString(descIndex),
            creadorUserId = cursor.getLong(creadorIndex),
            creadorNombre = creadorNombre,
            sincronizado = cursor.getInt(syncIndex),
            fechaCreacion = cursor.getString(dateIndex)
        )
    }
}
