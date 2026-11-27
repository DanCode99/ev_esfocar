package com.example.esfocar.data.remote

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

    // Adaptador Gson que deserializa booleanos de Spring Boot a Int (1 / 0) para SQLite, y serializa números o booleanos
    private val gson = GsonBuilder()
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
