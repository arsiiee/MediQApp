package com.example.mediq.ui.screens.doctors

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

/*
 * Every doctors state, renderable without a device.
 *
 * The two empty cases are separate previews on purpose: they say different
 * things to the patient and must not collapse into one screen.
 */

@Preview(name = "Doctors — loaded", showBackground = true, heightDp = 900)
@Composable
private fun DoctorsLoadedPreview() {
    MediQTheme {
        DoctorsContent(state = DoctorsSamples.loaded, actions = DoctorsActions())
    }
}

@Preview(name = "Doctors — loading", showBackground = true, heightDp = 900)
@Composable
private fun DoctorsLoadingPreview() {
    MediQTheme {
        DoctorsContent(state = DoctorsSamples.loading, actions = DoctorsActions())
    }
}

@Preview(name = "Doctors — no doctors published", showBackground = true, heightDp = 900)
@Composable
private fun DoctorsEmptyPreview() {
    MediQTheme {
        DoctorsContent(state = DoctorsSamples.empty, actions = DoctorsActions())
    }
}

@Preview(name = "Doctors — error", showBackground = true, heightDp = 900)
@Composable
private fun DoctorsErrorPreview() {
    MediQTheme {
        DoctorsContent(state = DoctorsSamples.error, actions = DoctorsActions())
    }
}

@Preview(name = "Doctors — no matches", showBackground = true, heightDp = 900)
@Composable
private fun DoctorsNoMatchesPreview() {
    MediQTheme {
        DoctorsNoMatchesState(onClearFilters = {})
    }
}
