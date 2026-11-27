package com.example.esfocar.data.repository

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.example.esfocar.data.db.DatabaseHelper
import com.example.esfocar.data.model.DocumentItem
import com.example.esfocar.data.remote.EsfocarApiService
import com.example.esfocar.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio de Documentos para operaciones SQLite y sincronización con Spring Boot en Render.
 */
class DocumentRepository(
    private val dbHelper: DatabaseHelper,
    private val apiService: EsfocarApiService = RetrofitClient.apiService
) {

    companion object {
        private const val TAG = "DocumentRepository"
    }

    /**
     * Guarda el registro de un nuevo documento en la base de datos SQLite.
     * @param offline Si es true, establece sincronizado = 0; si es false, sincronizado = 1.
     */
    fun insertDocument(document: DocumentItem, offline: Boolean = false): Long {
        val db = dbHelper.writableDatabase
        val syncValue = if (offline) 0 else 1

        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_DOC_TITLE, document.title.trim())
            put(DatabaseHelper.COLUMN_DOC_FILE_NAME, document.fileName)
            put(DatabaseHelper.COLUMN_DOC_FILE_PATH, document.filePath)
            put(DatabaseHelper.COLUMN_DOC_FILE_TYPE, document.fileType)
            put(DatabaseHelper.COLUMN_DOC_FILE_SIZE, document.fileSize)
            put(DatabaseHelper.COLUMN_DOC_UPLOAD_DATE, document.uploadDate)
            put(DatabaseHelper.COLUMN_DOC_UPLOADED_BY, document.uploadedByUserId)
            put(DatabaseHelper.COLUMN_DOC_SINCRONIZADO, syncValue)
        }
        return db.insert(DatabaseHelper.TABLE_DOCUMENTS, null, values)
    }

    /**
     * Guarda un documento en el backend Spring Boot (Render) y en SQLite local.
     */
    suspend fun createDocumentRemote(document: DocumentItem): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createDocumento(document)
            if (response.isSuccessful && response.body() != null) {
                val remoteDoc = response.body()!!
                val localId = insertDocument(remoteDoc, offline = false)
                return@withContext Result.success(remoteDoc.id.takeIf { it > 0 } ?: localId)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Código ${response.code()}"
                Log.e(TAG, "Error HTTP al crear documento en Render [${response.code()}]: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción de red al guardar documento en Render: ${e.message}", e)
        }
        // Fallback offline
        val offlineId = insertDocument(document, offline = true)
        return@withContext Result.success(offlineId)
    }

    /**
     * Obtiene la lista completa de documentos con el nombre del usuario que lo subió.
     */
    fun getAllDocuments(): List<DocumentItem> {
        val list = mutableListOf<DocumentItem>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT d.*, u.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS u_name
            FROM ${DatabaseHelper.TABLE_DOCUMENTS} d
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u ON d.${DatabaseHelper.COLUMN_DOC_UPLOADED_BY} = u.${DatabaseHelper.COLUMN_USER_ID}
            ORDER BY d.${DatabaseHelper.COLUMN_DOC_ID} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToDocument(it))
            }
        }
        return list
    }

    /**
     * Consulta documentos desde Render (GET /api/documentos) y actualiza el respaldo local.
     */
    suspend fun fetchDocumentsFromRemote(): List<DocumentItem> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDocumentos()
            if (response.isSuccessful && response.body() != null) {
                val remoteDocs = response.body()!!
                for (doc in remoteDocs) {
                    if (getDocumentById(doc.id) == null) {
                        insertDocument(doc, offline = false)
                    }
                }
                return@withContext remoteDocs
            } else {
                Log.e(TAG, "Error HTTP al listar documentos [${response.code()}]: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción de red al listar documentos de Render: ${e.message}", e)
        }
        return@withContext getAllDocuments()
    }

    /**
     * Obtiene documentos pendientes de sincronizar (sincronizado = 0).
     */
    fun getUnsyncedDocuments(): List<DocumentItem> {
        val list = mutableListOf<DocumentItem>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT d.*, u.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS u_name
            FROM ${DatabaseHelper.TABLE_DOCUMENTS} d
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u ON d.${DatabaseHelper.COLUMN_DOC_UPLOADED_BY} = u.${DatabaseHelper.COLUMN_USER_ID}
            WHERE d.${DatabaseHelper.COLUMN_DOC_SINCRONIZADO} = 0
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToDocument(it))
            }
        }
        return list
    }

    /**
     * Marca un documento como sincronizado en SQLite (sincronizado = 1).
     */
    fun markAsSynced(documentId: Long): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_DOC_SINCRONIZADO, 1)
        }
        val rows = db.update(
            DatabaseHelper.TABLE_DOCUMENTS,
            values,
            "${DatabaseHelper.COLUMN_DOC_ID} = ?",
            arrayOf(documentId.toString())
        )
        return rows > 0
    }

    /**
     * Obtiene un documento por su ID.
     */
    fun getDocumentById(id: Long): DocumentItem? {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT d.*, u.${DatabaseHelper.COLUMN_USER_FULL_NAME} AS u_name
            FROM ${DatabaseHelper.TABLE_DOCUMENTS} d
            LEFT JOIN ${DatabaseHelper.TABLE_USERS} u ON d.${DatabaseHelper.COLUMN_DOC_UPLOADED_BY} = u.${DatabaseHelper.COLUMN_USER_ID}
            WHERE d.${DatabaseHelper.COLUMN_DOC_ID} = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                return cursorToDocument(it)
            }
        }
        return null
    }

    /**
     * Elimina el registro de un documento de SQLite por ID.
     */
    fun deleteDocument(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        val rowsDeleted = db.delete(
            DatabaseHelper.TABLE_DOCUMENTS,
            "${DatabaseHelper.COLUMN_DOC_ID} = ?",
            arrayOf(id.toString())
        )
        return rowsDeleted > 0
    }

    private fun cursorToDocument(cursor: Cursor): DocumentItem {
        val idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOC_ID)
        val titleIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOC_TITLE)
        val fileNameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOC_FILE_NAME)
        val filePathIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOC_FILE_PATH)
        val fileTypeIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOC_FILE_TYPE)
        val fileSizeIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOC_FILE_SIZE)
        val uploadDateIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOC_UPLOAD_DATE)
        val uploadedByIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOC_UPLOADED_BY)
        val userNameIndex = cursor.getColumnIndex("u_name")

        val uName = if (userNameIndex != -1 && !cursor.isNull(userNameIndex)) {
            cursor.getString(userNameIndex)
        } else {
            "Desconocido"
        }

        return DocumentItem(
            id = cursor.getLong(idIndex),
            title = cursor.getString(titleIndex),
            fileName = cursor.getString(fileNameIndex),
            filePath = cursor.getString(filePathIndex),
            fileType = cursor.getString(fileTypeIndex),
            fileSize = cursor.getLong(fileSizeIndex),
            uploadDate = cursor.getString(uploadDateIndex),
            uploadedByUserId = cursor.getLong(uploadedByIndex),
            uploadedByName = uName
        )
    }
}
