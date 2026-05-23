package com.example.smaran


sealed class Screen(val route: String) {

    object Home : Screen("home")
    object Record : Screen("record")
    object Review : Screen("review/{transcribedText}/{durationSeconds}") {
        fun createRoute(text: String, duration: Int): String {
            val encoded = java.net.URLEncoder.encode(text, "UTF-8")
            return "review/$encoded/$duration"
        }
    }

    // object Memory : Screen("memory")
    // object Ask    : Screen("ask")
    // object Profile: Screen("profile")
}
