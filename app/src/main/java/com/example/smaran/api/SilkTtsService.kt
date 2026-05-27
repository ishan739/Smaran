package com.example.smaran.api

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class SilkTtsRequest(
    val model: String,
    val text: String,
    val description: String? = null,
    val speaker: String? = null,
    val f0_up_key: Int? = null
)

interface SilkApiService {
    @POST("v1/tts")
    suspend fun synthesize(
        @Header("Authorization") token: String,
        @Body request: SilkTtsRequest
    ): Response<ResponseBody>
}

object SilkRetrofitClient {
    val service: SilkApiService = Retrofit.Builder()
        .baseUrl(Constants.SILK_BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SilkApiService::class.java)
}