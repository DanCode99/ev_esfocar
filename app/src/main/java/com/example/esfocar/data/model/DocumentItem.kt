package com.example.esfocar.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de documento almacenado en la aplicación Esfocar.
 *
 * @property id Identificador único en SQLite / Backend.
 * @property title Título asignado al documento.
 * @property fileName Nombre original del archivo.
 * @property filePath Ruta absoluta del archivo guardado en el almacenamiento local del dispositivo.
 * @property fileType Tipo/Extensión del archivo (ej: "PDF", "DOCX", "DOC").
 * @property fileSize Tamaño del archivo en bytes.
 * @property uploadDate Fecha de carga del documento.
 * @property uploadedByUserId ID del usuario que subió el archivo.
 * @property uploadedByName Nombre completo o username del usuario creador.
 */
data class DocumentItem(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("title", alternate = ["titulo"]) val title: String,
    @SerializedName("fileName", alternate = ["file_name", "nombreArchivo"]) val fileName: String,
    @SerializedName("filePath", alternate = ["file_path", "rutaArchivo"]) val filePath: String,
    @SerializedName("fileType", alternate = ["file_type", "tipoArchivo"]) val fileType: String,
    @SerializedName("fileSize", alternate = ["file_size", "tamanoArchivo"]) val fileSize: Long,
    @SerializedName("uploadDate", alternate = ["upload_date", "fechaSubida"]) val uploadDate: String,
    @SerializedName("uploadedByUserId", alternate = ["uploaded_by_user_id", "usuarioId"]) val uploadedByUserId: Long,
    @SerializedName("uploadedByName", alternate = ["uploaded_by_name", "nombreUsuario"]) val uploadedByName: String = ""
) {
    val formattedSize: String
        get() {
            val kb = fileSize / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.2f MB", mb)
            } else {
                String.format("%.1f KB", kb)
            }
        }
}
