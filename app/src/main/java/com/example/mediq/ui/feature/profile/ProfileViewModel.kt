package com.example.mediq.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Sex
import com.example.mediq.domain.repository.AuthRepository
import com.example.mediq.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * The edit form: the fields as typed, one error slot per validated field, and
 * the state of the save in flight.
 *
 * `fullName` carries no error slot for trimming — the values are trimmed on the
 * way out, in [ProfileViewModel.saveProfile], so what the server stores and what
 * the form shows cannot disagree by a stray space.
 */
data class ProfileEditState(
    val fullName: String = "",
    val fullNameError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val mobileNumber: String = "",
    val mobileError: String? = null,
    /** Typed in ISO `yyyy-MM-dd` — the form the server and `TokenStore` use. */
    val dateOfBirth: String = "",
    val dateOfBirthError: String? = null,
    /** Null is "not specified", which the server accepts. */
    val sex: Sex? = null,
    val address: String = "",
    val isSaving: Boolean = false,
    /** The server's own sentence for a refused save; see the error contract. */
    val saveError: String? = null,
) {
    /** True when any field is refusing the value in it. */
    val hasFieldErrors: Boolean
        get() = fullNameError != null ||
            emailError != null ||
            mobileError != null ||
            dateOfBirthError != null
}

data class ProfileUiState(
    val session: LoadState<AuthSession?> = LoadState.Loading,
    val edit: ProfileEditState = ProfileEditState(),
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

    // --- The edit form --------------------------------------------------------

    fun onEditFullName(value: String) = updateEdit { it.copy(fullName = value, fullNameError = null) }

    fun onEditEmail(value: String) = updateEdit { it.copy(email = value, emailError = null) }

    fun onEditMobile(value: String) = updateEdit { it.copy(mobileNumber = value, mobileError = null) }

    fun onEditDateOfBirth(value: String) =
        updateEdit { it.copy(dateOfBirth = value, dateOfBirthError = null) }

    fun onEditSex(value: Sex?) = updateEdit { it.copy(sex = value) }

    fun onEditAddress(value: String) = updateEdit { it.copy(address = value) }

    /**
     * Validates, then saves against the signed-in account's own row.
     *
     * The merge is over the loaded profile, so the fields the form does not
     * carry — id, username, role — cannot be blanked by a save, and a field the
     * patient never touched keeps the value it had. The state is afterwards
     * rewritten from the **server's** answer, not from the typed values, and the
     * cached session is rewritten with it: `currentSession()` reads the store,
     * so without that write the profile screen would keep showing the old name.
     *
     * [onSaved] is a callback rather than a flag on the state, the same shape as
     * [signOut]'s: a flag would still be set the next time the form opened, and
     * the screen would finish before it started.
     */
    fun saveProfile(onSaved: () -> Unit) {
        val session = (uiState.value.session as? LoadState.Success)?.data ?: return
        val typed = uiState.value.edit
        val validated = validate(typed)
        if (validated.hasFieldErrors) {
            // A field the server would only refuse, and the patient would wait
            // for the round trip to find out. Nothing leaves the device.
            _uiState.value = _uiState.value.copy(edit = validated)
            return
        }
        _uiState.value = _uiState.value.copy(edit = validated.copy(isSaving = true, saveError = null))

        viewModelScope.launch {
            val merged = session.profile.copy(
                fullName = typed.fullName.trim(),
                email = typed.email.trim(),
                mobileNumber = typed.mobileNumber.trim(),
                dateOfBirth = LocalDate.parse(typed.dateOfBirth.trim()),
                sex = typed.sex,
                address = typed.address.trim().ifBlank { null },
            )
            try {
                val updated = profileRepository.updateProfile(merged)
                authRepository.updateCachedProfile(updated)
                _uiState.value = _uiState.value.copy(
                    session = LoadState.Success(session.copy(profile = updated)),
                    edit = _uiState.value.edit.copy(isSaving = false, saveError = null),
                )
                onSaved()
            } catch (e: ApiFailure) {
                _uiState.value = _uiState.value.copy(
                    edit = _uiState.value.edit.copy(isSaving = false, saveError = e.message),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    edit = _uiState.value.edit.copy(isSaving = false, saveError = SAVE_FAILED),
                )
            }
        }
    }

    private fun updateEdit(transform: (ProfileEditState) -> ProfileEditState) {
        _uiState.value = _uiState.value.copy(edit = transform(_uiState.value.edit))
    }

    private fun validate(edit: ProfileEditState): ProfileEditState {
        val dateOfBirth = parseDate(edit.dateOfBirth)
        return edit.copy(
            fullNameError = if (edit.fullName.isBlank()) "Enter your full name." else null,
            // Blank is a real answer: the server treats an absent email as
            // "none on file", and the profile screen already draws that state.
            emailError = if (edit.email.isNotBlank() && !EMAIL.matches(edit.email.trim())) {
                "Enter a valid email address."
            } else {
                null
            },
            mobileError = if (!MOBILE.matches(edit.mobileNumber.trim())) {
                "Enter a valid mobile number, e.g. +639171234567."
            } else {
                null
            },
            dateOfBirthError = when {
                dateOfBirth == null -> "Use the format YYYY-MM-DD, e.g. 1995-06-15."
                dateOfBirth.isAfter(LocalDate.now()) -> "Date of birth can't be in the future."
                else -> null
            },
        )
    }

    private suspend fun load() {
        // Only the read is reset. The form keeps whatever is in it: a refresh
        // must not wipe a draft the patient is halfway through typing.
        _uiState.value = _uiState.value.copy(session = LoadState.Loading)
        val session = try {
            LoadState.Success(authRepository.currentSession())
        } catch (e: BackendNotConnectedException) {
            LoadState.Success(null)
        } catch (e: ApiFailure) {
            LoadState.Error(e.message)
        } catch (e: Exception) {
            // Defence-in-depth: TokenStore validates blobs on read, so malformed
            // sessions clear themselves and return null instead of throwing here.
            LoadState.Error("Couldn't load your profile. Try again in a moment.")
        }
        _uiState.value = _uiState.value.copy(
            session = session,
            edit = prefilled(session),
        )
    }

    /**
     * Seeds the form from the loaded profile, once.
     *
     * The guard is "the form is untouched", not "this is the first load": a
     * refresh after an edit would otherwise restore the server's older values
     * over a draft, and a refresh before one still wants the fresh session.
     */
    private fun prefilled(session: LoadState<AuthSession?>): ProfileEditState {
        val current = _uiState.value.edit
        val profile = (session as? LoadState.Success)?.data?.profile ?: return current
        if (current != ProfileEditState()) return current
        return ProfileEditState(
            fullName = profile.fullName,
            email = profile.email,
            mobileNumber = profile.mobileNumber,
            dateOfBirth = profile.dateOfBirth.toString(),
            sex = profile.sex,
            address = profile.address.orEmpty(),
        )
    }

    private fun parseDate(text: String): LocalDate? =
        try {
            LocalDate.parse(text.trim())
        } catch (e: DateTimeParseException) {
            null
        }

    companion object {
        /**
         * A shape check, not a deliverability check — the server is the only
         * thing that can say an address exists.
         */
        private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        /** E.164-ish: an optional `+`, then 7 to 15 digits. */
        private val MOBILE = Regex("^\\+?[0-9]{7,15}$")

        private const val SAVE_FAILED = "Couldn't save your changes. Try again in a moment."

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
