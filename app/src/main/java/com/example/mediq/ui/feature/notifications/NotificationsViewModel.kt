package com.example.mediq.ui.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Notification
import com.example.mediq.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: LoadState<List<Notification>> = LoadState.Loading,
)

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        _uiState.value = NotificationsUiState(notifications = LoadState.Loading)
        _uiState.value = NotificationsUiState(
            notifications = try {
                LoadState.Success(notificationRepository.getNotifications().items)
            } catch (e: BackendNotConnectedException) {
                LoadState.Success(emptyList())
            } catch (e: ApiFailure) {
                LoadState.Error(e.message)
            } catch (e: Exception) {
                LoadState.Error("Couldn't load notifications. Try again in a moment.")
            }
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { NotificationsViewModel(AppContainer.notificationRepository) }
        }
    }
}