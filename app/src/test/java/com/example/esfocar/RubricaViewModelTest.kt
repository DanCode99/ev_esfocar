package com.example.esfocar

import android.content.Context
import com.example.esfocar.data.model.CriterioRubrica
import com.example.esfocar.data.repository.RubricaRepository
import com.example.esfocar.ui.viewmodel.RubricaState
import com.example.esfocar.ui.viewmodel.RubricaViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para RubricaViewModel utilizando MockK.
 */
class RubricaViewModelTest {

    private lateinit var rubricaRepository: RubricaRepository
    private lateinit var context: Context
    private lateinit var rubricaViewModel: RubricaViewModel

    @Before
    fun setUp() {
        rubricaRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        rubricaViewModel = RubricaViewModel(rubricaRepository)
    }

    @Test
    fun `crearRubrica con titulo vacio emite estado Error`() {
        rubricaViewModel.crearRubrica(context, "", "Desc", emptyList(), 1L)

        val state = rubricaViewModel.rubricaState.value
        assertTrue(state is RubricaState.Error)
        assertEquals("Debe ingresar el título de la rúbrica.", (state as RubricaState.Error).message)
    }

    @Test
    fun `crearRubrica con suma de porcentajes distinta de 100 emite estado Error`() {
        val criteriosInvalidos = listOf(
            CriterioRubrica(nombre = "Crit 1", descripcion = "", puntajeMaximo = 10.0, porcentaje = 40.0),
            CriterioRubrica(nombre = "Crit 2", descripcion = "", puntajeMaximo = 10.0, porcentaje = 40.0)
        )

        rubricaViewModel.crearRubrica(context, "Rúbrica Test", "Desc", criteriosInvalidos, 1L)

        val state = rubricaViewModel.rubricaState.value
        assertTrue(state is RubricaState.Error)
        assertTrue((state as RubricaState.Error).message.contains("debe ser 100.0%"))
    }

    @Test
    fun `crearRubrica con criterios validos guarda exitosamente`() {
        val criteriosValidos = listOf(
            CriterioRubrica(nombre = "Crit 1", descripcion = "", puntajeMaximo = 10.0, porcentaje = 50.0),
            CriterioRubrica(nombre = "Crit 2", descripcion = "", puntajeMaximo = 10.0, porcentaje = 50.0)
        )

        every { rubricaRepository.insertRubrica(any(), any()) } returns 10L

        rubricaViewModel.crearRubrica(context, "Rúbrica Tiro 2026", "Desc", criteriosValidos, 1L)

        val state = rubricaViewModel.rubricaState.value
        assertTrue(state is RubricaState.Success)
    }
}
