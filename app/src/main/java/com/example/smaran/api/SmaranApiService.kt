package com.example.smaran.api

import com.example.smaran.api.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ─── Request / Response ───────────────────────────────────────────────────────

data class SaveMemoryRequest(
    val text: String,
)

data class SaveMemoryResponse(
    val id: String,
    val status: String,
)

data class AskRequest(val question: String)
data class AskResponse(val answer: String)

// ─── Interface ────────────────────────────────────────────────────────────────

interface SmaranApiService {
    @POST(Constants.ENDPOINT_MEMORY)
    suspend fun saveMemory(@Body request: SaveMemoryRequest): Response<SaveMemoryResponse>

    @POST(Constants.ENDPOINT_QUERY)
    suspend fun askMemory(@Body request: AskRequest): Response<AskResponse>
}

// ─── Client singleton ─────────────────────────────────────────────────────────

object RetrofitClient {
    val apiService: SmaranApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SmaranApiService::class.java)
    }
}