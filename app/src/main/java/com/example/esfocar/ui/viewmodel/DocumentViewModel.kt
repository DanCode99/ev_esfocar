package com.example.esfocar.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.esfocar.data.model.DocumentItem
import com.example.esfocar.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    data class Success(val message: String) : UploadState()
    data class Error(val message: String) : UploadState()
}

class DocumentViewModel(private val documentRepository: DocumentRepository) : ViewModel() {

    private val _documentsList = MutableStateFlow<List<DocumentItem>>(emptyList())
    val documentsList: StateFlow<List<DocumentItem>> = _documentsList.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri.asStateFlow()

    private val _selectedFileName = MutableStateFlow<String>("")
    val selectedFileName: StateFlow<String> = _selectedFileName.asStateFlow()

    private val _selectedFileSize = MutableStateFlow<Long>(0L)
    val selectedFileSize: StateFlow<Long> = _selectedFileSize.asStateFlow()

    fun loadDocuments() {
        _documentsList.value = documentRepository.getAllDocuments()
    }

    fun setSelectedFile(context: Context, uri: Uri?) {
        _selectedUri.value = uri
        if (uri != null) {
            val (name, size) = getFileNameAndSize(context, uri)
            _selectedFileName.value = name
            _selectedFileSize.value = size
        } else {
            _selectedFileName.value = ""
            _selectedFileSize.value = 0L
        }
    }

    fun uploadDocument(context: Context, titleInput: String, userId: Long): Boolean {
        val uri = _selectedUri.value
        if (uri == null) {
            _uploadState.value = UploadState.Error("Debe seleccionar un archivo PDF o Word.")
            return false
        }
        if (titleInput.isBlank()) {
            _uploadState.value = UploadState.Error("Debe ingresar un título para el documento.")
            return false
        }

        _uploadState.value = UploadState.Loading

        try {
            val originalName = _selectedFileName.value.ifBlank { "documento.pdf" }
            val fileType = getFileType(originalName)

            // Validar extensión
            if (fileType !in listOf("PDF", "DOC", "DOCX")) {
                _uploadState.value = UploadState.Error("Tipo de archivo no permitido. Solo se aceptan PDF, DOC y DOCX.")
                return false
            }

            // Guardar en el almacenamiento local interno del dispositivo
            val docDir = File(context.filesDir, "esfocar_docs")
            if (!docDir.exists()) {
                docDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val savedFile = File(docDir, "DOC_${timestamp}_$originalName")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(savedFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: run {
                _uploadState.value = UploadState.Error("No se pudo leer el archivo seleccionado.")
                return false
            }

            val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            val newDoc = DocumentItem(
                title = titleInput.trim(),
                fileName = originalName,
                filePath = savedFile.absolutePath,
                fileType = fileType,
                fileSize = savedFile.length(),
                uploadDate = currentDate,
                uploadedByUserId = userId
            )

            val insertedId = documentRepository.insertDocument(newDoc)
            if (insertedId > 0) {
                _uploadState.value = UploadState.Success("Documento '${newDoc.title}' cargado con éxito en SQLite y almacenamiento local.")
                resetSelection()
                loadDocuments()
                return true
            } else {
                _uploadState.value = UploadState.Error("Error al guardar registro en la base de datos.")
                return false
            }
        } catch (e: Exception) {
            _uploadState.value = UploadState.Error("Error al procesar el archivo: ${e.localizedMessage}")
            return false
        }
    }

    fun deleteDocument(document: DocumentItem) {
        try {
            // 1. Borrar archivo físico del dispositivo
            val file = File(document.filePath)
            if (file.exists()) {
                file.delete()
            }

            // 2. Borrar registro de la base de datos SQLite
            val deleted = documentRepository.deleteDocument(document.id)
            if (deleted) {
                loadDocuments()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openDocument(context: Context, document: DocumentItem) {
        try {
            val file = File(document.filePath)
            if (!file.exists()) {
                _uploadState.value = UploadState.Error("El archivo físico no se encuentra en el dispositivo.")
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = when (document.fileType.uppercase()) {
                "PDF" -> "application/pdf"
                "DOC" -> "application/msword"
                "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                else -> "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Abrir documento con...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            _uploadState.value = UploadState.Error("No se encontró una aplicación para abrir este archivo.")
        }
    }

    fun resetSelection() {
        _selectedUri.value = null
        _selectedFileName.value = ""
        _selectedFileSize.value = 0L
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    private fun getFileNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        var name = "archivo"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(name, size)
    }

    private fun getFileType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").uppercase()
        return when (extension) {
            "PDF" -> "PDF"
            "DOC" -> "DOC"
            "DOCX" -> "DOCX"
            else -> extension.ifBlank { "DESCONOCIDO" }
        }
    }

    class Factory(private val documentRepository: DocumentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DocumentViewModel(documentRepository) as T
        }
    }
}
