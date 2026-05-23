package com.example.smaran.record

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smaran.NetworkStatus
import com.example.smaran.ui.theme.SmaranColors
import com.example.smaran.ui.theme.SmaranType
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun RecordScreen(
    onTranscriptionReady: (text: String, duration: Int) -> Unit,
    vm: RecordViewModel = viewModel()
) {
    val uiState       by vm.uiState.collectAsState()
    val partialText   by vm.partialText.collectAsState()
    val networkStatus by vm.networkStatus.collectAsState()

    // ── Collect one-time nav events ───────────────────────────────────────────
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is RecordEvent.NavigateToReview ->
                    onTranscriptionReady(event.text, event.durationSeconds)
            }
        }
    }

    // ── Mic permission ────────────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.startRecording() }

    // ── Timer ─────────────────────────────────────────────────────────────────
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState) {
        if (uiState is RecordUiState.Recording) {
            seconds = 0
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    val timerText = remember(seconds) {
        "%02d:%02d".format(seconds / 60, seconds % 60)
    }

    // ── Pulse animation ───────────────────────────────────────────────────────
    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue  = 1.5f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )

    val isRecording = uiState is RecordUiState.Recording

    // ── Layout ────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmaranColors.Background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(56.dp))

        // App name
        Text(
            text  = "SMARAN",
            style = SmaranType.labelSmall.copy(
                color    = SmaranColors.Purple,
                fontSize = 13.sp
            )
        )

        Spacer(Modifier.height(40.dp))

        // State label
        Text(
            text  = when (uiState) {
                is RecordUiState.Idle         -> "READY"
                is RecordUiState.Recording    -> "● RECORDING"
                is RecordUiState.Transcribing -> "PROCESSING..."
                is RecordUiState.Error        -> "ERROR"
            },
            style = SmaranType.labelSmall.copy(
                color = when (uiState) {
                    is RecordUiState.Recording    -> SmaranColors.Red
                    is RecordUiState.Transcribing -> SmaranColors.Amber
                    is RecordUiState.Error        -> SmaranColors.Red
                    else                          -> SmaranColors.TextMuted
                }
            )
        )

        Spacer(Modifier.height(8.dp))

        // Timer
        Text(
            text  = if (isRecording) timerText else "00:00",
            style = SmaranType.timerLarge.copy(
                color = if (isRecording) SmaranColors.TextPrimary else SmaranColors.TextMuted
            )
        )

        Spacer(Modifier.height(32.dp))

        // Waveform
        WaveformVisualizer(
            isActive = isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        )

        // Live partial text while speaking
        if (isRecording && partialText.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text      = partialText,
                style     = SmaranType.body.copy(
                    color    = SmaranColors.TextSecondary,
                    fontSize = 13.sp
                ),
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }

        Spacer(Modifier.height(40.dp))

        // Record button
        Box(contentAlignment = Alignment.Center) {
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .border(1.dp, SmaranColors.Purple.copy(alpha = 0.3f), CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(SmaranColors.SurfaceVariant)
                    .border(1.5.dp, SmaranColors.Border, CircleShape)
                    .clickable(enabled = uiState !is RecordUiState.Transcribing) {
                        when (uiState) {
                            is RecordUiState.Idle      -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            is RecordUiState.Recording -> vm.stopRecording()
                            is RecordUiState.Error     -> vm.clearError()
                            else                       -> Unit
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when (uiState) {
                    is RecordUiState.Transcribing -> {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(28.dp),
                            color       = SmaranColors.Amber,
                            strokeWidth = 2.dp
                        )
                    }
                    is RecordUiState.Recording -> {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SmaranColors.Red)
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SmaranColors.Purple),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎙", fontSize = 20.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text  = when (uiState) {
                is RecordUiState.Idle         -> "TAP TO RECORD"
                is RecordUiState.Recording    -> "TAP TO STOP"
                is RecordUiState.Transcribing -> "PROCESSING"
                is RecordUiState.Error        -> "TAP TO RETRY"
            },
            style = SmaranType.labelMedium.copy(color = SmaranColors.TextMuted)
        )

        Spacer(Modifier.height(12.dp))

        // Network status banner — only shown when there is an issue
        NetworkBanner(networkStatus)

        // Error message
        if (uiState is RecordUiState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                text      = (uiState as RecordUiState.Error).message,
                style     = SmaranType.body.copy(color = SmaranColors.Red, fontSize = 12.sp),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Network Status Banner ────────────────────────────────────────────────────

@Composable
private fun NetworkBanner(status: NetworkStatus) {
    val (message, color) = when (status) {
        NetworkStatus.Unavailable -> "No internet — turn on Wi-Fi or mobile data" to SmaranColors.Red
        NetworkStatus.LowSpeed    -> "Internet speed is too low" to SmaranColors.Red.copy(alpha = 0.85f)
        NetworkStatus.Unstable    -> "Network is unstable — may take some time" to SmaranColors.Amber
        NetworkStatus.Available   -> return
    }

    Text(
        text      = message,
        style     = SmaranType.labelSmall.copy(color = color, fontSize = 10.sp),
        textAlign = TextAlign.Center,
        modifier  = Modifier.fillMaxWidth()
    )
}

// ─── Waveform Visualizer ──────────────────────────────────────────────────────

@Composable
fun WaveformVisualizer(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val barHeights = remember { mutableStateListOf(*Array(48) { 4f }) }

    LaunchedEffect(isActive) {
        while (true) {
            for (i in barHeights.indices) {
                barHeights[i] = if (isActive) {
                    4f + Random.nextFloat() * 28f
                } else {
                    4f + sin(System.currentTimeMillis() / 1200.0 + i * 0.3).toFloat() * 3f
                }
            }
            delay(80)
        }
    }

    val activeColor = SmaranColors.Red.copy(alpha = 0.85f)
    val idleColor   = SmaranColors.Purple.copy(alpha = 0.25f)

    Canvas(modifier = modifier) {
        val totalWidth  = size.width
        val totalHeight = size.height
        val barCount    = barHeights.size
        val barWidth    = 3.dp.toPx()
        val gap         = (totalWidth - barCount * barWidth) / (barCount - 1)
        val centerY     = totalHeight / 2f

        barHeights.forEachIndexed { i, h ->
            val x = i * (barWidth + gap)
            drawLine(
                color       = if (isActive) activeColor else idleColor,
                start       = Offset(x + barWidth / 2, centerY - h / 2),
                end         = Offset(x + barWidth / 2, centerY + h / 2),
                strokeWidth = barWidth
            )
        }
    }
}