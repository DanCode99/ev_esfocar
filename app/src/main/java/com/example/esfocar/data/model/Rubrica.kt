package com.example.esfocar.data.model

import kotlin.math.abs

/**
 * Modelo de Criterio de Evaluación perteneciente a una Rúbrica.
 *
 * @property id Identificador único en SQLite / Backend.
 * @property rubricaId Identificador de la rúbrica asociada.
 * @property nombre Nombre del criterio (ej: "Presentación Personal", "Ejecución Técnica").
 * @property descripcion Explicación detallada del criterio evaluado.
 * @property puntajeMaximo Puntaje máximo a obtener (ej: 10.0, 100.0).
 * @property porcentaje Peso porcentual dentro de la rúbrica (ej: 30.0 para 30%).
 */
data class CriterioRubrica(
    val id: Long = 0,
    val rubricaId: Long = 0,
    val nombre: String,
    val descripcion: String,
    val puntajeMaximo: Double,
    val porcentaje: Double
)

/**
 * Modelo de Rúbrica de Evaluación Institucional.
 *
 * @property id Identificador único en SQLite / Backend.
 * @property titulo Título de la rúbrica (ej: "Evaluación Práctica de Tiro 2026").
 * @property descripcion Descripción general o instrucciones de la rúbrica.
 * @property criterios Lista de criterios evaluativos asociados.
 * @property creadorUserId ID del docente/profesor creador.
 * @property creadorNombre Nombre completo del creador.
 * @property sincronizado 1: Sincronizado en servidor Spring Boot, 0: Guardado offline en SQLite.
 * @property fechaCreacion Marca de tiempo de registro.
 */
data class Rubrica(
    val id: Long = 0,
    val titulo: String,
    val descripcion: String,
    val criterios: List<CriterioRubrica> = emptyList(),
    val creadorUserId: Long,
    val creadorNombre: String = "",
    val sincronizado: Int = 1,
    val fechaCreacion: String = ""
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
