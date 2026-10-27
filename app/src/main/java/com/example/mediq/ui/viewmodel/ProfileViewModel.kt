package com.example.mediq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediq.data.api.RetrofitClient
import com.example.mediq.data.model.*
import com.example.mediq.ui.session.MediQSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ProfileViewModel : ViewModel() {
    private val api = RetrofitClient.apiService

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _authError = MutableStateFlow(false)
    val authError: StateFlow<Boolean> = _authError.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    private fun handleAuthFailure() {
        _authError.value = true
        MediQSession.signOut()
        RetrofitClient.accessToken = null
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _authError.value = false
            try {
                _profile.value = api.getProfile()
            } catch (e: Exception) {
                val isAuth = e is HttpException && e.code() == 401
                if (isAuth) {
                    handleAuthFailure()
                } else {
                    _error.value = e.message ?: "Failed to load profile"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(draft: ProfileDraft) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _updateSuccess.value = false
            try {
                _profile.value = api.updateProfile(draft)
                _updateSuccess.value = true
            } catch (e: Exception) {
                val isAuth = e is HttpException && e.code() == 401
                if (isAuth) {
                    handleAuthFailure()
                } else {
                    _error.value = e.message ?: "Failed to update profile"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }
}