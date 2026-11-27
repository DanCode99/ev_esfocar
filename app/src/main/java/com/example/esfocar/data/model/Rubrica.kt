package com.example.esfocar.data.model

import com.google.gson.annotations.SerializedName
import kotlin.math.abs

/**
 * Modelo de Criterio de Evaluación perteneciente a una Rúbrica.
 * Alineado con la entidad `CriterioRubrica` de Spring Boot (`com.esfocar.backend.model.CriterioRubrica`).
 *
 * @property id Identificador único en SQLite / Backend.
 * @property rubricaId Identificador de la rúbrica asociada.
 * @property nombre Nombre del criterio ('nombre' en Spring Boot).
 * @property descripcion Explicación detallada del criterio evaluado.
 * @property puntajeMaximo Puntaje máximo a obtener ('puntajeMax' en Spring Boot).
 * @property porcentaje Peso porcentual dentro de la rúbrica ('porcentaje' en Spring Boot).
 */
data class CriterioRubrica(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("rubricaId", alternate = ["rubrica_id"]) val rubricaId: Long = 0,
    @SerializedName("nombre", alternate = ["name"]) val nombre: String,
    @SerializedName("descripcion", alternate = ["description"]) val descripcion: String = "",
    @SerializedName("puntajeMax", alternate = ["puntajeMaximo", "puntaje_maximo", "maxScore"]) val puntajeMaximo: Double,
    @SerializedName("porcentaje", alternate = ["percentage", "peso"]) val porcentaje: Double
)

/**
 * Modelo de Rúbrica de Evaluación Institucional.
 * Alineado con la entidad `Rubrica` de Spring Boot (`com.esfocar.backend.model.Rubrica`).
 *
 * @property id Identificador único en SQLite / Backend.
 * @property titulo Título de la rúbrica ('titulo' en Spring Boot).
 * @property descripcion Descripción general de la rúbrica.
 * @property asignatura Asignatura asociada ('asignatura' en Spring Boot).
 * @property escalaMaxima Escala máxima ('escalaMaxima' en Spring Boot, por defecto 7.00).
 * @property criterios Lista de criterios evaluativos asociados ('criterios' en Spring Boot).
 * @property creadorUserId ID del docente/profesor creador ('profesorId' en Spring Boot).
 * @property creadorNombre Nombre completo del creador.
 * @property sincronizado 1: Sincronizado en servidor Spring Boot, 0: Guardado offline en SQLite.
 * @property fechaCreacion Marca de tiempo de registro ('fechaCreacion' / fecha registro).
 */
data class Rubrica(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("titulo", alternate = ["title"]) val titulo: String,
    @SerializedName("descripcion", alternate = ["description"]) val descripcion: String = "",
    @SerializedName("asignatura", alternate = ["subject"]) val asignatura: String = "General",
    @SerializedName("escalaMaxima", alternate = ["escala_maxima", "maxScale"]) val escalaMaxima: Double = 7.00,
    @SerializedName("criterios", alternate = ["criteria"]) val criterios: List<CriterioRubrica> = emptyList(),
    @SerializedName("profesorId", alternate = ["creadorUserId", "creador_user_id", "creadorId"]) val creadorUserId: Long,
    @SerializedName("creadorNombre", alternate = ["creador_nombre"]) val creadorNombre: String = "",
    @SerializedName("sincronizado") val sincronizado: Int = 1,
    @SerializedName("fechaCreacion", alternate = ["fecha_creacion", "createdAt"]) val fechaCreacion: String = ""
) {
    /**
     * Suma total de los porcentajes de todos sus criterios. Debe ser exactamente 100%.
     */
    val sumaPorcentajes: Double
        get() = criterios.sumOf { it.porcentaje }

    /**
     * Valida que la rúbrica tenga criterios y que la suma de sus porcentajes sea igual a 100%.
     */
    val esValida: Boolean
        get() = criterios.isNotEmpty() && abs(sumaPorcentajes - 100.0) < 0.01
}
