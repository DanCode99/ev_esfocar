package com.example.esfocar.data.model

import com.google.gson.annotations.SerializedName
import kotlin.math.floor

/**
 * Modelo de Detalle de Evaluación para un Criterio Específico.
 *
 * @property id Identificador único en SQLite / Backend.
 * @property evaluacionId Identificador de la evaluación asociada.
 * @property criterioId Identificador del criterio evaluado.
 * @property criterioNombre Nombre del criterio evaluado.
 * @property puntajeObtenido Puntaje ingresado por el profesor (0 <= puntajeObtenido <= puntajeMaximo).
 * @property puntajeMaximo Puntaje máximo posible en este criterio.
 * @property porcentaje Peso porcentual del criterio dentro de la rúbrica (0 a 100%).
 */
data class DetalleEvaluacion(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("evaluacionId", alternate = ["evaluacion_id"]) val evaluacionId: Long = 0,
    @SerializedName("criterioId", alternate = ["criterio_id"]) val criterioId: Long,
    @SerializedName("criterioNombre", alternate = ["criterio_nombre"]) val criterioNombre: String = "",
    @SerializedName("puntajeObtenido", alternate = ["puntaje_obtenido"]) val puntajeObtenido: Double,
    @SerializedName("puntajeMaximo", alternate = ["puntaje_maximo"]) val puntajeMaximo: Double,
    @SerializedName("porcentaje") val porcentaje: Double
)

/**
 * Modelo de Evaluación de Estudiante en la plataforma Esfocar.
 *
 * @property id Identificador único en SQLite / Backend.
 * @property rubricaId ID de la rúbrica utilizada.
 * @property rubricaTitulo Título de la rúbrica aplicada.
 * @property estudianteUserId ID del usuario evaluado (Rol ESTUDIANTE).
 * @property estudianteNombre Nombre completo del estudiante evaluado.
 * @property evaluadorUserId ID del profesor o evaluador.
 * @property evaluadorNombre Nombre del profesor evaluador.
 * @property notaFinal Nota calculada en escala de 1.00 a 7.00 (truncada a 2 decimales sin redondear).
 * @property observacion Observación o retroalimentación ingresada por el evaluador.
 * @property detalles Lista de detalles/puntajes obtenidos por criterio.
 * @property fechaEvaluacion Fecha y hora del registro de la evaluación.
 * @property sincronizado 1: Sincronizado con backend Spring Boot, 0: Almacenado offline en SQLite.
 */
data class Evaluacion(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("rubricaId", alternate = ["rubrica_id"]) val rubricaId: Long,
    @SerializedName("rubricaTitulo", alternate = ["rubrica_titulo"]) val rubricaTitulo: String = "",
    @SerializedName("estudianteUserId", alternate = ["estudiante_user_id", "estudianteId"]) val estudianteUserId: Long,
    @SerializedName("estudianteNombre", alternate = ["estudiante_nombre"]) val estudianteNombre: String = "",
    @SerializedName("evaluadorUserId", alternate = ["evaluador_user_id", "evaluadorId"]) val evaluadorUserId: Long,
    @SerializedName("evaluadorNombre", alternate = ["evaluador_nombre"]) val evaluadorNombre: String = "",
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
