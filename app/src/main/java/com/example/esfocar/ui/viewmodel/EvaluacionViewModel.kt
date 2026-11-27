package com.example.esfocar.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.esfocar.data.model.DetalleEvaluacion
import com.example.esfocar.data.model.Evaluacion
import com.example.esfocar.data.model.Rubrica
import com.example.esfocar.data.remote.NetworkUtils
import com.example.esfocar.data.remote.RetrofitClient.apiService
import com.example.esfocar.data.repository.EvaluacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EvaluacionState {
    object Idle : EvaluacionState()
    object Loading : EvaluacionState()
    data class Success(val message: String) : EvaluacionState()
    data class Error(val message: String) : EvaluacionState()
}

class EvaluacionViewModel(private val evaluacionRepository: EvaluacionRepository) : ViewModel() {

    private val _evaluacionesList = MutableStateFlow<List<Evaluacion>>(emptyList())
    val evaluacionesList: StateFlow<List<Evaluacion>> = _evaluacionesList.asStateFlow()

    private val _evalState = MutableStateFlow<EvaluacionState>(EvaluacionState.Idle)
    val evalState: StateFlow<EvaluacionState> = _evalState.asStateFlow()

    // Estado para la rúbrica seleccionada y los puntajes ingresados en tiempo real
    private val _selectedRubrica = MutableStateFlow<Rubrica?>(null)
    val selectedRubrica: StateFlow<Rubrica?> = _selectedRubrica.asStateFlow()

    private val _puntajesMap = MutableStateFlow<Map<Long, Double>>(emptyMap())
    val puntajesMap: StateFlow<Map<Long, Double>> = _puntajesMap.asStateFlow()

    // Estado reactivo del cálculo automático de la nota final (escala 1.00 a 7.00)
    private val _notaCalculada = MutableStateFlow(1.00)
    val notaCalculada: StateFlow<Double> = _notaCalculada.asStateFlow()

    private val _evaluaciones = MutableStateFlow<List<Evaluacion>>(emptyList())
    val evaluaciones: StateFlow<List<Evaluacion>> = _evaluaciones.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadEvaluaciones() {
        viewModelScope.launch {
            try {
                evaluacionRepository.fetchEvaluacionesFromRemote()
            } catch (_: Exception) {}
            _evaluacionesList.value = evaluacionRepository.getAllEvaluaciones()
        }
    }

