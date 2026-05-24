package com.example.smaran.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smaran.ui.theme.SmaranColors
import com.example.smaran.ui.theme.SmaranType

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(Unit) {
        vm.onSuccess.collect { onAuthSuccess() }
    }

    var isLogin    by remember { mutableStateOf(true) }
    var email      by remember { mutableStateOf("") }
    var password   by remember { mutableStateOf("") }
    var firstName  by remember { mutableStateOf("") }
    var lastName   by remember { mutableStateOf("") }

    val isLoading = uiState is AuthUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmaranColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(72.dp))

        Text(
            text      = "SMARAN",
            style     = SmaranType.labelSmall.copy(color = SmaranColors.Purple, fontSize = 16.sp),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text      = "your personal memory",
            style     = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted, fontSize = 9.sp),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        // ── Tab toggle ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SmaranColors.SurfaceVariant)
                .border(1.dp, SmaranColors.Border, RoundedCornerShape(20.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(true to "LOGIN", false to "SIGN UP").forEach { (isLoginTab, label) ->
                val selected = isLogin == isLoginTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) SmaranColors.Purple else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) {
                            if (isLogin != isLoginTab) {
                                isLogin = isLoginTab
                                // Clear error on tab switch
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = label,
                        style = SmaranType.labelSmall.copy(
                            color    = if (selected) Color.White else SmaranColors.TextMuted,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Sign up extra fields ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = !isLogin,
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AuthTextField(
                        value         = firstName,
                        label         = "FIRST NAME",
                        onValueChange = { firstName = it },
                        modifier      = Modifier.weight(1f)
                    )
                    AuthTextField(
                        value         = lastName,
                        label         = "LAST NAME",
                        onValueChange = { lastName = it },
                        modifier      = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        AuthTextField(
            value         = email,
            label         = "EMAIL",
            onValueChange = { email = it },
            keyboardType  = KeyboardType.Email,
            modifier      = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        AuthTextField(
            value         = password,
            label         = "PASSWORD",
            onValueChange = { password = it },
            keyboardType  = KeyboardType.Password,
            isPassword    = true,
            modifier      = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // ── Error ─────────────────────────────────────────────────────────────
        AnimatedVisibility(visible = uiState is AuthUiState.Error) {
            Column {
                Text(
                    text      = (uiState as? AuthUiState.Error)?.message ?: "",
                    style     = SmaranType.labelSmall.copy(color = SmaranColors.Red, fontSize = 10.sp),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
            }
        }

        // ── Submit button ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isLoading) SmaranColors.PurpleDim else SmaranColors.Purple)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    enabled           = !isLoading
                ) {
                    if (isLogin) vm.login(email, password)
                    else vm.signup(email, password, firstName, lastName)
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text  = if (isLogin) "LOGIN" else "SIGN UP",
                    style = SmaranType.labelMedium.copy(color = Color.White)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Spacer(Modifier.height(32.dp))
    }
}

// ─── Auth text field ──────────────────────────────────────────────────────────

@Composable
private fun AuthTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text  = label,
            style = SmaranType.labelSmall.copy(color = SmaranColors.TextMuted, fontSize = 8.sp)
        )
        Spacer(Modifier.height(5.dp))
        BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            singleLine    = true,
            textStyle     = SmaranType.body.copy(color = SmaranColors.TextPrimary, fontSize = 14.sp),
            cursorBrush   = SolidColor(SmaranColors.Purple),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier      = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .clip(RoundedCornerShape(10.dp))
                .background(SmaranColors.Surface)
                .border(
                    width = 1.dp,
                    color = if (focused) SmaranColors.Purple.copy(alpha = 0.6f) else SmaranColors.Border,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 14.dp, vertical = 13.dp)
        )
    }
}