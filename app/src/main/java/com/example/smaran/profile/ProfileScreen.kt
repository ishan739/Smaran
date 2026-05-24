package com.example.smaran.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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

@Composable
fun ProfileScreen(vm: ProfileViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    val initials = buildString {
        settings.firstName.firstOrNull()?.let { append(it.uppercaseChar()) }
        settings.lastName.firstOrNull()?.let { append(it.uppercaseChar()) }
    }.ifBlank { "?" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmaranColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(52.dp))

        Text(
            text      = "PROFILE",
            style     = SmaranType.labelSmall.copy(color = SmaranColors.Purple, fontSize = 13.sp),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        // ── User info ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SmaranColors.Surface)
                .border(1.dp, SmaranColors.Border, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar circle with initials
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SmaranColors.PurpleDim)
                        .border(1.dp, SmaranColors.Purple.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = initials,
                        style = SmaranType.labelMedium.copy(
                            color    = SmaranColors.Purple,
                            fontSize = 16.sp
                        )
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    val fullName = listOf(settings.firstName, settings.lastName)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                    Text(
                        text  = fullName.ifBlank { "—" },
                        style = SmaranType.body.copy(color = SmaranColors.TextPrimary, fontSize = 15.sp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text  = settings.email.ifBlank { "—" },
                        style = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted, fontSize = 9.sp)
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Stats ─────────────────────────────────────────────────────────────
        Text(
            text     = "STATS",
            style    = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(label = "MEMORIES",   value = "—", modifier = Modifier.weight(1f))
            StatCard(label = "TOTAL TIME", value = "—", modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        // ── Settings ──────────────────────────────────────────────────────────
        Text(
            text     = "SETTINGS",
            style    = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        SettingRow(title = "Language") {
            PillToggle(
                options  = listOf("English", "Hindi", "Hinglish"),
                selected = settings.language,
                onSelect = { vm.setLanguage(it) }
            )
        }

        Spacer(Modifier.height(40.dp))

        HorizontalDivider(color = SmaranColors.Border)

        Spacer(Modifier.height(24.dp))

        // ── Danger zone ───────────────────────────────────────────────────────
        Text(
            text     = "DANGER ZONE",
            style    = SmaranType.labelSmall.copy(color = SmaranColors.Red),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick  = { showClearDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = SmaranColors.Red),
            border   = androidx.compose.foundation.BorderStroke(1.dp, SmaranColors.Red.copy(alpha = 0.6f))
        ) {
            Text(
                text  = "CLEAR ALL MEMORIES",
                style = SmaranType.labelMedium.copy(color = SmaranColors.Red)
            )
        }

        Spacer(Modifier.height(40.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest  = { showClearDialog = false },
            containerColor    = SmaranColors.Surface,
            titleContentColor = SmaranColors.TextPrimary,
            textContentColor  = SmaranColors.TextSecondary,
            title = {
                Text("Clear all memories?", style = SmaranType.body.copy(color = SmaranColors.TextPrimary))
            },
            text = {
                Text(
                    "This cannot be undone. All your saved memories will be permanently deleted.",
                    style = SmaranType.body.copy(color = SmaranColors.TextSecondary, fontSize = 13.sp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("CLEAR", style = SmaranType.labelMedium.copy(color = SmaranColors.Red))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("CANCEL", style = SmaranType.labelMedium.copy(color = SmaranColors.TextMuted))
                }
            }
        )
    }
}

// ─── Stat Card ────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SmaranColors.Surface)
            .border(1.dp, SmaranColors.Border, RoundedCornerShape(12.dp))
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = value,
                style = SmaranType.timerLarge.copy(color = SmaranColors.TextPrimary, fontSize = 28.sp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = label,
                style = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted)
            )
        }
    }
}

// ─── Setting Row ──────────────────────────────────────────────────────────────

@Composable
private fun SettingRow(title: String, toggle: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text     = title.uppercase(),
            style    = SmaranType.labelSmall.copy(color = SmaranColors.TextSecondary),
            modifier = Modifier.weight(1f)
        )
        toggle()
    }
}

// ─── Pill Toggle ──────────────────────────────────────────────────────────────

@Composable
private fun PillToggle(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) SmaranColors.Purple else SmaranColors.SurfaceVariant)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else SmaranColors.Border,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = option,
                    style = SmaranType.labelSmall.copy(
                        color    = if (isSelected) Color.White else SmaranColors.TextMuted,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}