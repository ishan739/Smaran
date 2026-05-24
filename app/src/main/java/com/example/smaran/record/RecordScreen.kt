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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
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

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is RecordEvent.NavigateToReview ->
                    onTranscriptionReady(event.text, event.durationSeconds)
            }
        }
    }

    @SuppressLint("MissingPermission")
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.startRecording() }

    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState) {
        if (uiState is RecordUiState.Recording) {
            seconds = 0
            while (true) { delay(1000); seconds++ }
        }
    }

    val timerText   = remember(seconds) { "%02d:%02d".format(seconds / 60, seconds % 60) }
    val isRecording = uiState is RecordUiState.Recording

    val pulse = rememberInfiniteTransition(label = "pulse")
    val p1 by pulse.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, easing = EaseOut), RepeatMode.Restart), "p1")
    val p2 by pulse.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, delayMillis = 600, easing = EaseOut), RepeatMode.Restart), "p2")
    val p3 by pulse.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, delayMillis = 1200, easing = EaseOut), RepeatMode.Restart), "p3")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmaranColors.Background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(52.dp))

        Text(
            text  = "SMARAN",
            style = SmaranType.labelSmall.copy(color = SmaranColors.Purple, fontSize = 13.sp)
        )

        Spacer(Modifier.weight(0.8f))

        // State label
        Text(
            text  = when (uiState) {
                is RecordUiState.Idle         -> "READY"
                is RecordUiState.Recording    -> "● REC"
                is RecordUiState.Transcribing -> "PROCESSING"
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

        Spacer(Modifier.height(6.dp))

        // Timer
        Text(
            text  = if (isRecording) timerText else "00:00",
            style = SmaranType.timerLarge.copy(
                color = if (isRecording) SmaranColors.TextPrimary
                        else SmaranColors.TextMuted.copy(alpha = 0.35f)
            )
        )

        Spacer(Modifier.height(28.dp))

        // ── Pulse rings + record button ───────────────────────────────────────
        Box(
            modifier         = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            val purple = SmaranColors.Purple

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center      = Offset(size.width / 2f, size.height / 2f)
                val buttonRadPx = 44.dp.toPx()
                val maxRadPx    = 120.dp.toPx()

                // Static decorative rings (always visible, faint)
                listOf(0.55f, 0.75f, 1.0f).forEach { fraction ->
                    drawCircle(
                        color  = purple,
                        radius = maxRadPx * fraction,
                        center = center,
                        alpha  = 0.07f,
                        style  = Stroke(width = 1.dp.toPx())
                    )
                }

                // Animated pulse rings while recording
                if (isRecording) {
                    listOf(p1, p2, p3).forEach { t ->
                        drawCircle(
                            color  = purple,
                            radius = buttonRadPx + (maxRadPx - buttonRadPx) * t,
                            center = center,
                            alpha  = (1f - t) * 0.55f,
                            style  = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(SmaranColors.SurfaceVariant)
                    .border(
                        width = 1.5.dp,
                        color = if (isRecording) SmaranColors.Purple.copy(alpha = 0.6f)
                                else SmaranColors.Border,
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        enabled           = uiState !is RecordUiState.Transcribing
                    ) {
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
                    is RecordUiState.Transcribing -> CircularProgressIndicator(
                        modifier    = Modifier.size(28.dp),
                        color       = SmaranColors.Amber,
                        strokeWidth = 2.dp
                    )
                    is RecordUiState.Recording -> Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SmaranColors.Red)
                    )
                    else -> Box(
                        modifier         = Modifier.size(48.dp).clip(CircleShape).background(SmaranColors.Purple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎙", fontSize = 20.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        WaveformVisualizer(
            isActive = isRecording,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text  = when (uiState) {
                is RecordUiState.Idle         -> "TAP TO RECORD"
                is RecordUiState.Recording    -> "TAP TO STOP"
                is RecordUiState.Transcribing -> "PROCESSING"
                is RecordUiState.Error        -> "TAP TO RETRY"
            },
            style = SmaranType.labelMedium.copy(color = SmaranColors.TextMuted)
        )

        // Live partial transcription — scrollable box, always shows latest text
        if (isRecording && partialText.isNotBlank()) {
            Spacer(Modifier.height(16.dp))

            val scrollState = rememberScrollState()
            LaunchedEffect(partialText) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SmaranColors.Surface.copy(alpha = 0.6f))
                    .border(1.dp, SmaranColors.Border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text     = partialText,
                    style    = SmaranType.body.copy(
                        color    = SmaranColors.TextSecondary,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.verticalScroll(scrollState)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        NetworkBanner(networkStatus)

        if (uiState is RecordUiState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                text      = (uiState as RecordUiState.Error).message,
                style     = SmaranType.body.copy(color = SmaranColors.Red, fontSize = 12.sp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))
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
fun WaveformVisualizer(isActive: Boolean, modifier: Modifier = Modifier) {
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
        val totalWidth = size.width
        val barCount   = barHeights.size
        val barWidth   = 3.dp.toPx()
        val gap        = (totalWidth - barCount * barWidth) / (barCount - 1)
        val centerY    = size.height / 2f

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