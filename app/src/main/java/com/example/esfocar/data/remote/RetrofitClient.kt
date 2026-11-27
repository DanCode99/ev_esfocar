package com.example.esfocar.data.remote

import com.example.esfocar.data.model.CriterioRubrica
import com.example.esfocar.data.model.DetalleEvaluacion
import com.example.esfocar.data.model.DocumentItem
import com.example.esfocar.data.model.Evaluacion
import com.example.esfocar.data.model.Rubrica
import com.example.esfocar.data.model.User
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Singleton Retrofit 2 configurado para el backend Spring Boot en Render
 * (https://esfocar-backend.onrender.com/).
 * Cuenta con timeouts de 60 segundos para tolerar el arranque de la instancia gratuita en Render.
 */
object RetrofitClient {

    private const val BASE_URL = "https://esfocar-backend.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val userSerializer = JsonSerializer<User> { src, _, _ ->
        com.google.gson.JsonObject().apply {
            if (src.id > 0) addProperty("id", src.id)
            addProperty("correo", src.email)
            addProperty("password", src.password)
            addProperty("nombre", src.fullName)
            addProperty("rut", src.rut)
            addProperty("rol", src.role)
            addProperty("primerIngreso", src.isFirstLogin)
            if (src.createdAt.isNotBlank()) addProperty("createdAt", src.createdAt)
        }
    }

    private val documentSerializer = JsonSerializer<DocumentItem> { src, _, _ ->
        com.google.gson.JsonObject().apply {
            if (src.id > 0) addProperty("id", src.id)
            addProperty("titulo", src.title)
            addProperty("descripcion", src.fileName)
            addProperty("rutaArchivo", src.filePath)
            addProperty("tipoArchivo", src.fileType)
            addProperty("fileSize", src.fileSize)
            addProperty("fechaSubida", src.uploadDate)
            addProperty("usuarioId", src.uploadedByUserId)
            if (src.uploadedByName.isNotBlank()) addProperty("uploadedByName", src.uploadedByName)
        }
    }

    private val criterioSerializer = JsonSerializer<CriterioRubrica> { src, _, _ ->
        com.google.gson.JsonObject().apply {
            if (src.id > 0) addProperty("id", src.id)
            if (src.rubricaId > 0) addProperty("rubricaId", src.rubricaId)
            addProperty("nombre", src.nombre)
            addProperty("descripcion", src.descripcion)
            addProperty("puntajeMax", src.puntajeMaximo)
            addProperty("porcentaje", src.porcentaje)
        }
    }

    private val rubricaSerializer = JsonSerializer<Rubrica> { src, _, context ->
        com.google.gson.JsonObject().apply {
            if (src.id > 0) addProperty("id", src.id)
            addProperty("titulo", src.titulo)
            addProperty("descripcion", src.descripcion)
            addProperty("asignatura", src.asignatura)
            addProperty("escalaMaxima", src.escalaMaxima)
            addProperty("profesorId", src.creadorUserId)
            if (src.creadorNombre.isNotBlank()) addProperty("creadorNombre", src.creadorNombre)
            if (src.fechaCreacion.isNotBlank()) addProperty("fechaCreacion", src.fechaCreacion)

            val criteriosArray = com.google.gson.JsonArray()
            for (criterio in src.criterios) {
                criteriosArray.add(context.serialize(criterio, CriterioRubrica::class.java))
            }
            add("criterios", criteriosArray)
        }
    }

    private val detalleSerializer = JsonSerializer<DetalleEvaluacion> { src, _, _ ->
        com.google.gson.JsonObject().apply {
            if (src.id > 0) addProperty("id", src.id)
            if (src.evaluacionId > 0) addProperty("evaluacionId", src.evaluacionId)
            addProperty("criterioId", src.criterioId)
            if (src.criterioNombre.isNotBlank()) addProperty("nombreCriterio", src.criterioNombre)
            addProperty("puntajeObtenido", src.puntajeObtenido)
            addProperty("puntajeMaximo", src.puntajeMaximo)
            addProperty("porcentaje", src.porcentaje)
        }
    }

    private val evaluacionSerializer = JsonSerializer<Evaluacion> { src, _, context ->
        com.google.gson.JsonObject().apply {
            if (src.id > 0) addProperty("id", src.id)
            addProperty("rubricaId", src.rubricaId)
            if (src.rubricaTitulo.isNotBlank()) addProperty("tituloRubrica", src.rubricaTitulo)
            addProperty("estudianteId", src.estudianteUserId)
            if (src.estudianteNombre.isNotBlank()) addProperty("nombreEstudiante", src.estudianteNombre)
            addProperty("profesorId", src.evaluadorUserId)
            if (src.evaluadorNombre.isNotBlank()) addProperty("evaluadorNombre", src.evaluadorNombre)
            addProperty("notaFinal", src.notaFinal)
            addProperty("observacion", src.observacion)
            if (src.fechaEvaluacion.isNotBlank()) addProperty("fechaEvaluacion", src.fechaEvaluacion)

            val detallesArray = com.google.gson.JsonArray()
            for (detalle in src.detalles) {
                detallesArray.add(context.serialize(detalle, DetalleEvaluacion::class.java))
            }
            add("detalles", detallesArray)
        }
    }

    // Adaptador Gson que deserializa booleanos de Spring Boot a Int (1 / 0) para SQLite, y serializa números o booleanos
    private val gson = GsonBuilder()
        .registerTypeAdapter(User::class.java, userSerializer)
        .registerTypeAdapter(DocumentItem::class.java, documentSerializer)
        .registerTypeAdapter(Rubrica::class.java, rubricaSerializer)
        .registerTypeAdapter(CriterioRubrica::class.java, criterioSerializer)
        .registerTypeAdapter(Evaluacion::class.java, evaluacionSerializer)
        .registerTypeAdapter(DetalleEvaluacion::class.java, detalleSerializer)
        .registerTypeAdapter(Int::class.java, JsonDeserializer<Int> { json, _, _ ->
            if (json.isJsonPrimitive) {
                val prim = json.asJsonPrimitive
                if (prim.isBoolean) if (prim.asBoolean) 1 else 0
                else if (prim.isNumber) prim.asInt
                else prim.asString.toIntOrNull() ?: 0
            } else 0
        })
        .registerTypeAdapter(java.lang.Integer::class.java, JsonDeserializer<Int> { json, _, _ ->
            if (json.isJsonPrimitive) {
                val prim = json.asJsonPrimitive
                if (prim.isBoolean) if (prim.asBoolean) 1 else 0
                else if (prim.isNumber) prim.asInt
                else prim.asString.toIntOrNull() ?: 0
            } else 0
        })
        .registerTypeAdapter(Boolean::class.java, JsonDeserializer<Boolean> { json, _, _ ->
            if (json.isJsonPrimitive) {
                val prim = json.asJsonPrimitive
                if (prim.isBoolean) prim.asBoolean
                else if (prim.isNumber) prim.asInt != 0
                else prim.asString.equals("true", ignoreCase = true) || prim.asString == "1"
            } else false
        })
        .registerTypeAdapter(java.lang.Boolean::class.java, JsonDeserializer<Boolean> { json, _, _ ->
            if (json.isJsonPrimitive) {
                val prim = json.asJsonPrimitive
                if (prim.isBoolean) prim.asBoolean
                else if (prim.isNumber) prim.asInt != 0
                else prim.asString.equals("true", ignoreCase = true) || prim.asString == "1"
            } else false
        })
        .create()

    val apiService: EsfocarApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(EsfocarApiService::class.java)
    }
}
