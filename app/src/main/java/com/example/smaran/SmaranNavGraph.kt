package com.example.smaran

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smaran.ask.AskScreen
import com.example.smaran.auth.AuthScreen
import com.example.smaran.profile.ProfileScreen
import com.example.smaran.record.RecordScreen
import com.example.smaran.review.ReviewScreen
import com.example.smaran.ui.theme.SmaranColors
import com.example.smaran.ui.theme.SmaranType

private data class NavTab(val screen: Screen, val icon: String, val label: String)

private val tabs = listOf(
    NavTab(Screen.Record,  "🎙", "RECORD"),
    NavTab(Screen.Ask,     "✦",  "ASK"),
    NavTab(Screen.Profile, "◉",  "PROFILE"),
)

private val fullScreenRoutes = setOf(
    Screen.Review.route.substringBefore("/"),
    Screen.Auth.route,
)

// Shared transition specs
private val fadeDuration  = 160
private val slideMs       = 280

@Composable
fun SmaranNavGraph() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Screen.Record.route
    val showBottomNav = fullScreenRoutes.none { currentRoute.startsWith(it) }

    val startDestination = remember {
        if (TokenStorage.isLoggedIn) Screen.Record.route else Screen.Auth.route
    }

    Scaffold(
        containerColor = SmaranColors.Background,
        bottomBar = {
            if (showBottomNav) {
                SmaranBottomNav(
                    currentRoute = currentRoute,
                    onTabSelected = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Record.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding),
            // No global transitions — each route defines its own
            enterTransition  = { fadeIn(tween(fadeDuration)) },
            exitTransition   = { fadeOut(tween(fadeDuration)) },
            popEnterTransition  = { fadeIn(tween(fadeDuration)) },
            popExitTransition   = { fadeOut(tween(fadeDuration)) },
        ) {

            // ── Auth: shown on first launch until logged in ────────────────────

            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Screen.Record.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Tab screens: simple cross-fade, no slide ──────────────────────

            composable(Screen.Record.route) {
                RecordScreen(
                    onTranscriptionReady = { text, duration ->
                        navController.navigate(Screen.Review.createRoute(text, duration))
                    }
                )
            }

            composable(Screen.Ask.route) { AskScreen() }

            composable(Screen.Profile.route) { ProfileScreen() }

            // ── Review: slides in from right, slides out to right on back ─────

            composable(
                route     = Screen.Review.route,
                arguments = listOf(
                    navArgument("transcribedText") { type = NavType.StringType },
                    navArgument("durationSeconds") { type = NavType.IntType }
                ),
                enterTransition    = {
                    slideInHorizontally(tween(slideMs, easing = EaseOut)) { it }
                },
                exitTransition     = { fadeOut(tween(fadeDuration)) },
                popEnterTransition = { fadeIn(tween(fadeDuration)) },
                popExitTransition  = {
                    slideOutHorizontally(tween(slideMs, easing = EaseIn)) { it }
                },
            ) { backStackEntry ->
                val rawText  = backStackEntry.arguments?.getString("transcribedText") ?: ""
                val text     = java.net.URLDecoder.decode(rawText, "UTF-8")
                val duration = backStackEntry.arguments?.getInt("durationSeconds") ?: 0

                ReviewScreen(
                    transcribedText    = text,
                    durationSeconds    = duration,
                    onBack             = { navController.popBackStack() },
                    onSentSuccessfully = { navController.popBackStack() }
                )
            }
        }
    }
}

// ─── Bottom Nav ───────────────────────────────────────────────────────────────

@Composable
private fun SmaranBottomNav(currentRoute: String, onTabSelected: (Screen) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SmaranColors.Surface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = SmaranColors.Border, thickness = 0.5.dp)
        Row(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                NavTabItem(
                    tab      = tab,
                    selected = currentRoute == tab.screen.route,
                    onClick  = { onTabSelected(tab.screen) }
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(tab: NavTab, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(3.dp)
                .clip(CircleShape)
                .background(if (selected) SmaranColors.Purple else Color.Transparent)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = tab.icon,
            fontSize  = if (selected) 20.sp else 18.sp,
            color     = if (selected) SmaranColors.Purple else SmaranColors.TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text  = tab.label,
            style = SmaranType.labelSmall.copy(
                color    = if (selected) SmaranColors.Purple else SmaranColors.TextMuted,
                fontSize = 8.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}