    fun loadEvaluacionesEstudiante(estudianteUserId: Long) {
        if (estudianteUserId <= 0L) {
            _evaluacionesList.value = emptyList()
            _errorMessage.value = "ID de estudiante no válido."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Intenta actualizar desde Spring Boot y guarda en SQLite;
                // si falla o no hay red, el repositorio devuelve lo guardado en SQLite.
                val evaluaciones = evaluacionRepository.fetchEvaluacionesByEstudianteFromRemote(estudianteUserId)
                _evaluacionesList.value = evaluaciones
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar calificaciones: ${e.localizedMessage}"
                _evaluacionesList.value = evaluacionRepository.getEvaluacionesByEstudiante(estudianteUserId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectRubrica(rubrica: Rubrica?) {
        _selectedRubrica.value = rubrica
        if (rubrica != null) {
            val initialMap = rubrica.criterios.associate { it.id to 0.0 }
            _puntajesMap.value = initialMap
            recalcularNota(rubrica, initialMap)
        } else {
            _puntajesMap.value = emptyMap()
            _notaCalculada.value = 1.00
        }
    }

    fun setPuntajeObtenido(criterioId: Long, puntajeInput: Double) {
        val currentRubrica = _selectedRubrica.value ?: return
        val currentMap = _puntajesMap.value.toMutableMap()

        // Buscar el criterio para limitar al puntaje máximo
        val criterio = currentRubrica.criterios.find { it.id == criterioId }
        val maxPuntaje = criterio?.puntajeMaximo ?: Double.MAX_VALUE
        val puntajeValido = puntajeInput.coerceIn(0.0, maxPuntaje)

        currentMap[criterioId] = puntajeValido
        _puntajesMap.value = currentMap

        recalcularNota(currentRubrica, currentMap)
    }

    private fun recalcularNota(rubrica: Rubrica, map: Map<Long, Double>) {
        val detalles = rubrica.criterios.map { crit ->
            val pObtenido = map[crit.id] ?: 0.0
            DetalleEvaluacion(
                criterioId = crit.id,
                criterioNombre = crit.nombre,
                puntajeObtenido = pObtenido,
                puntajeMaximo = crit.puntajeMaximo,
                porcentaje = crit.porcentaje
            )
        }
        _notaCalculada.value = Evaluacion.calcularNotaFinal(detalles)
    }

    fun guardarEvaluacion(
        context: Context,
        rubrica: Rubrica,
        estudianteUserId: Long,
        evaluadorUserId: Long,
        observacion: String
    ) {
        if (estudianteUserId <= 0) {
            _evalState.value = EvaluacionState.Error("Debe seleccionar un estudiante para evaluar.")
            return
        }

        val map = _puntajesMap.value
        val detalles = rubrica.criterios.map { crit ->
            val pObtenido = map[crit.id] ?: 0.0
            if (pObtenido < 0.0 || pObtenido > crit.puntajeMaximo) {
                _evalState.value = EvaluacionState.Error(
                    String.format("El puntaje del criterio '%s' debe estar entre 0.0 y %.1f.", crit.nombre, crit.puntajeMaximo)
                )
                return
            }
            DetalleEvaluacion(
                criterioId = crit.id,
                criterioNombre = crit.nombre,
                puntajeObtenido = pObtenido,
                puntajeMaximo = crit.puntajeMaximo,
                porcentaje = crit.porcentaje
            )
        }

        val notaFinalCalculada = Evaluacion.calcularNotaFinal(detalles)
        _evalState.value = EvaluacionState.Loading

        viewModelScope.launch {
            try {
                val isOnline = NetworkUtils.isNetworkAvailable(context)
                val nuevaEvaluacion = Evaluacion(
                    rubricaId = rubrica.id,
                    rubricaTitulo = rubrica.titulo,
                    estudianteUserId = estudianteUserId,
                    evaluadorUserId = evaluadorUserId,
                    notaFinal = notaFinalCalculada,
                    observacion = observacion.trim(),
                    detalles = detalles,
                    sincronizado = if (isOnline) 1 else 0
                )

                val result = if (isOnline) {
                    evaluacionRepository.createEvaluacionRemote(nuevaEvaluacion)
                } else {
                    val id = evaluacionRepository.insertEvaluacion(nuevaEvaluacion, offline = true)
                    Result.success(id)
                }

                result.fold(
                    onSuccess = { insertedId ->
                        if (insertedId > 0) {
                            val msg = if (isOnline) {
                                String.format("Evaluación registrada exitosamente con Nota Final: %.2f.", notaFinalCalculada)
                            } else {
                                String.format("Evaluación con Nota Final %.2f guardada localmente en modo Offline en SQLite. Se sincronizará con Spring Boot al conectar red.", notaFinalCalculada)
                            }
                            _evalState.value = EvaluacionState.Success(msg)
                            selectRubrica(null)
                            loadEvaluaciones()
                        } else {
                            _evalState.value = EvaluacionState.Error("Error al guardar la evaluación en la base de datos.")
                        }
                    },
                    onFailure = { ex ->
                        _evalState.value = EvaluacionState.Error(ex.message ?: "Error al guardar la evaluación.")
                    }
                )
            } catch (e: Exception) {
                _evalState.value = EvaluacionState.Error("Error inesperado: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _evalState.value = EvaluacionState.Idle
    }

    class Factory(private val evaluacionRepository: EvaluacionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EvaluacionViewModel(evaluacionRepository) as T
        }
    }
}
