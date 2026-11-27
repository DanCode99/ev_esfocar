package com.example.esfocar.data.remote

import com.example.esfocar.data.model.DocumentItem
import com.example.esfocar.data.model.Evaluacion
import com.example.esfocar.data.model.Rubrica
import com.example.esfocar.data.model.User
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * DTO para Solicitud de Login en Spring Boot (LoginRequestDto).
 */
data class LoginRequest(
    @SerializedName("identificador", alternate = ["email", "correo", "rut"]) val identificador: String,
    @SerializedName("password") val password: String
)

/**
 * DTO para Cambio de Clave en Spring Boot (PasswordUpdateDto).
 */
data class PasswordUpdateRequest(
    @SerializedName("nuevaPassword", alternate = ["newPassword", "password"]) val nuevaPassword: String
)

/**
 * Interfaz Retrofit 2 para comunicación con el microservicio Spring Boot en https://esfocar-backend.onrender.com/
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

    @DELETE("api/usuarios/{id}")
    suspend fun deleteUsuario(@Path("id") id: Long): Response<Unit>

    @POST("api/usuarios/sync")
    suspend fun syncUsuarios(@Body usuarios: List<User>): Response<List<User>>

    // === DOCUMENTOS ===
    @GET("api/documentos")
    suspend fun getDocumentos(): Response<List<DocumentItem>>

    @GET("api/documentos/usuario/{usuarioId}")
    suspend fun getDocumentosByUsuario(@Path("usuarioId") usuarioId: Long): Response<List<DocumentItem>>

    @POST("api/documentos")
    suspend fun createDocumento(@Body documento: DocumentItem): Response<DocumentItem>

    @POST("api/documentos/sync")
    suspend fun syncDocumentos(@Body documentos: List<DocumentItem>): Response<List<DocumentItem>>

    @DELETE("api/documentos/{id}")
    suspend fun deleteDocumento(@Path("id") id: Long): Response<Unit>

    // === RÚBRICAS Y CRITERIOS ===
    @GET("api/rubricas")
    suspend fun getRubricas(): Response<List<Rubrica>>

    @POST("api/rubricas")
    suspend fun createRubrica(@Body rubrica: Rubrica): Response<Rubrica>

    @POST("api/rubricas/sync")
    suspend fun syncRubricas(@Body rubricas: List<Rubrica>): Response<List<Rubrica>>

    @DELETE("api/rubricas/{id}")
    suspend fun deleteRubrica(@Path("id") id: Long): Response<Unit>

    // === EVALUACIONES Y DETALLES ===
    @GET("api/evaluaciones")
    suspend fun getEvaluaciones(): Response<List<Evaluacion>>

    @GET("api/evaluaciones/estudiante/{estudianteId}")
    suspend fun getEvaluacionesByEstudiante(@Path("estudianteId") estudianteId: Long): Response<List<Evaluacion>>

    @POST("api/evaluaciones")
    suspend fun createEvaluacion(@Body evaluacion: Evaluacion): Response<Evaluacion>

    @POST("api/evaluaciones/sync")
    suspend fun syncEvaluaciones(@Body evaluaciones: List<Evaluacion>): Response<List<Evaluacion>>
}
