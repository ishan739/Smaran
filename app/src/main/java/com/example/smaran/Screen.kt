package com.example.smaran

sealed class Screen(val route: String) {

    object Auth    : Screen("auth")
    object Record  : Screen("record")
    object Ask     : Screen("ask")
    object Profile : Screen("profile")

    object Review : Screen("review/{transcribedText}/{durationSeconds}") {
        fun createRoute(text: String, duration: Int): String {
            val encoded = java.net.URLEncoder.encode(text, "UTF-8")
            return "review/$encoded/$duration"
        }
    }
}