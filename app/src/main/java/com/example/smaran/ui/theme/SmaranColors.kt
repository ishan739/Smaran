package com.example.smaran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Colors ───────────────────────────────────────────────────────────────────

object SmaranColors {
    val Background       = Color(0xFF0D0D14)
    val Surface          = Color(0xFF10101A)
    val SurfaceVariant   = Color(0xFF14141F)
    val Border           = Color(0xFF1E1E2E)
    val BorderFocus      = Color(0xFF2E2A5E)
    val Purple           = Color(0xFF7C6AF7)
    val PurpleDim        = Color(0xFF3A3060)
    val Red              = Color(0xFFF75A5A)
    val Green            = Color(0xFF3AB87A)
    val Amber            = Color(0xFFF7A85A)
    val TextPrimary      = Color(0xFFD0D0E8)
    val TextSecondary    = Color(0xFF6A6A8A)
    val TextMuted        = Color(0xFF3A3A5A)
    val TextDim          = Color(0xFF2A2A40)
}

private val DarkColorScheme = darkColorScheme(
    primary          = SmaranColors.Purple,
    background       = SmaranColors.Background,
    surface          = SmaranColors.Surface,
    onPrimary        = Color.White,
    onBackground     = SmaranColors.TextPrimary,
    onSurface        = SmaranColors.TextPrimary,
)

// ─── Typography ───────────────────────────────────────────────────────────────

object SmaranType {
    val Mono = FontFamily.Monospace

    val labelSmall = TextStyle(
        fontFamily = Mono,
        fontSize   = 10.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 3.sp,
    )
    val labelMedium = TextStyle(
        fontFamily = Mono,
        fontSize   = 11.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 2.sp,
    )
    val timerLarge = TextStyle(
        fontFamily = Mono,
        fontSize   = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
    )
    val body = TextStyle(
        fontSize   = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
        color      = SmaranColors.TextPrimary,
    )
}

// ─── Theme wrapper ────────────────────────────────────────────────────────────

@Composable
fun SmaranTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content     = content,
    )
}