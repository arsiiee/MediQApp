package com.example.mediq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediq.data.api.RetrofitClient
import com.example.mediq.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppointmentsViewModel : ViewModel() {
    private val api = RetrofitClient.apiService

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadAppointments(status: AppointmentStatus? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = api.getAppointments(status = status)
                _appointments.value = result.items
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load appointments"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelAppointment(id: String) {
        viewModelScope.launch {
            try {
                api.cancelAppointment(id)
                loadAppointments()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to cancel appointment"
            }
        }
    }
}