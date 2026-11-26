package com.example.esfocar

import com.example.esfocar.data.model.DetalleEvaluacion
import com.example.esfocar.data.model.Evaluacion
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias para verificar el cálculo exacto de la Nota Final en escala 1.00 a 7.00.
 */
class EvaluacionFormulaTest {

    @Test
    fun `calcularNotaFinal con puntajes en cero retorna nota minima 1_00`() {
        val detalles = listOf(
            DetalleEvaluacion(criterioId = 1, puntajeObtenido = 0.0, puntajeMaximo = 10.0, porcentaje = 50.0),
            DetalleEvaluacion(criterioId = 2, puntajeObtenido = 0.0, puntajeMaximo = 10.0, porcentaje = 50.0)
        )

        val nota = Evaluacion.calcularNotaFinal(detalles)

        assertEquals(1.00, nota, 0.001)
    }

    @Test
    fun `calcularNotaFinal con puntajes maximos retorna nota maxima 7_00`() {
        val detalles = listOf(
            DetalleEvaluacion(criterioId = 1, puntajeObtenido = 10.0, puntajeMaximo = 10.0, porcentaje = 30.0),
            DetalleEvaluacion(criterioId = 2, puntajeObtenido = 10.0, puntajeMaximo = 10.0, porcentaje = 70.0)
        )

        val nota = Evaluacion.calcularNotaFinal(detalles)

        assertEquals(7.00, nota, 0.001)
    }

    @Test
    fun `calcularNotaFinal al 50 por ciento de rendimiento retorna nota 4_00`() {
        val detalles = listOf(
            DetalleEvaluacion(criterioId = 1, puntajeObtenido = 5.0, puntajeMaximo = 10.0, porcentaje = 50.0),
            DetalleEvaluacion(criterioId = 2, puntajeObtenido = 5.0, puntajeMaximo = 10.0, porcentaje = 50.0)
        )

        val nota = Evaluacion.calcularNotaFinal(detalles)

        assertEquals(4.00, nota, 0.001)
    }

    @Test
    fun `calcularNotaFinal comprueba truncamiento a 2 decimales sin redondear`() {
        // Rendimiento que genera decimales periódicos
        val detalles = listOf(
            DetalleEvaluacion(criterioId = 1, puntajeObtenido = 8.3333, puntajeMaximo = 10.0, porcentaje = 100.0)
        )

        val nota = Evaluacion.calcularNotaFinal(detalles)

        // Verificamos que se mantenga truncado en 2 decimales dentro del rango 1.00 a 7.00
        assertEquals(5.99, nota, 0.01)
    }
}
