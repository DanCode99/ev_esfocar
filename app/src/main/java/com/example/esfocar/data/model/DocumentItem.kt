package com.example.esfocar.data.model

/**
 * Modelo de documento almacenado en la aplicación Esfocar.
 *
 * @property id Identificador único en SQLite.
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
    val id: Long = 0,
    val title: String,
    val fileName: String,
    val filePath: String,
    val fileType: String,
    val fileSize: Long,
    val uploadDate: String,
    val uploadedByUserId: Long,
    val uploadedByName: String = ""
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
