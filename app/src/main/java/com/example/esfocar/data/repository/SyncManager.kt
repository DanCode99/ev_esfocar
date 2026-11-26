package com.example.esfocar.data.repository

import android.content.Context
import com.example.esfocar.data.remote.NetworkUtils
import com.example.esfocar.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor de Sincronización para Arquitectura Offline-First.
 * Envía registros pendientes con sincronizado = 0 al servidor Spring Boot (http://10.0.2.2:8080/)
 * y los marca como sincronizado = 1 al recibir respuesta exitosa (HTTP 200).
 */
class SyncManager(
    private val documentRepository: DocumentRepository,
    private val rubricaRepository: RubricaRepository,
    private val evaluacionRepository: EvaluacionRepository
) {

    /**
     * Realiza la sincronización total de Documentos, Rúbricas y Evaluaciones hacia Spring Boot.
     */
    suspend fun syncAll(context: Context): Result<String> = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            return@withContext Result.failure(Exception("Sin conexión a Internet. Operando en modo Offline."))
        }

        var docsSyncedCount = 0
        var rubricasSyncedCount = 0
        var evalsSyncedCount = 0

        try {
            val apiService = RetrofitClient.apiService

            // 1. Sincronizar Documentos pendientes (sincronizado = 0)
            val unsyncedDocs = documentRepository.getUnsyncedDocuments()
            if (unsyncedDocs.isNotEmpty()) {
                val response = apiService.syncDocumentos(unsyncedDocs)
                if (response.isSuccessful) {
                    for (doc in unsyncedDocs) {
                        documentRepository.markAsSynced(doc.id)
                        docsSyncedCount++
                    }
                }
            }

            // 2. Sincronizar Rúbricas pendientes (sincronizado = 0)
            val unsyncedRubricas = rubricaRepository.getUnsyncedRubricas()
            if (unsyncedRubricas.isNotEmpty()) {
                val response = apiService.syncRubricas(unsyncedRubricas)
                if (response.isSuccessful) {
                    for (rubrica in unsyncedRubricas) {
                        rubricaRepository.markAsSynced(rubrica.id)
                        rubricasSyncedCount++
                    }
                }
            }

            // 3. Sincronizar Evaluaciones pendientes (sincronizado = 0)
            val unsyncedEvals = evaluacionRepository.getUnsyncedEvaluaciones()
            if (unsyncedEvals.isNotEmpty()) {
                val response = apiService.syncEvaluaciones(unsyncedEvals)
                if (response.isSuccessful) {
                    for (eval in unsyncedEvals) {
                        evaluacionRepository.markAsSynced(eval.id)
                        evalsSyncedCount++
                    }
                }
            }

            val totalSynced = docsSyncedCount + rubricasSyncedCount + evalsSyncedCount
            val message = if (totalSynced > 0) {
                "Sincronización completada exitosamente: $docsSyncedCount docs, $rubricasSyncedCount rúbricas, $evalsSyncedCount evaluaciones."
            } else {
                "Sistema al día. Todos los registros locales están sincronizados con Spring Boot."
            }

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(Exception("Error al conectar con servidor Spring Boot: ${e.localizedMessage}"))
        }
    }

    /**
     * Retorna la cantidad total de registros locales pendientes de sincronizar en SQLite.
     */
    fun getPendingSyncCount(): Int {
        val pendingDocs = documentRepository.getUnsyncedDocuments().size
        val pendingRubricas = rubricaRepository.getUnsyncedRubricas().size
        val pendingEvals = evaluacionRepository.getUnsyncedEvaluaciones().size
        return pendingDocs + pendingRubricas + pendingEvals
    }
}
