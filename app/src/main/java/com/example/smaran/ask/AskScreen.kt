package com.example.smaran.ask

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smaran.ui.theme.SmaranColors
import com.example.smaran.ui.theme.SmaranType

@Composable
fun AskScreen(vm: AskViewModel = viewModel()) {
    val entries     by vm.entries.collectAsState()
    val inputText   by vm.inputText.collectAsState()
    val isListening by vm.isListening.collectAsState()
    val playingId   by vm.playingId.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmaranColors.Background)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Spacer(Modifier.height(20.dp))

        Text(
            text      = "ASK SMARAN",
            style     = SmaranType.labelSmall.copy(color = SmaranColors.Purple, fontSize = 13.sp),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text      = "ask anything about your memories",
            style     = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted, fontSize = 10.sp, letterSpacing = 1.sp),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = SmaranColors.Border)

        // ── Q&A List ──────────────────────────────────────────────────────────
        val listState = rememberLazyListState()

        LazyColumn(
            modifier        = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state           = listState,
            reverseLayout   = true,
            contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries.reversed(), key = { it.id }) { entry ->
                AnswerCard(
                    entry     = entry,
                    isPlaying = playingId == entry.id,
                    onPlay    = { vm.speakAnswer(entry.answer, entry.id, entry.mood) },
                    onStop    = { vm.stopSpeaking() }
                )
                Spacer(Modifier.height(8.dp))
                QuestionBubble(question = entry.question, timestamp = entry.timestamp)
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        HorizontalDivider(color = SmaranColors.Border)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value         = inputText,
                onValueChange = { vm.onInputChange(it) },
                modifier      = Modifier.weight(1f),
                textStyle     = SmaranType.body.copy(color = SmaranColors.TextPrimary),
                cursorBrush   = SolidColor(SmaranColors.Purple),
                decorationBox = { inner ->
                    Box {
                        if (inputText.isEmpty()) {
                            Text(
                                text  = "what do you want to know?",
                                style = SmaranType.body.copy(color = SmaranColors.TextMuted, fontSize = 14.sp)
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.width(8.dp))

            // Voice button
            VoiceInputButton(isListening = isListening) {
                if (isListening) vm.stopVoiceInput() else vm.startVoiceInput()
            }

            Spacer(Modifier.width(6.dp))

            // Send button
            val canSend = inputText.isNotBlank()
            val sendSource = remember { MutableInteractionSource() }
            val sendPressed by sendSource.collectIsPressedAsState()

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .scale(if (sendPressed) 0.95f else 1f)
                    .clip(CircleShape)
                    .background(if (canSend) SmaranColors.Purple else SmaranColors.SurfaceVariant)
                    .border(1.dp, if (canSend) Color.Transparent else SmaranColors.Border, CircleShape)
                    .clickable(
                        interactionSource = sendSource,
                        indication        = null,
                        enabled           = canSend
                    ) { vm.sendQuestion(inputText) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "→",
                    color = if (canSend) Color.White else SmaranColors.TextMuted,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ─── Question Bubble ──────────────────────────────────────────────────────────

@Composable
private fun QuestionBubble(question: String, timestamp: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        BoxWithConstraints {
            Box(
                modifier = Modifier
                    .widthIn(max = maxWidth * 0.78f)
                    .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                    .background(Color(0xFF1A1428))
                    .border(1.dp, SmaranColors.Purple.copy(alpha = 0.35f), RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text  = question,
                        style = SmaranType.body.copy(color = SmaranColors.TextPrimary, fontSize = 14.sp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text      = timestamp,
                        style     = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted, fontSize = 9.sp),
                        modifier  = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

// ─── Answer Card ──────────────────────────────────────────────────────────────

@Composable
private fun AnswerCard(
    entry: QaEntry,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SmaranColors.Surface)
            .border(1.dp, SmaranColors.Border, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        if (entry.isLoading) {
            LoadingDots()
        } else {
            Column {
                Text(
                    text  = entry.answer,
                    style = SmaranType.body.copy(
                        color      = SmaranColors.TextPrimary,
                        fontSize   = 14.sp,
                        lineHeight = 24.sp
                    )
                )

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = entry.timestamp,
                        style = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted, fontSize = 9.sp)
                    )
                    Spacer(Modifier.weight(1f))
                    PlayButton(isPlaying = isPlaying, onPlay = onPlay, onStop = onStop)
                }
            }
        }
    }
}

// ─── Play Button ──────────────────────────────────────────────────────────────

@Composable
private fun PlayButton(isPlaying: Boolean, onPlay: () -> Unit, onStop: () -> Unit) {
    val pulseAnim = rememberInfiniteTransition(label = "ttsPlay")
    val pulse by pulseAnim.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.25f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label         = "ttsPulse"
    )

    Box(
        modifier = Modifier
            .size(32.dp)
            .scale(if (isPlaying) pulse else 1f)
            .clip(CircleShape)
            .background(if (isPlaying) SmaranColors.Amber else SmaranColors.Purple)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { if (isPlaying) onStop() else onPlay() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text     = if (isPlaying) "■" else "▶",
            fontSize = 11.sp,
            color    = Color.White
        )
    }
}

// ─── Voice Input Button ───────────────────────────────────────────────────────

@Composable
private fun VoiceInputButton(isListening: Boolean, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "voicePulse")
    val scale by pulse.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.4f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseOut), RepeatMode.Restart),
        label         = "voiceScale"
    )

    Box(contentAlignment = Alignment.Center) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .border(1.dp, SmaranColors.Red.copy(alpha = 0.3f), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isListening) SmaranColors.Red else SmaranColors.SurfaceVariant)
                .border(1.dp, if (isListening) Color.Transparent else SmaranColors.Border, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("🎙", fontSize = 15.sp)
        }
    }
}

// ─── Loading Dots ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val alphas = (0..2).map { i ->
        infiniteTransition.animateFloat(
            initialValue  = 0.2f,
            targetValue   = 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(600, delayMillis = i * 200, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot$i"
        )
    }

    Row(
        modifier            = Modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment   = Alignment.CenterVertically
    ) {
        alphas.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(SmaranColors.Purple.copy(alpha = alpha.value))
            )
        }
    }
}
