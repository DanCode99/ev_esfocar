package com.example.esfocar.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.example.esfocar.data.db.DatabaseHelper
import com.example.esfocar.data.model.DetalleEvaluacion
import com.example.esfocar.data.model.Evaluacion

/**
 * Repositorio para gestión de Evaluaciones y Detalles en SQLite.
 */
class EvaluacionRepository(private val dbHelper: DatabaseHelper) {

    /**
     * Inserta una evaluación con sus detalles por criterio en SQLite.
     * @param offline Si es true, establece sincronizado = 0; si es false, sincronizado = 1.
     */
    fun insertEvaluacion(evaluacion: Evaluacion, offline: Boolean = false): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        var evalId = -1L
        try {
            val syncValue = if (offline) 0 else evaluacion.sincronizado

            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_EVAL_RUBRICA_ID, evaluacion.rubricaId)
                put(DatabaseHelper.COLUMN_EVAL_ESTUDIANTE_ID, evaluacion.estudianteUserId)
                put(DatabaseHelper.COLUMN_EVAL_EVALUADOR_ID, evaluacion.evaluadorUserId)
                put(DatabaseHelper.COLUMN_EVAL_NOTA_FINAL, evaluacion.notaFinal)
                put(DatabaseHelper.COLUMN_EVAL_OBSERVACION, evaluacion.observacion.trim())
                put(DatabaseHelper.COLUMN_EVAL_SINCRONIZADO, syncValue)
            }

            evalId = db.insert(DatabaseHelper.TABLE_EVALUACIONES, null, values)

            if (evalId != -1L) {
                for (detalle in evaluacion.detalles) {
                    val detValues = ContentValues().apply {
                        put(DatabaseHelper.COLUMN_DETALLE_EVALUACION_ID, evalId)
                        put(DatabaseHelper.COLUMN_DETALLE_CRITERIO_ID, detalle.criterioId)
                        put(DatabaseHelper.COLUMN_DETALLE_PUNTAJE_OBTENIDO, detalle.puntajeObtenido)
                        put(DatabaseHelper.COLUMN_DETALLE_PUNTAJE_MAXIMO, detalle.puntajeMaximo)
                        put(DatabaseHelper.COLUMN_DETALLE_PORCENTAJE, detalle.porcentaje)
                    }
                    db.insert(DatabaseHelper.TABLE_DETALLES_EVALUACION, null, detValues)
                }
                db.setTransactionSuccessful()
            }
        } finally {
            db.endTransaction()
        }
        return evalId
    }

    /**
     * Obtiene la lista completa de evaluaciones con títulos de rúbrica y nombres de estudiantes/evaluadores.
     */
    fun getAllEvaluaciones(): List<Evaluacion> {
        val list = mutableListOf<Evaluacion>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT e.*, 
                   r.${DatabaseHelper.COLUMN_RUBRICA_TITULO} AS rubrica_titulo,
                   u_est.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS estudiante_nombre,
                   u_eval.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS evaluador_nombre
            FROM ${DatabaseHelper.TABLE_EVALUACIONES} e
            LEFT JOIN ${DatabaseHelper.TABLE_RUBRICAS} r ON e.${DatabaseHelper.COLUMN_EVAL_RUBRICA_ID} = r.${DatabaseHelper.COLUMN_RUBRICA_ID}
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u_est ON e.${DatabaseHelper.COLUMN_EVAL_ESTUDIANTE_ID} = u_est.${DatabaseHelper.COLUMN_USER_ID}
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u_eval ON e.${DatabaseHelper.COLUMN_EVAL_EVALUADOR_ID} = u_eval.${DatabaseHelper.COLUMN_USER_ID}
            ORDER BY e.${DatabaseHelper.COLUMN_EVAL_ID} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                val eval = cursorToEvaluacion(it)
                val detalles = getDetallesForEvaluacion(db, eval.id)
                list.add(eval.copy(detalles = detalles))
            }
        }
        return list
    }

    /**
     * Obtiene la lista de evaluaciones asignadas a un estudiante específico.
     */
    fun getEvaluacionesByEstudiante(estudianteUserId: Long): List<Evaluacion> {
        val list = mutableListOf<Evaluacion>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT e.*, 
                   r.${DatabaseHelper.COLUMN_RUBRICA_TITULO} AS rubrica_titulo,
                   u_est.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS estudiante_nombre,
                   u_eval.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS evaluador_nombre
            FROM ${DatabaseHelper.TABLE_EVALUACIONES} e
            LEFT JOIN ${DatabaseHelper.TABLE_RUBRICAS} r ON e.${DatabaseHelper.COLUMN_EVAL_RUBRICA_ID} = r.${DatabaseHelper.COLUMN_RUBRICA_ID}
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u_est ON e.${DatabaseHelper.COLUMN_EVAL_ESTUDIANTE_ID} = u_est.${DatabaseHelper.COLUMN_USER_ID}
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u_eval ON e.${DatabaseHelper.COLUMN_EVAL_EVALUADOR_ID} = u_eval.${DatabaseHelper.COLUMN_USER_ID}
            WHERE e.${DatabaseHelper.COLUMN_EVAL_ESTUDIANTE_ID} = ?
            ORDER BY e.${DatabaseHelper.COLUMN_EVAL_ID} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(estudianteUserId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                val eval = cursorToEvaluacion(it)
                val detalles = getDetallesForEvaluacion(db, eval.id)
                list.add(eval.copy(detalles = detalles))
            }
        }
        return list
    }

    /**
     * Obtiene las evaluaciones pendientes de sincronizar (sincronizado = 0).
     */
    fun getUnsyncedEvaluaciones(): List<Evaluacion> {
        val list = mutableListOf<Evaluacion>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT e.*, 
                   r.${DatabaseHelper.COLUMN_RUBRICA_TITULO} AS rubrica_titulo,
                   u_est.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS estudiante_nombre,
                   u_eval.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS evaluador_nombre
            FROM ${DatabaseHelper.TABLE_EVALUACIONES} e
            LEFT JOIN ${DatabaseHelper.TABLE_RUBRICAS} r ON e.${DatabaseHelper.COLUMN_EVAL_RUBRICA_ID} = r.${DatabaseHelper.COLUMN_RUBRICA_ID}
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u_est ON e.${DatabaseHelper.COLUMN_EVAL_ESTUDIANTE_ID} = u_est.${DatabaseHelper.COLUMN_USER_ID}
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u_eval ON e.${DatabaseHelper.COLUMN_EVAL_EVALUADOR_ID} = u_eval.${DatabaseHelper.COLUMN_USER_ID}
            WHERE e.${DatabaseHelper.COLUMN_EVAL_SINCRONIZADO} = 0
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                val eval = cursorToEvaluacion(it)
                val detalles = getDetallesForEvaluacion(db, eval.id)
                list.add(eval.copy(detalles = detalles))
            }
        }
        return list
    }

    /**
     * Marca una evaluación como sincronizada en SQLite (sincronizado = 1).
     */
    fun markAsSynced(evaluacionId: Long): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_EVAL_SINCRONIZADO, 1)
        }
        val rows = db.update(
            DatabaseHelper.TABLE_EVALUACIONES,
            values,
            "${DatabaseHelper.COLUMN_EVAL_ID} = ?",
            arrayOf(evaluacionId.toString())
        )
        return rows > 0
    }

    private fun getDetallesForEvaluacion(db: android.database.sqlite.SQLiteDatabase, evaluacionId: Long): List<DetalleEvaluacion> {
        val list = mutableListOf<DetalleEvaluacion>()

        val query = """
            SELECT d.*, c.${DatabaseHelper.COLUMN_CRITERIO_NOMBRE} AS criterio_nombre
            FROM ${DatabaseHelper.TABLE_DETALLES_EVALUACION} d
            LEFT JOIN ${DatabaseHelper.TABLE_CRITERIOS_RUBRICA} c ON d.${DatabaseHelper.COLUMN_DETALLE_CRITERIO_ID} = c.${DatabaseHelper.COLUMN_CRITERIO_ID}
            WHERE d.${DatabaseHelper.COLUMN_DETALLE_EVALUACION_ID} = ?
            ORDER BY d.${DatabaseHelper.COLUMN_DETALLE_ID} ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(evaluacionId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                val idIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DETALLE_ID)
                val eIdIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DETALLE_EVALUACION_ID)
                val cIdIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DETALLE_CRITERIO_ID)
                val obtIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DETALLE_PUNTAJE_OBTENIDO)
                val maxIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DETALLE_PUNTAJE_MAXIMO)
                val porcIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DETALLE_PORCENTAJE)
                val nombreIndex = it.getColumnIndex("criterio_nombre")

                val cNombre = if (nombreIndex != -1 && !it.isNull(nombreIndex)) it.getString(nombreIndex) else "Criterio"

                list.add(
                    DetalleEvaluacion(
                        id = it.getLong(idIndex),
                        evaluacionId = it.getLong(eIdIndex),
                        criterioId = it.getLong(cIdIndex),
                        criterioNombre = cNombre,
                        puntajeObtenido = it.getDouble(obtIndex),
                        puntajeMaximo = it.getDouble(maxIndex),
                        porcentaje = it.getDouble(porcIndex)
                    )
                )
            }
        }
        return list
    }

    private fun cursorToEvaluacion(cursor: Cursor): Evaluacion {
        val idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVAL_ID)
        val rIdIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVAL_RUBRICA_ID)
        val estIdIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVAL_ESTUDIANTE_ID)
        val evalIdIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVAL_EVALUADOR_ID)
        val notaIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVAL_NOTA_FINAL)
        val obsIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVAL_OBSERVACION)
        val syncIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVAL_SINCRONIZADO)
        val dateIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVAL_FECHA)

        val rTituloIndex = cursor.getColumnIndex("rubrica_titulo")
        val estNombreIndex = cursor.getColumnIndex("estudiante_nombre")
        val evalNombreIndex = cursor.getColumnIndex("evaluador_nombre")

        val rTitulo = if (rTituloIndex != -1 && !cursor.isNull(rTituloIndex)) cursor.getString(rTituloIndex) else "Rúbrica"
        val estNombre = if (estNombreIndex != -1 && !cursor.isNull(estNombreIndex)) cursor.getString(estNombreIndex) else "Estudiante"
        val evalNombre = if (evalNombreIndex != -1 && !cursor.isNull(evalNombreIndex)) cursor.getString(evalNombreIndex) else "Evaluador"

        return Evaluacion(
            id = cursor.getLong(idIndex),
            rubricaId = cursor.getLong(rIdIndex),
            rubricaTitulo = rTitulo,
            estudianteUserId = cursor.getLong(estIdIndex),
            estudianteNombre = estNombre,
            evaluadorUserId = cursor.getLong(evalIdIndex),
            evaluadorNombre = evalNombre,
            notaFinal = cursor.getDouble(notaIndex),
            observacion = cursor.getString(obsIndex),
            sincronizado = cursor.getInt(syncIndex),
            fechaEvaluacion = cursor.getString(dateIndex)
        )
    }
}
