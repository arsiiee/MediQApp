package com.example.mediq.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.data.repository.BackendNotConnectedException
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.repository.AuthRepository
import com.example.mediq.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val session: LoadState<AuthSession?> = LoadState.Loading,
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            // Best effort: navigate away even if the server call fails, so a
            // user is never stuck on a signed-in screen they can't leave.
            runCatching { authRepository.signOut() }
            onDone()
        }
    }

    private suspend fun load() {
        _uiState.value = ProfileUiState(session = LoadState.Loading)
        _uiState.value = ProfileUiState(
            session = try {
                LoadState.Success(authRepository.currentSession())
            } catch (e: BackendNotConnectedException) {
                LoadState.Success(null)
            } catch (e: ApiFailure) {
                LoadState.Error(e.message)
            } catch (e: Exception) {
                LoadState.Error("Couldn't load your profile. Try again in a moment.")
            }
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    profileRepository = AppContainer.profileRepository,
                    authRepository = AppContainer.authRepository,
                )
            }
        }
    }
}