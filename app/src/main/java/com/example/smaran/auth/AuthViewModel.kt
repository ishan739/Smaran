package com.example.smaran.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smaran.TokenStorage
import com.example.smaran.api.LoginRequest
import com.example.smaran.api.RetrofitClient
import com.example.smaran.api.SignupRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _onSuccess = MutableSharedFlow<Unit>()
    val onSuccess: SharedFlow<Unit> = _onSuccess.asSharedFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.login(
                    LoginRequest(email = email.trim(), password = password)
                )
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    storeAuth(body)
                    _onSuccess.emit(Unit)
                } else {
                    _uiState.value = AuthUiState.Error(body?.message ?: "Login failed — check your credentials")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Network error — please try again")
            }
        }
    }

    fun signup(email: String, password: String, firstName: String, lastName: String) {
        if (email.isBlank() || password.isBlank() || firstName.isBlank() || lastName.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.signup(
                    SignupRequest(
                        email      = email.trim(),
                        password   = password,
                        first_name = firstName.trim(),
                        last_name  = lastName.trim()
                    )
                )
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    storeAuth(body)
                    _onSuccess.emit(Unit)
                } else {
                    _uiState.value = AuthUiState.Error(body?.message ?: "Signup failed — please try again")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Network error — please try again")
            }
        }
    }

    private fun storeAuth(body: com.example.smaran.api.AuthResponse) {
        TokenStorage.token     = body.token
        TokenStorage.userId    = body.user?.id ?: -1
        TokenStorage.email     = body.user?.email
        TokenStorage.firstName = body.user?.first_name
        TokenStorage.lastName  = body.user?.last_name
    }
}