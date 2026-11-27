package com.example.esfocar.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de documento almacenado en la aplicación Esfocar.
 * Alineado exactamente con la entidad `Documento` de Spring Boot (`com.esfocar.backend.model.Documento`).
 *
 * @property id Identificador único en SQLite / Backend.
 * @property title Título asignado al documento ('titulo' en Spring Boot).
 * @property fileName Nombre original o descripción del archivo ('descripcion' en Spring Boot).
 * @property filePath Ruta del archivo ('rutaArchivo' en Spring Boot).
 * @property fileType Tipo/Extensión del archivo ('tipoArchivo' en Spring Boot).
 * @property fileSize Tamaño del archivo en bytes.
 * @property uploadDate Fecha de carga del documento ('fechaSubida' en Spring Boot).
 * @property uploadedByUserId ID del usuario creador ('usuarioId' en Spring Boot).
 * @property uploadedByName Nombre completo o username del usuario creador.
 */
data class DocumentItem(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("titulo", alternate = ["title"]) val title: String,
    @SerializedName("descripcion", alternate = ["description", "fileName", "file_name", "nombreArchivo"]) val fileName: String,
    @SerializedName("rutaArchivo", alternate = ["filePath", "file_path"]) val filePath: String,
    @SerializedName("tipoArchivo", alternate = ["fileType", "file_type"]) val fileType: String,
    @SerializedName("fileSize", alternate = ["file_size", "tamanoArchivo"]) val fileSize: Long = 0,
    @SerializedName("fechaSubida", alternate = ["uploadDate", "upload_date"]) val uploadDate: String,
    @SerializedName("usuarioId", alternate = ["uploadedByUserId", "uploaded_by_user_id"]) val uploadedByUserId: Long,
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
