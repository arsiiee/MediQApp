package com.example.mediq.ui.feature.messages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Messages is still a stub. It renders its own title so the placeholder is no
 * longer a generic shared `PlaceholderScreen(name)` reused across tabs.
 */
@Composable
fun MessagesScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Messages")
    }
}
