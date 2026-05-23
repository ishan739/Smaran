package com.example.smaran.api

// ─── Result wrapper ───────────────────────────────────────────────────────────

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}
class MemoryRepository {
    private val api = RetrofitClient.apiService

    suspend fun saveMemory(
        text: String,
    ): Result<SaveMemoryResponse> = try {
        val response = api.saveMemory(
            SaveMemoryRequest(text)
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