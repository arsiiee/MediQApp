package com.example.mediq.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mediq.ui.components.MediQNotice
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorContainer
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextSecondary
import com.example.mediq.ui.theme.MediQWarning
import com.example.mediq.ui.theme.MediQWarningContainer

/*
 * The two smaller parts of the sign-in screen: what went wrong with the last
 * attempt, and the two shortcuts off it.
 */

/**
 * Why the last attempt did not land, if it did not.
 *
 * A wrong pair gets the warning tone rather than the error tone: nothing is
 * broken, the patient just mistyped. A failure to reach the server is the
 * error tone, because retrying is the only thing that helps.
 */
@Composable
internal fun SignInNotice(state: SignInState, modifier: Modifier = Modifier) {
    when (state) {
        is SignInState.InvalidCredentials -> {
            Spacer(Modifier.height(MediQSpacing.md))
            MediQNotice(
                icon = Icons.Filled.Lock,
                message = state.message,
                container = MediQWarningContainer,
                content = MediQWarning,
                modifier = modifier
            )
        }

        is SignInState.Error -> {
            Spacer(Modifier.height(MediQSpacing.md))
            MediQNotice(
                icon = Icons.Outlined.CloudOff,
                message = state.message,
                container = MediQErrorContainer,
                content = MediQError,
                modifier = modifier
            )
        }

        SignInState.Idle, SignInState.SigningIn -> Unit
    }
}

/**
 * The line under the password field: the way out of a forgotten password, and
 * the note that the session is private.
 *
 * The lock repeats the words beside it, so it carries no description of its
 * own — a screen reader would otherwise say "Secure session. Secure session."
 */
@Composable
internal fun ForgotPasswordRow(
    onForgotPassword: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onForgotPassword,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = MediQSpacing.xxs),
            modifier = Modifier.heightIn(min = MediQSize.minTouchTarget)
        ) {
            Text(
                text = "Forgot password?",
                style = MaterialTheme.typography.labelLarge,
                color = MediQGreen
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MediQTextSecondary,
                modifier = Modifier.size(MediQSize.icon)
            )

            Text(
                text = "Secure session",
                style = MaterialTheme.typography.bodySmall,
                color = MediQTextSecondary,
                modifier = Modifier.padding(start = MediQSpacing.xs)
            )
        }
    }
}
