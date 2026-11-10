package com.example.mediq.ui.screens.booking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPlaceholder
import com.example.mediq.ui.theme.MediQTextPrimary

/**
 * The confirm-a-slot form.
 *
 * The reason and the confirmation tick are view state the patient owns, so
 * they stay put while [submission] moves between states — a failed booking
 * never clears what they typed.
 */
@Composable
internal fun BookingForm(
    summary: BookingSummary,
    submission: BookingSubmission,
    actions: BookingActions,
    modifier: Modifier = Modifier
) {
    var reason by rememberSaveable { mutableStateOf("") }
    var confirmed by rememberSaveable { mutableStateOf(false) }

    val isSubmitting = submission == BookingSubmission.Submitting
    val slotGone = submission is BookingSubmission.SlotTaken
    val canConfirm = confirmed && !isSubmitting && !slotGone

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MediQSpacing.gutter)
    ) {
        Spacer(Modifier.height(MediQSpacing.md))

        BookingDetailsCard(summary)

        Spacer(Modifier.height(MediQSpacing.lg))

        Text(
            text = "Reason for visit",
            style = MaterialTheme.typography.labelLarge,
            color = MediQTextPrimary
        )

        Spacer(Modifier.height(MediQSpacing.sm))

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MediQSize.multilineField),
            placeholder = {
                Text(
                    text = "Enter the reason for your consultation...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediQTextPlaceholder
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MediQBackground,
                unfocusedContainerColor = MediQBackground,
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = MediQBorder,
                cursorColor = MediQGreen
            )
        )

        Spacer(Modifier.height(MediQSpacing.md))

        ConfirmationRow(
            checked = confirmed,
            enabled = !isSubmitting,
            onCheckedChange = { confirmed = it }
        )

        SubmissionNotice(submission = submission, actions = actions)

        Spacer(Modifier.height(MediQSpacing.sm))

        ConfirmButton(
            label = when {
                isSubmitting -> "Confirming..."
                slotGone -> "Slot no longer available"
                else -> "Confirm Booking"
            },
            enabled = canConfirm,
            onClick = actions.onConfirm
        )

        Spacer(Modifier.height(MediQSpacing.xxl))
    }
}
