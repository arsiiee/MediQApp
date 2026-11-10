package com.example.mediq.ui.screens.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQSpacing

/**
 * The bookings behind one tab.
 *
 * An empty tab is not a blank page: it says which kind of nothing it is via
 * [AppointmentsEmptyState].
 */
@Composable
internal fun AppointmentsList(
    tab: AppointmentsTab,
    bookings: List<AppointmentBooking>,
    onOpen: (AppointmentBooking) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MediQSpacing.gutter,
            end = MediQSpacing.gutter,
            top = MediQSpacing.sm,
            bottom = MediQSpacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(MediQSpacing.md)
    ) {
        if (bookings.isEmpty()) {
            item { AppointmentsEmptyState(tab = tab) }
        } else {
            items(items = bookings, key = { it.id }) { booking ->
                AppointmentCard(
                    booking = booking,
                    onClick = { onOpen(booking) }
                )
            }
        }
    }
}
