package com.example.mediq.ui.feature.auth.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.MediQBrandAppBar
import com.example.mediq.core.designsystem.component.MediQButton
import com.example.mediq.core.designsystem.component.MediQInfoCard
import com.example.mediq.core.designsystem.component.MediQInfoCardTint
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQLightGreen
import com.example.mediq.core.designsystem.theme.MediQSurface
import com.example.mediq.ui.navigation.Screen

/** The code is six digits on the wire; the boxes are drawn to that count. */
private const val OTP_LENGTH = 6

@Composable
fun RegisterOTPScreen(
    navController: NavController,
    viewModel: RegisterViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Only a verified code moves the wizard on. The button used to navigate
    // unconditionally, so any six digits reached the credentials step.
    LaunchedEffect(uiState.pendingStep) {
        if (uiState.pendingStep == RegisterStep.CREDENTIALS) {
            viewModel.onStepHandled()
            navController.navigate(Screen.RegisterCredentials.route)
        }
    }

    Scaffold(
        topBar = {
            MediQBrandAppBar(
                title = "Verify your number",
                onBack = { navController.popBackStack() },
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                text = "We sent a 6-digit code to",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (uiState.mobileNumber.isNotBlank()) {
                Text(
                    text = uiState.mobileNumber,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            // The rule is real: `AuthService` counts wrong guesses against the
            // code row and refuses past `MAX_OTP_ATTEMPTS` (five). The figure's
            // timer is not drawn, because no countdown state exists yet.
            Text(
                text = "Five wrong attempts locks this code.",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = LocalMediQColors.current.secondaryText,
            )

            Spacer(modifier = Modifier.height(24.dp))
            OtpBoxes(
                value = uiState.otp,
                onValueChange = viewModel::onOtpChanged,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            // There is no SMS provider yet, so the server returns the code it
            // generated (`AuthService.returnCodeToCaller`). Shown as a hint, never
            // required - a real provider sends nothing back and the field still
            // works. Fig. 2 draws this as a warning card; the copy stays the app's.
            if (uiState.devOtpHint.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                MediQInfoCard(tint = MediQInfoCardTint.Warning) {
                    Text(
                        text = "Development only",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Your code is ${uiState.devOtpHint}.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            val error = uiState.error
            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                MediQInfoCard(tint = MediQInfoCardTint.Error) {
                    Text(text = error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            MediQButton(
                text = "Verify and continue",
                onClick = viewModel::verifyOtp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                loading = uiState.isLoading,
            )

            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Wrong number?", color = LocalMediQColors.current.secondaryText)
            }
        }
    }
}

/**
 * Fig. 2's six-box code display.
 *
 * It is one real `BasicTextField` stretched across six decorative boxes, with its
 * glyphs and caret drawn transparent so the boxes are what a patient sees. That
 * keeps the screen on the single `otp` string the ViewModel already owns - six
 * separate fields would need six pieces of focus state, which is new state this
 * module was scoped not to add.
 *
 * The field underneath still carries the keyboard options, so Rule 4 holds: this
 * is a credential, and `NumberPassword` is what stops the keyboard rewriting the
 * digits. The transparent glyphs are a presentation trick, not a masked field -
 * what protects the value is the keyboard, not the pixels.
 */
@Composable
private fun OtpBoxes(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(OTP_LENGTH) { index ->
                val digit = value.getOrNull(index)
                val isNext = index == value.length
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isNext) MediQLightGreen else MediQSurface,
                    border = if (isNext) {
                        BorderStroke(2.dp, MediQGreen)
                    } else {
                        BorderStroke(1.dp, LocalMediQColors.current.outline)
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = digit?.toString().orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.matchParentSize(),
            enabled = enabled,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
        )
    }
}
