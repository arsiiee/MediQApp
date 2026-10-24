package com.example.mediq.ui.screens.doctors

/** A doctor as shown in search results. */
data class DoctorInfo(
    val id: String,
    val name: String,
    val initials: String,
    val specialty: String,
    val experience: Int,
    val fee: Int,
    val location: String,
    val openSlots: Int
)

/** The catalogue the screen searches. */
sealed interface DoctorsUiState {

    /** Skeleton that mirrors the result list. */
    data object Loading : DoctorsUiState

    /**
     * Catalogue loaded.
     *
     * An empty [doctors] list is a normal state — the screen says so rather than
     * showing a blank page.
     */
    data class Content(val doctors: List<DoctorInfo>) : DoctorsUiState

    /** The catalogue could not load. */
    data class Error(val message: String) : DoctorsUiState
}

/** Callbacks the doctors screen raises. */
data class DoctorsActions(
    val onDoctorClick: (String) -> Unit = {},
    val onRetry: () -> Unit = {}
)

/**
 * What the patient has narrowed the list to.
 *
 * View state, owned by the screen: it is not part of the loaded catalogue and
 * not something the caller needs to know about.
 */
data class DoctorFilters(
    val query: String = "",
    val specialty: String = AllSpecialties,
    val location: String = AllLocations
) {
    /** True when nothing has been narrowed, i.e. the full catalogue is on screen. */
    val isDefault: Boolean
        get() = query.isBlank() &&
                specialty == AllSpecialties &&
                location == AllLocations

    companion object {
        val Default = DoctorFilters()
    }
}
