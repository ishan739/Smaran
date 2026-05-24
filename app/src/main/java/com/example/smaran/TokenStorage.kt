package com.example.smaran

import android.content.Context
import android.content.SharedPreferences

object TokenStorage {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext
                .getSharedPreferences("smaran_auth", Context.MODE_PRIVATE)
        }
    }

    var token: String?
        get() = prefs.getString("token", null)
        set(value) { prefs.edit().putString("token", value).apply() }

    var userId: Int
        get() = prefs.getInt("user_id", -1)
        set(value) { prefs.edit().putInt("user_id", value).apply() }

    var email: String?
        get() = prefs.getString("email", null)
        set(value) { prefs.edit().putString("email", value).apply() }

    var firstName: String?
        get() = prefs.getString("first_name", null)
        set(value) { prefs.edit().putString("first_name", value).apply() }

    var lastName: String?
        get() = prefs.getString("last_name", null)
        set(value) { prefs.edit().putString("last_name", value).apply() }

    val bearerToken: String get() = "Bearer ${token ?: ""}"
    val isLoggedIn:  Boolean get() = !token.isNullOrBlank()

    val displayName: String get() = listOfNotNull(firstName, lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { email ?: "User" }
}