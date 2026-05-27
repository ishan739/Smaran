package com.example.smaran.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ─── Auth ─────────────────────────────────────────────────────────────────────

data class SignupRequest(
    val email: String,
    val password: String,
    val first_name: String,
    val last_name: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthUser(
    val id: Int,
    val email: String,
    val first_name: String,
    val last_name: String,
    val role: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: AuthUser?,
    val token: String?
)

// ─── Memory ───────────────────────────────────────────────────────────────────

data class SaveMemoryRequest(
    val text: String,
    val recordedAt: String,
    val durationSeconds: Int
)

data class SaveMemoryResponse(
    val id: String,
    val status: String
)

// ─── Ask ──────────────────────────────────────────────────────────────────────

data class AskRequest(val query: String)
data class AskResponse(
    val answer: String,
    val mood: String?,
    val voiceDescription: String?,
    val speaker: String?,
    val pitchShift: Int?
)

// ─── Interface ────────────────────────────────────────────────────────────────

interface SmaranApiService {

    @POST(Constants.ENDPOINT_SIGNUP)
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST(Constants.ENDPOINT_LOGIN)
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST(Constants.ENDPOINT_MEMORY)
    suspend fun saveMemory(
        @Header("Authorization") token: String,
        @Body request: SaveMemoryRequest
    ): Response<SaveMemoryResponse>

    @POST(Constants.ENDPOINT_QUERY)
    suspend fun askMemory(
        @Header("Authorization") token: String,
        @Body request: AskRequest
    ): Response<AskResponse>
}

// ─── Client ───────────────────────────────────────────────────────────────────

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