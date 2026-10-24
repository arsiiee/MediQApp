package com.example.mediq.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

/*
 * Every home state, renderable without a device or a running app.
 *
 * These are the acceptance views for the home screen: if a state is not here,
 * it is not designed.
 */

@Preview(name = "Home — loaded", showBackground = true, heightDp = 900)
@Composable
private fun HomeLoadedPreview() {
    MediQTheme {
        HomeContent(state = HomeSamples.content, actions = HomeActions())
    }
}

@Preview(name = "Home — nothing booked", showBackground = true, heightDp = 900)
@Composable
private fun HomeNothingBookedPreview() {
    MediQTheme {
        HomeContent(state = HomeSamples.nothingBooked, actions = HomeActions())
    }
}

@Preview(name = "Home — no open slots", showBackground = true, heightDp = 900)
@Composable
private fun HomeNoOpenSlotsPreview() {
    MediQTheme {
        HomeContent(state = HomeSamples.noOpenSlots, actions = HomeActions())
    }
}

@Preview(name = "Home — loading", showBackground = true, heightDp = 900)
@Composable
private fun HomeLoadingPreview() {
    MediQTheme {
        HomeContent(state = HomeSamples.loading, actions = HomeActions())
    }
}

@Preview(name = "Home — error", showBackground = true, heightDp = 900)
@Composable
private fun HomeErrorPreview() {
    MediQTheme {
        HomeContent(state = HomeSamples.error, actions = HomeActions())
    }
}

@Preview(name = "Home — large font scale", showBackground = true, heightDp = 900, fontScale = 2f)
@Composable
private fun HomeLargeFontScalePreview() {
    MediQTheme {
        HomeContent(state = HomeSamples.content, actions = HomeActions())
    }
}
