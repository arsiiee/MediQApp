package com.example.mediq.ui.screens.doctordetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSpacing

/**
 * The loaded screen: profile block, then the slot picker, in one scroll.
 *
 * Selection lives inside [DoctorSlotPicker] — it is view state the patient
 * changes on this screen and never leaves it, so nothing above needs to hear
 * about it.
 */
@Composable
internal fun DoctorDetailsLoaded(
    state: DoctorDetailsUiState.Content,
    actions: DoctorDetailsActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MediQScaffoldBackground)
    ) {
        DoctorProfileCard(doctor = state.doctor)

        DoctorSlotPicker(
            schedule = state.schedule,
            onContinue = actions.onContinue,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(MediQSpacing.xs))
    }
}
