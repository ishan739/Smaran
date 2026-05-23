package com.example.smaran.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smaran.ui.theme.SmaranColors
import com.example.smaran.ui.theme.SmaranType

// View model and state classes
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReviewScreen(
    transcribedText: String,
    durationSeconds: Int,
    onBack: () -> Unit,
    onSentSuccessfully: () -> Unit,
    vm: ReviewViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()

    // One-time nav events
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is ReviewEvent.NavigateBack -> onSentSuccessfully()
            }
        }
    }

    // Editable text state — starts with Whisper output, user can fix it
    var editedText by remember { mutableStateOf(transcribedText) }

    val wordCount = remember(editedText) {
        editedText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    }

    val durationLabel = remember(durationSeconds) {
        "%02d:%02d".format(durationSeconds / 60, durationSeconds % 60)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmaranColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(52.dp))

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SmaranColors.SurfaceVariant)
                    .border(1.dp, SmaranColors.Border, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = SmaranColors.Purple, fontSize = 16.sp)
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text     = "REVIEW MEMORY",
                style    = SmaranType.labelSmall.copy(color = SmaranColors.Purple),
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SmaranColors.SurfaceVariant)
                    .border(1.dp, SmaranColors.Border, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text  = durationLabel,
                    style = SmaranType.labelMedium.copy(color = SmaranColors.TextMuted)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Transcription label + word count ──────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text  = "TRANSCRIPTION",
                style = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted)
            )
            Text(
                text  = "$wordCount words",
                style = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted)
            )
        }

        Spacer(Modifier.height(10.dp))

        // ── Editable text box ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SmaranColors.Surface)
                .border(
                    width = 1.dp,
                    color = SmaranColors.Border,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                TextField(
                    value         = editedText,
                    onValueChange = { editedText = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 160.dp),
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor        = SmaranColors.TextPrimary,
                        unfocusedTextColor      = SmaranColors.TextPrimary,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor             = SmaranColors.Purple,
                    ),
                    textStyle     = SmaranType.body,
                    placeholder   = {
                        Text(
                            "Transcription will appear here...",
                            style = SmaranType.body.copy(color = SmaranColors.TextMuted)
                        )
                    }
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✎ ", color = SmaranColors.TextDim, fontSize = 12.sp)
                    Text(
                        text  = "TAP TO EDIT — FIX WHISPER ERRORS",
                        style = SmaranType.labelSmall.copy(
                            color    = SmaranColors.TextDim,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Recording info chips ──────────────────────────────────────────────
        Text(
            text     = "RECORDING INFO",
            style    = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Get current date using Calendar (API level safe)
            val today = remember {
                java.util.Calendar.getInstance().run {
                    "%04d-%02d-%02d".format(
                        get(java.util.Calendar.YEAR),
                        get(java.util.Calendar.MONTH) + 1,
                        get(java.util.Calendar.DAY_OF_MONTH)
                    )
                }
            }
            InfoChip(label = "📅 $today")
            InfoChip(label = "⏱ $durationLabel")
            InfoChip(
                label = "✓ TRANSCRIBED",
                textColor = SmaranColors.Purple,
                bgColor   = Color(0xFF1A1428),
                borderColor = Color(0xFF2A2040)
            )
        }

        Spacer(Modifier.height(24.dp))

        HorizontalDivider(color = SmaranColors.Border, thickness = 1.dp)

        Spacer(Modifier.height(20.dp))

        // ── Error message ─────────────────────────────────────────────────────
        AnimatedVisibility(visible = uiState is ReviewUiState.Error) {
            Text(
                text      = (uiState as? ReviewUiState.Error)?.message ?: "",
                style     = SmaranType.body.copy(color = SmaranColors.Red, fontSize = 12.sp),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(bottom = 12.dp)
            )
        }

        // ── Send button ───────────────────────────────────────────────────────
        val isSending = uiState is ReviewUiState.Sending
        val isSent    = uiState is ReviewUiState.Sent

        Button(
            onClick  = { vm.sendMemory(editedText) },
            enabled  = !isSending && !isSent,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = when {
                    isSent    -> Color(0xFF1A3A28)
                    isSending -> SmaranColors.PurpleDim
                    else      -> SmaranColors.Purple
                },
                contentColor           = when {
                    isSent -> SmaranColors.Green
                    else   -> Color.White
                },
                disabledContainerColor = when {
                    isSent    -> Color(0xFF1A3A28)
                    isSending -> SmaranColors.PurpleDim
                    else      -> SmaranColors.PurpleDim
                },
                disabledContentColor   = when {
                    isSent -> SmaranColors.Green
                    else   -> SmaranColors.TextSecondary
                }
            )
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(18.dp),
                    color       = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text  = when {
                    isSent    -> "SENT ✓"
                    isSending -> "SENDING..."
                    else      -> "SEND TO MEMORY"
                },
                style = SmaranType.labelMedium.copy(fontSize = 12.sp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Discard button ────────────────────────────────────────────────────
        OutlinedButton(
            onClick  = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SmaranColors.TextMuted
            ),
            border = BorderStroke(1.dp, SmaranColors.Border)
        ) {
            Text(
                text  = "DISCARD",
                style = SmaranType.labelMedium.copy(color = SmaranColors.TextMuted)
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─── Info chip ─────────────────────────────────────────────────────────────────

@Composable
private fun InfoChip(
    label: String,
    textColor: Color   = SmaranColors.TextMuted,
    bgColor: Color     = SmaranColors.SurfaceVariant,
    borderColor: Color = SmaranColors.Border,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text  = label,
            style = SmaranType.labelSmall.copy(color = textColor, fontSize = 10.sp)
        )
    }
}