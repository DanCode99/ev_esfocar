package com.example.esfocar.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.esfocar.data.model.CriterioRubrica
import com.example.esfocar.data.model.Rubrica
import com.example.esfocar.data.remote.NetworkUtils
import com.example.esfocar.data.repository.RubricaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

sealed class RubricaState {
    object Idle : RubricaState()
    object Loading : RubricaState()
    data class Success(val message: String) : RubricaState()
    data class Error(val message: String) : RubricaState()
}

class RubricaViewModel(private val rubricaRepository: RubricaRepository) : ViewModel() {

    private val _rubricasList = MutableStateFlow<List<Rubrica>>(emptyList())
    val rubricasList: StateFlow<List<Rubrica>> = _rubricasList.asStateFlow()

    private val _rubricaState = MutableStateFlow<RubricaState>(RubricaState.Idle)
    val rubricaState: StateFlow<RubricaState> = _rubricaState.asStateFlow()

    fun loadRubricas() {
        _rubricasList.value = rubricaRepository.getAllRubricas()
    }

    fun crearRubrica(
        context: Context,
        titulo: String,
        descripcion: String,
        criterios: List<CriterioRubrica>,
        creadorUserId: Long
    ) {
        if (titulo.isBlank()) {
            _rubricaState.value = RubricaState.Error("Debe ingresar el título de la rúbrica.")
            return
        }
        if (criterios.isEmpty()) {
            _rubricaState.value = RubricaState.Error("Debe agregar al menos un criterio a la rúbrica.")
            return
        }

        // Validar que la suma de los porcentajes sea exactamente 100%
        val sumaPorcentajes = criterios.sumOf { it.porcentaje }
        if (abs(sumaPorcentajes - 100.0) >= 0.01) {
            _rubricaState.value = RubricaState.Error(
                String.format("La suma de porcentajes de los criterios debe ser 100.0%% (Suma actual: %.1f%%).", sumaPorcentajes)
            )
            return
        }

        // Validar que los puntajes máximos y porcentajes sean válidos
        for (c in criterios) {
            if (c.nombre.isBlank()) {
                _rubricaState.value = RubricaState.Error("Todos los criterios deben tener un nombre.")
                return
            }
            if (c.puntajeMaximo <= 0.0) {
                _rubricaState.value = RubricaState.Error("El puntaje máximo del criterio '${c.nombre}' debe ser mayor a 0.")
                return
            }
            if (c.porcentaje <= 0.0) {
                _rubricaState.value = RubricaState.Error("El porcentaje del criterio '${c.nombre}' debe ser mayor a 0%.")
                return
            }
        }

        _rubricaState.value = RubricaState.Loading

        try {
            val isOnline = NetworkUtils.isNetworkAvailable(context)
            val nuevaRubrica = Rubrica(
                titulo = titulo.trim(),
                descripcion = descripcion.trim(),
                criterios = criterios,
                creadorUserId = creadorUserId,
                sincronizado = if (isOnline) 1 else 0
            )

            val insertedId = rubricaRepository.insertRubrica(nuevaRubrica, offline = !isOnline)
            if (insertedId > 0) {
                val msg = if (isOnline) {
                    "Rúbrica '${nuevaRubrica.titulo}' creada exitosamente."
                } else {
                    "Rúbrica '${nuevaRubrica.titulo}' guardada localmente en modo Offline en SQLite. Se sincronizará al conectar a red."
                }
                _rubricaState.value = RubricaState.Success(msg)
                loadRubricas()
            } else {
                _rubricaState.value = RubricaState.Error("Error al guardar la rúbrica en la base de datos.")
            }
        } catch (e: Exception) {
            _rubricaState.value = RubricaState.Error("Error inesperado: ${e.localizedMessage}")
        }
    }

    fun resetState() {
        _rubricaState.value = RubricaState.Idle
    }

    class Factory(private val rubricaRepository: RubricaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RubricaViewModel(rubricaRepository) as T
        }
    }
}
