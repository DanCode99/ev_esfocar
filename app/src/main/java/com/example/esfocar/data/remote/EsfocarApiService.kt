package com.example.esfocar.data.remote

import com.example.esfocar.data.model.DocumentItem
import com.example.esfocar.data.model.Evaluacion
import com.example.esfocar.data.model.Rubrica
import com.example.esfocar.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interfaz Retrofit 2 para comunicación con el microservicio Spring Boot en http://10.0.2.2:8080/
 */
interface EsfocarApiService {

    // === USUARIOS ===
    @GET("api/usuarios")
    suspend fun getUsuarios(): Response<List<User>>

    @POST("api/usuarios/sync")
    suspend fun syncUsuarios(@Body usuarios: List<User>): Response<List<User>>

    // === DOCUMENTOS ===
    @GET("api/documentos")
    suspend fun getDocumentos(): Response<List<DocumentItem>>

    @POST("api/documentos/sync")
    suspend fun syncDocumentos(@Body documentos: List<DocumentItem>): Response<List<DocumentItem>>

    // === RÚBRICAS Y CRITERIOS ===
    @GET("api/rubricas")
    suspend fun getRubricas(): Response<List<Rubrica>>

    @POST("api/rubricas/sync")
    suspend fun syncRubricas(@Body rubricas: List<Rubrica>): Response<List<Rubrica>>

    // === EVALUACIONES Y DETALLES ===
    @GET("api/evaluaciones")
    suspend fun getEvaluaciones(): Response<List<Evaluacion>>

    @POST("api/evaluaciones/sync")
    suspend fun syncEvaluaciones(@Body evaluaciones: List<Evaluacion>): Response<List<Evaluacion>>
}
