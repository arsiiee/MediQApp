package com.example.mediq.ui.screens.secretarychat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

@Preview(name = "Secretary chat - loading", showBackground = true, heightDp = 800)
@Composable
private fun ChatLoadingPreview() {
    MediQTheme { SecretaryChatContent(state = SecretaryChatSamples.loading, actions = ChatActions()) }
}

@Preview(name = "Secretary chat - error", showBackground = true, heightDp = 800)
@Composable
private fun ChatErrorPreview() {
    MediQTheme { SecretaryChatContent(state = SecretaryChatSamples.error, actions = ChatActions()) }
}

@Preview(name = "Secretary chat - maria", showBackground = true, heightDp = 800)
@Composable
private fun ChatMariaPreview() {
    MediQTheme { SecretaryChatContent(state = SecretaryChatSamples.maria, actions = ChatActions()) }
}

@Preview(name = "Secretary chat - kathleen", showBackground = true, heightDp = 800)
@Composable
private fun ChatKathleenPreview() {
    MediQTheme { SecretaryChatContent(state = SecretaryChatSamples.kathleen, actions = ChatActions()) }
}