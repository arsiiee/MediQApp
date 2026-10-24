package com.example.mediq.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

/*
 * Splash and sign-in, every state, renderable without a device.
 *
 * The sign-in previews reuse one action set, so the four states read as one
 * screen moving forward rather than four different screens.
 */

private val sampleActions = SignInActions()

@Preview(name = "Splash", showBackground = true, heightDp = 800)
@Composable
private fun SplashPreview() {
    MediQTheme {
        SplashContent(onSignIn = {}, onRegister = {})
    }
}

@Preview(name = "Sign in - idle", showBackground = true, heightDp = 900)
@Composable
private fun SignInIdlePreview() {
    MediQTheme {
        SignInContent(state = AuthSamples.signInIdle, actions = sampleActions)
    }
}

@Preview(name = "Sign in - blank fields", showBackground = true, heightDp = 900)
@Composable
private fun SignInBlankPreview() {
    MediQTheme {
        SignInContent(
            state = AuthSamples.signInIdle,
            actions = sampleActions,
            initialErrors = AuthSamples.signInErrors
        )
    }
}

@Preview(name = "Sign in - loading", showBackground = true, heightDp = 900)
@Composable
private fun SignInLoadingPreview() {
    MediQTheme {
        SignInContent(state = AuthSamples.signingIn, actions = sampleActions)
    }
}

@Preview(name = "Sign in - rejected", showBackground = true, heightDp = 900)
@Composable
private fun SignInRejectedPreview() {
    MediQTheme {
        SignInContent(state = AuthSamples.invalidCredentials, actions = sampleActions)
    }
}

@Preview(name = "Sign in - error", showBackground = true, heightDp = 900)
@Composable
private fun SignInErrorPreview() {
    MediQTheme {
        SignInContent(state = AuthSamples.signInError, actions = sampleActions)
    }
}
