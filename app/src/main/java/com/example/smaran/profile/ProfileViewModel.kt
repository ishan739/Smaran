package com.example.smaran.profile

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.smaran.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileSettings(
    val language:  String = "English",
    val firstName: String = "",
    val lastName:  String = "",
    val email:     String = "",
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("smaran_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        ProfileSettings(
            language  = prefs.getString("language", "English") ?: "English",
            firstName = TokenStorage.firstName ?: "",
            lastName  = TokenStorage.lastName  ?: "",
            email     = TokenStorage.email     ?: "",
        )
    )
    val settings: StateFlow<ProfileSettings> = _settings.asStateFlow()

    fun setLanguage(value: String) {
        _settings.value = _settings.value.copy(language = value)
        prefs.edit().putString("language", value).apply()
    }
}