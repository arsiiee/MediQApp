package com.example.mediq.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

/*
 * The two registration form steps, every state, renderable without a device.
 */

private val sampleActions = RegisterActions()

@Preview(name = "Register details - idle", showBackground = true, heightDp = 800)
@Composable
private fun DetailsIdlePreview() {
    MediQTheme {
        RegisterDetailsContent(
            state = AuthSamples.registerIdle,
            actions = sampleActions
        )
    }
}

@Preview(name = "Register details - invalid", showBackground = true, heightDp = 800)
@Composable
private fun DetailsInvalidPreview() {
    MediQTheme {
        RegisterDetailsContent(
            state = AuthSamples.registerIdle,
            actions = sampleActions,
            initialErrors = AuthSamples.detailsErrors
        )
    }
}

@Preview(name = "Register details - sending", showBackground = true, heightDp = 800)
@Composable
private fun DetailsSendingPreview() {
    MediQTheme {
        RegisterDetailsContent(
            state = AuthSamples.registering,
            actions = sampleActions
        )
    }
}

@Preview(name = "Register details - error", showBackground = true, heightDp = 800)
@Composable
private fun DetailsErrorPreview() {
    MediQTheme {
        RegisterDetailsContent(
            state = AuthSamples.detailsError,
            actions = sampleActions
        )
    }
}

@Preview(name = "Register credentials - idle", showBackground = true, heightDp = 800)
@Composable
private fun CredentialsIdlePreview() {
    MediQTheme {
        RegisterCredentialsContent(
            state = AuthSamples.registerIdle,
            actions = sampleActions
        )
    }
}

@Preview(name = "Register credentials - invalid", showBackground = true, heightDp = 800)
@Composable
private fun CredentialsInvalidPreview() {
    MediQTheme {
        RegisterCredentialsContent(
            state = AuthSamples.registerIdle,
            actions = sampleActions,
            initialErrors = AuthSamples.credentialsErrors
        )
    }
}

@Preview(name = "Register credentials - taken", showBackground = true, heightDp = 800)
@Composable
private fun CredentialsTakenPreview() {
    MediQTheme {
        RegisterCredentialsContent(
            state = AuthSamples.usernameTaken,
            actions = sampleActions
        )
    }
}
