package com.example.mediq.ui.screens.appointments

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppointmentCancellationStore {
    private val _cancelledIds = MutableStateFlow<Set<String>>(emptySet())
    val cancelledIds: StateFlow<Set<String>> = _cancelledIds.asStateFlow()

    fun cancel(id: String) {
        _cancelledIds.value = _cancelledIds.value + id
    }

    fun isCancelled(id: String): Boolean = _cancelledIds.value.contains(id)
}
