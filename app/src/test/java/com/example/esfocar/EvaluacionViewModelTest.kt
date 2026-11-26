package com.example.esfocar

import android.content.Context
import com.example.esfocar.data.model.CriterioRubrica
import com.example.esfocar.data.model.Rubrica
import com.example.esfocar.data.repository.EvaluacionRepository
import com.example.esfocar.ui.viewmodel.EvaluacionState
import com.example.esfocar.ui.viewmodel.EvaluacionViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para EvaluacionViewModel utilizando MockK.
 */
class EvaluacionViewModelTest {

    private lateinit var evaluacionRepository: EvaluacionRepository
    private lateinit var context: Context
    private lateinit var evaluacionViewModel: EvaluacionViewModel

    @Before
    fun setUp() {
        evaluacionRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        evaluacionViewModel = EvaluacionViewModel(evaluacionRepository)
    }

    @Test
    fun `setPuntajeObtenido recalcula la nota en tiempo real a escala 7_00`() {
        val testRubrica = Rubrica(
            id = 1L,
            titulo = "Rúbrica Tiro",
            descripcion = "Desc",
            criterios = listOf(
                CriterioRubrica(id = 101, rubricaId = 1L, nombre = "Precisión", descripcion = "", puntajeMaximo = 10.0, porcentaje = 50.0),
                CriterioRubrica(id = 102, rubricaId = 1L, nombre = "Rapidez", descripcion = "", puntajeMaximo = 10.0, porcentaje = 50.0)
            ),
            creadorUserId = 1L
        )

        evaluacionViewModel.selectRubrica(testRubrica)
        assertEquals(1.00, evaluacionViewModel.notaCalculada.value, 0.001)

        // Ingresar 10.0 en precisión y 10.0 en rapidez (Rendimiento 100%)
        evaluacionViewModel.setPuntajeObtenido(101, 10.0)
        evaluacionViewModel.setPuntajeObtenido(102, 10.0)

        assertEquals(7.00, evaluacionViewModel.notaCalculada.value, 0.001)
    }

    @Test
    fun `guardarEvaluacion sin estudiante seleccionado emite estado Error`() {
        val testRubrica = Rubrica(id = 1L, titulo = "Rúbrica", descripcion = "", creadorUserId = 1L)

        evaluacionViewModel.guardarEvaluacion(context, testRubrica, 0L, 1L, "Obs")

        val state = evaluacionViewModel.evalState.value
        assertTrue(state is EvaluacionState.Error)
        assertEquals("Debe seleccionar un estudiante para evaluar.", (state as EvaluacionState.Error).message)
    }

    @Test
    fun `guardarEvaluacion con datos validos realiza insercion exitosamente`() {
        val testRubrica = Rubrica(
            id = 1L,
            titulo = "Rúbrica Tiro",
            descripcion = "Desc",
            criterios = listOf(
                CriterioRubrica(id = 101, rubricaId = 1L, nombre = "Precisión", descripcion = "", puntajeMaximo = 10.0, porcentaje = 100.0)
            ),
            creadorUserId = 1L
        )

        evaluacionViewModel.selectRubrica(testRubrica)
        evaluacionViewModel.setPuntajeObtenido(101, 8.0)

        every { evaluacionRepository.insertEvaluacion(any(), any()) } returns 5L

        evaluacionViewModel.guardarEvaluacion(context, testRubrica, 4L, 1L, "Buena ejecución")

        val state = evaluacionViewModel.evalState.value
        assertTrue(state is EvaluacionState.Success)
    }
}
