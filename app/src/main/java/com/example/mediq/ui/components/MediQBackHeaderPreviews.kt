package com.example.mediq.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.theme.MediQTheme

/*
 * MediQBackHeader previews.
 *
 * Renders the shared brand-green header in isolation so the fixed-height
 * variant used by appointments and messages (80dp) can be verified without
 * navigating to those screens.
 */

@Preview(name = "Back header — default height, with back", showBackground = true, heightDp = 200)
@Composable
private fun BackHeaderDefaultWithBackPreview() {
    MediQTheme {
        MediQBackHeader(title = "My appointments", onBack = {})
    }
}

@Preview(name = "Back header — 80dp fixed, no back", showBackground = true, heightDp = 200)
@Composable
private fun BackHeaderFixed80NoBackPreview() {
    MediQTheme {
        MediQBackHeader(title = "Messages", onBack = null, height = 80.dp)
    }
}

@Preview(name = "Back header — 80dp fixed, with back", showBackground = true, heightDp = 200)
@Composable
private fun BackHeaderFixed80WithBackPreview() {
    MediQTheme {
        MediQBackHeader(title = "Edit My Details", onBack = {}, height = 80.dp)
    }
}

@Preview(name = "Back header — long title ellipsis", showBackground = true, heightDp = 200)
@Composable
private fun BackHeaderLongTitlePreview() {
    MediQTheme {
        MediQBackHeader(
            title = "This is a very long screen title that should be ellipsized",
            onBack = null,
            height = 80.dp
        )
    }
}
