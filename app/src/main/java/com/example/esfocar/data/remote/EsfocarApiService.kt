package com.example.esfocar.data.remote

import com.example.esfocar.data.model.DocumentItem
import com.example.esfocar.data.model.Evaluacion
import com.example.esfocar.data.model.Rubrica
import com.example.esfocar.data.model.User
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * DTO para Solicitud de Login en Spring Boot.
 */
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

/**
 * DTO para Cambio de Clave en Spring Boot.
 */
data class PasswordUpdateRequest(
    @SerializedName("newPassword", alternate = ["password", "nuevaPassword"]) val newPassword: String
)

/**
 * Interfaz Retrofit 2 para comunicación con el microservicio Spring Boot en http://10.0.2.2:8080/
 */
interface EsfocarApiService {

    // === USUARIOS ===
    @GET("api/usuarios")
    suspend fun getUsuarios(): Response<List<User>>

    @GET("api/usuarios/estudiantes")
    suspend fun getEstudiantes(): Response<List<User>>

    @GET("api/usuarios/{id}")
    suspend fun getUsuarioById(@Path("id") id: Long): Response<User>

    @POST("api/usuarios/login")
    suspend fun login(@Body request: LoginRequest): Response<User>

    @POST("api/usuarios")
    suspend fun createUsuario(@Body user: User): Response<User>

    @PUT("api/usuarios/{id}/password")
    suspend fun updatePassword(
        @Path("id") id: Long,
        @Body request: PasswordUpdateRequest
    ): Response<User>

    @PUT("api/usuarios/{id}")
    suspend fun updateUsuario(
        @Path("id") id: Long,
        @Body user: User
    ): Response<User>

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
