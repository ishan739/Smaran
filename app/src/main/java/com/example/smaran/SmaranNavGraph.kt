package com.example.smaran

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smaran.record.RecordScreen
import com.example.smaran.review.ReviewScreen

@Composable
fun SmaranNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Record.route
    ) {
        composable(Screen.Record.route) {
            RecordScreen(
                onTranscriptionReady = { text, duration ->
                    navController.navigate(Screen.Review.createRoute(text, duration))
                }
            )
        }

        composable(
            route = Screen.Review.route,
            arguments = listOf(
                navArgument("transcribedText")  { type = NavType.StringType },
                navArgument("durationSeconds")  { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val rawText  = backStackEntry.arguments?.getString("transcribedText") ?: ""
            val text     = java.net.URLDecoder.decode(rawText, "UTF-8")
            val duration = backStackEntry.arguments?.getInt("durationSeconds") ?: 0

            ReviewScreen(
                transcribedText   = text,
                durationSeconds   = duration,
                onBack            = { navController.popBackStack() },
                onSentSuccessfully = { navController.popBackStack() }
            )
        }

    }
}