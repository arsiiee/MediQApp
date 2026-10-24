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
import java.io.IOException

class AuthViewModel : ViewModel() {
    private val api = RetrofitClient.apiService

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var currentUser: UserProfile? = null

    /**
     * Checks a pair with the server.
     *
     * Returns the account it authenticated, or null when it did not — the
     * reason is left in [uiState] for the screen to say.
     */
    suspend fun signIn(username: String, password: String): UserProfile? {
        _uiState.value = AuthUiState.Loading
        return try {
            val session = api.signIn(SignInRequest(username, password))
            accessToken = session.accessToken
            refreshToken = session.refreshToken
            currentUser = session.profile
            RetrofitClient.accessToken = session.accessToken
            MediQSession.setTokens(session.accessToken, session.refreshToken)
            _uiState.value = AuthUiState.Success(session.profile)
            session.profile
        } catch (e: HttpException) {
            /*
             * A rejected pair is the patient's mistake; anything else is
             * ours. Saying which one it was is what stops the screen from
             * blaming the patient for a broken connection.
             */
            _uiState.value = if (e.code() == 401) {
                AuthUiState.InvalidCredentials
            } else {
                AuthUiState.Error("We could not reach MediQ. Check your connection and try again.")
            }
            null
        } catch (e: IOException) {
            _uiState.value = AuthUiState.Error("We could not reach MediQ. Check your connection and try again.")
            null
        } catch (e: Exception) {
            _uiState.value = AuthUiState.Error(e.message ?: "Sign in failed")
            null
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val session = api.register(request)
                accessToken = session.accessToken
                refreshToken = session.refreshToken
                currentUser = session.profile
                RetrofitClient.accessToken = session.accessToken
                MediQSession.setTokens(session.accessToken, session.refreshToken)
                _uiState.value = AuthUiState.Success(session.profile)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Registration failed")
            }
        }
    }

    /**
     * Asks the server to send a one-time code to a contact.
     *
     * Returns true when the code went out. The code itself is on the server,
     * which has no SMS gateway behind it yet.
     */
    suspend fun requestOtp(contact: String, purpose: OtpPurpose): Boolean = try {
        api.requestOtp(RequestOtpRequest(contact = contact.trim(), purpose = purpose))
        true
    } catch (e: HttpException) {
        _uiState.value = AuthUiState.Error(e.message ?: "We could not send the code.")
        false
    } catch (e: IOException) {
        _uiState.value = AuthUiState.Error("We could not reach MediQ. Check your connection and try again.")
        false
    } catch (e: Exception) {
        _uiState.value = AuthUiState.Error(e.message ?: "We could not send the code.")
        false
    }

    /**
     * Checks a one-time code.
     *
     * Returns the session the server issued when the code was right, or null
     * when it was not — the reason is left in [uiState].
     */
    suspend fun verifyOtp(contact: String, code: String, purpose: OtpPurpose): AuthSession? = try {
        val session = api.verifyOtp(
            VerifyOtpRequest(contact = contact.trim(), code = code.trim(), purpose = purpose)
        )
        RetrofitClient.accessToken = session.accessToken
        _uiState.value = AuthUiState.Success(session.profile)
        session
    } catch (e: HttpException) {
        _uiState.value = AuthUiState.Error(otpMessage(e))
        null
    } catch (e: IOException) {
        _uiState.value = AuthUiState.Error("We could not reach MediQ. Check your connection and try again.")
        null
    } catch (e: Exception) {
        _uiState.value = AuthUiState.Error(e.message ?: "We could not check the code.")
        null
    }

    /**
     * Sets a new password for the account the code proved.
     *
     * The session that came back from [verifyOtp] carries the token this
     * call needs.
     */
    suspend fun updatePassword(newPassword: String): Boolean = try {
        api.updatePassword(UpdatePasswordRequest(newPassword = newPassword))
        true
    } catch (e: HttpException) {
        _uiState.value = AuthUiState.Error(otpMessage(e))
        false
    } catch (e: IOException) {
        _uiState.value = AuthUiState.Error("We could not reach MediQ. Check your connection and try again.")
        false
    } catch (e: Exception) {
        _uiState.value = AuthUiState.Error(e.message ?: "We could not save your new password.")
        false
    }

    /**
     * What the server said about a rejected one-time code.
     *
     * The server explains it, so the screen does not have to guess.
     */
    private fun otpMessage(e: HttpException): String {
        val body = e.response()?.errorBody()?.string().orEmpty()
        val message = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
        return message ?: when (e.code()) {
            400 -> "That code is not right. Check the six digits and try again."
            404 -> "We have no account for that number."
            else -> "We could not check the code. Try again."
        }
    }

    fun getAccessToken(): String? = accessToken
    fun getCurrentUser(): UserProfile? = currentUser
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object InvalidCredentials : AuthUiState()
    data class Success(val profile: UserProfile) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
