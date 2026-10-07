package com.example.mediq.ui.feature.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.SignInRequest
import com.example.mediq.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
)

/**
 * Handles the sign-in form and delegates to [AuthRepository.signIn].
 *
 * On success [signedIn] becomes true; the screen observes this and navigates
 * to Home, popping the splash off the back stack. On failure [error] is set
 * for display below the login button.
 */
class SignInViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(value: String) =
        _uiState.update { it.copy(username = value, error = null) }

    fun onPasswordChanged(value: String) =
        _uiState.update { it.copy(password = value, error = null) }

    fun signIn() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Username and password are required.") }
            return
        }
        if (state.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                authRepository.signIn(SignInRequest(state.username, state.password))
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, signedIn = true) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        // The server writes this for a patient to read, and
                        // deliberately gives the same message for an unknown
                        // username and a wrong password so neither can be
                        // probed. Show it rather than a generic string — and
                        // never `e.message` from a framework exception, which
                        // would read "HTTP 401 ".
                        error = (e as? ApiFailure)?.message
                            ?: "Sign-in failed. Check your username and password.",
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SignInViewModel(AppContainer.authRepository) }
        }
    }
}
