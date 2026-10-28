package com.example.esfocar.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.example.esfocar.data.db.DatabaseHelper
import com.example.esfocar.data.model.DocumentItem

/**
 * Repositorio de Documentos para operaciones SQLite.
 */
class DocumentRepository(private val dbHelper: DatabaseHelper) {

    /**
     * Guarda el registro de un nuevo documento en la base de datos SQLite.
     */
    fun insertDocument(document: DocumentItem): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_DOC_TITLE, document.title.trim())
            put(DatabaseHelper.COLUMN_DOC_FILE_NAME, document.fileName)
            put(DatabaseHelper.COLUMN_DOC_FILE_PATH, document.filePath)
            put(DatabaseHelper.COLUMN_DOC_FILE_TYPE, document.fileType)
            put(DatabaseHelper.COLUMN_DOC_FILE_SIZE, document.fileSize)
            put(DatabaseHelper.COLUMN_DOC_UPLOAD_DATE, document.uploadDate)
            put(DatabaseHelper.COLUMN_DOC_UPLOADED_BY, document.uploadedByUserId)
        }
        return db.insert(DatabaseHelper.TABLE_DOCUMENTS, null, values)
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
