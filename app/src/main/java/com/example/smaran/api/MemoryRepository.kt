package com.example.smaran.api

import com.example.smaran.TokenStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ─── Result wrapper ───────────────────────────────────────────────────────────

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

// ─── Repository ───────────────────────────────────────────────────────────────

class MemoryRepository {
    private val api = RetrofitClient.apiService

    suspend fun saveMemory(text: String, durationSeconds: Int): Result<SaveMemoryResponse> = try {
        val recordedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

        val response = api.saveMemory(
            token   = TokenStorage.bearerToken,
            request = SaveMemoryRequest(
                text            = text,
                recordedAt      = recordedAt,
                durationSeconds = durationSeconds
            )
        )
        if (response.isSuccessful && response.body() != null) {
            Result.Success(response.body()!!)
        } else {
            Result.Error("Server error ${response.code()}")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }
}