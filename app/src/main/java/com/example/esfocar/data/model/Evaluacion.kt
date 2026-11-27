package com.example.esfocar.data.model

import com.google.gson.annotations.SerializedName
import kotlin.math.floor

/**
 * Modelo de Detalle de Evaluación para un Criterio Específico.
 * Alineado con la entidad `DetalleEvaluacion` de Spring Boot (`com.esfocar.backend.model.DetalleEvaluacion`).
 *
 * @property id Identificador único en SQLite / Backend.
 * @property evaluacionId Identificador de la evaluación asociada.
 * @property criterioId Identificador del criterio evaluado ('criterioId' en Spring Boot).
 * @property criterioNombre Nombre del criterio evaluado ('nombreCriterio' en Spring Boot).
 * @property puntajeObtenido Puntaje obtenido por el estudiante ('puntajeObtenido' en Spring Boot).
 * @property puntajeMaximo Puntaje máximo del criterio ('puntajeMaximo' en Spring Boot).
 * @property porcentaje Peso porcentual del criterio ('porcentaje' en Spring Boot).
 */
data class DetalleEvaluacion(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("evaluacionId", alternate = ["evaluacion_id"]) val evaluacionId: Long = 0,
    @SerializedName("criterioId", alternate = ["criterio_id"]) val criterioId: Long,
    @SerializedName("nombreCriterio", alternate = ["criterioNombre", "criterio_nombre"]) val criterioNombre: String = "",
    @SerializedName("puntajeObtenido", alternate = ["puntaje_obtenido"]) val puntajeObtenido: Double,
    @SerializedName("puntajeMaximo", alternate = ["puntaje_maximo"]) val puntajeMaximo: Double,
    @SerializedName("porcentaje") val porcentaje: Double
)

/**
 * Modelo de Evaluación de Estudiante en la plataforma Esfocar.
 * Alineado con la entidad `Evaluacion` de Spring Boot (`com.esfocar.backend.model.Evaluacion`).
 *
 * @property id Identificador único en SQLite / Backend.
 * @property rubricaId ID de la rúbrica utilizada ('rubricaId' en Spring Boot).
 * @property rubricaTitulo Título de la rúbrica ('tituloRubrica' en Spring Boot).
 * @property estudianteUserId ID del estudiante evaluado ('estudianteId' en Spring Boot).
 * @property estudianteNombre Nombre del estudiante ('nombreEstudiante' en Spring Boot).
 * @property evaluadorUserId ID del docente/evaluador ('profesorId' en Spring Boot).
 * @property evaluadorNombre Nombre del profesor evaluador.
 * @property notaFinal Nota en escala 1.00 a 7.00 ('notaFinal' en Spring Boot).
 * @property observacion Observación o retroalimentación ('observacion' en Spring Boot).
 * @property detalles Lista de detalles/puntajes obtenidos ('detalles' en Spring Boot).
 * @property fechaEvaluacion Fecha y hora del registro ('fechaEvaluacion' en Spring Boot).
 * @property sincronizado 1: Sincronizado con backend Spring Boot, 0: Almacenado offline en SQLite.
 */
data class Evaluacion(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("rubricaId", alternate = ["rubrica_id"]) val rubricaId: Long,
    @SerializedName("tituloRubrica", alternate = ["rubricaTitulo", "rubrica_titulo"]) val rubricaTitulo: String = "",
    @SerializedName("estudianteId", alternate = ["estudianteUserId", "estudiante_user_id"]) val estudianteUserId: Long,
    @SerializedName("nombreEstudiante", alternate = ["estudianteNombre", "estudiante_nombre"]) val estudianteNombre: String = "",
    @SerializedName("profesorId", alternate = ["evaluadorUserId", "evaluador_user_id", "evaluadorId"]) val evaluadorUserId: Long,
    @SerializedName("evaluadorNombre", alternate = ["evaluador_nombre", "nombreProfesor"]) val evaluadorNombre: String = "",
    @SerializedName("notaFinal", alternate = ["nota_final", "nota"]) val notaFinal: Double,
    @SerializedName("observacion", alternate = ["comments"]) val observacion: String = "",
    @SerializedName("detalles", alternate = ["details"]) val detalles: List<DetalleEvaluacion> = emptyList(),
    @SerializedName("fechaEvaluacion", alternate = ["fecha_evaluacion", "fecha"]) val fechaEvaluacion: String = "",
    @SerializedName("sincronizado") val sincronizado: Int = 1
) {
    companion object {
        /**
         * Calcula la nota final en escala de 1.00 a 7.00 a partir de la lista de detalles obtenidos.
         * Fórmula institucional:
         * nota = 1.0 + 6.0 * suma((puntajeObtenido / puntajeMaximo) * (porcentaje / 100.0))
         * Truncada a 2 decimales sin aproximar (ej: 5.999 -> 5.99).
         */
        fun calcularNotaFinal(detalles: List<DetalleEvaluacion>): Double {
            if (detalles.isEmpty()) return 1.00

            val sumaPonderada = detalles.sumOf { det ->
                if (det.puntajeMaximo <= 0.0) 0.0
                else {
                    val proporcion = (det.puntajeObtenido / det.puntajeMaximo).coerceIn(0.0, 1.0)
                    proporcion * (det.porcentaje / 100.0)
                }
            }

            val notaBruta = 1.0 + 6.0 * sumaPonderada
            val notaTruncada = floor(notaBruta * 100.0) / 100.0
            return notaTruncada.coerceIn(1.00, 7.00)
        }
    }
}
