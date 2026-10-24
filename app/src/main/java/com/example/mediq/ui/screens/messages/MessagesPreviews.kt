package com.example.mediq.ui.screens.messages

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

@Preview(name = "Messages - loading", showBackground = true, heightDp = 800)
@Composable
private fun MessagesLoadingPreview() {
    MediQTheme { MessagesContent(state = MessagesSamples.loading, actions = MessagesActions()) }
}

@Preview(name = "Messages - error", showBackground = true, heightDp = 800)
@Composable
private fun MessagesErrorPreview() {
    MediQTheme { MessagesContent(state = MessagesSamples.error, actions = MessagesActions()) }
}

@Preview(name = "Messages - empty", showBackground = true, heightDp = 800)
@Composable
private fun MessagesEmptyPreview() {
    MediQTheme { MessagesContent(state = MessagesSamples.empty, actions = MessagesActions()) }
}

@Preview(name = "Messages - content", showBackground = true, heightDp = 800)
@Composable
private fun MessagesContentPreview() {
    MediQTheme { MessagesContent(state = MessagesSamples.content, actions = MessagesActions()) }
}