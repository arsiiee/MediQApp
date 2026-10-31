package com.example.mediq.ui.viewmodel

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediq.ui.session.MediQSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Biometric unlock gate.
 *
 * Job: prove the person holding the phone is the enrolled owner before the
 * app opens. On success the caller navigates to Home; on failure or
 * cancellation the screen shows an error and offers retry.
 *
 * BiometricPrompt is callback-driven; this ViewModel turns those callbacks
 * into a [StateFlow] the screen collects.
 */
class BiometricViewModel private constructor(
    private val context: Context
) : ViewModel() {

    private val executor: Executor = Executors.newSingleThreadExecutor()

    private val _uiState = MutableStateFlow<BiometricUiState>(BiometricUiState.Idle)
    val uiState: StateFlow<BiometricUiState> = _uiState.asStateFlow()

    private var prompt: BiometricPrompt? = null

    init {
        val activity = context as FragmentActivity
        prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errorString: CharSequence) {
                    super.onAuthenticationError(errorCode, errorString)
                    viewModelScope.launch {
                        _uiState.value = BiometricUiState.Error(errorString.toString())
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModelScope.launch {
                        _uiState.value = BiometricUiState.Success
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    viewModelScope.launch {
                        _uiState.value = BiometricUiState.Error("That fingerprint did not match. Try again.")
                    }
                }
            }
        )
    }

    /**
     * Starts the biometric prompt. Safe to call repeatedly; each call cancels
     * any in-flight prompt first.
     */
    fun authenticate() {
        viewModelScope.launch {
            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("MediQ")
                .setSubtitle("MediQ requires your biometrics to proceed to the app")
                .setDescription("Scan your enrolled fingerprint or face to unlock the app.")
                .setNegativeButtonText("Cancel")
                .build()

            _uiState.value = BiometricUiState.Authenticating

            prompt?.authenticate(promptInfo)
        }
    }

    fun cancel() {
        viewModelScope.launch {
            _uiState.value = BiometricUiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    /** Factory so screens can obtain an instance via [viewModel()]. */
    class Factory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BiometricViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BiometricViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
}

sealed class BiometricUiState {
    object Idle : BiometricUiState()
    object Authenticating : BiometricUiState()
    object Success : BiometricUiState()
    data class Error(val message: String) : BiometricUiState()
}
