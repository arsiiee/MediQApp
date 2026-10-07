package com.example.mediq.ui.feature.debug.seededdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.repository.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SeededDataUiState(
    val doctors: LoadState<List<Doctor>> = LoadState.Loading,
)

/**
 * Reads what `MEDIQ_SEED_DEMO=true` actually inserted, so a developer can answer
 * "is the backend working?" from the device instead of from a console window that
 * has already scrolled the evidence away.
 *
 * ## Temporary
 *
 * **Delete this, and nothing outside this file.** To remove the whole screen:
 *
 * 1. `SeededDataViewModel.kt` and `SeededDataScreen.kt`
 * 2. `Screen.SeededData` in `ui/navigation/Routes.kt`
 * 3. the one `composable(Screen.SeededData.route)` in `MediQNavHost.kt`
 * 4. the 2-second long-press on the splash wordmark in `SplashScreen.kt`
 * 5. `SeededDataViewModelTest.kt`
 *
 * That is the whole feature, and `tasks/todo.md` Task 4 proves it by doing it on
 * a scratch branch rather than asserting it. Nothing else may depend on this
 * class — a second consumer turns a five-minute deletion into an archaeology
 * project, which is how temporary code outlives its purpose.
 *
 * ## Why nothing here is hardcoded
 *
 * There is no fallback list, no placeholder row, and no cached copy, and that is
 * the load-bearing property of the whole screen rather than a style preference.
 * A hardcoded list renders identically whether the backend is alive or dead, so
 * the screen would report a healthy clinic while proving nothing — a green light
 * wired to nothing. `AGENTS.md` forbids fabricated doctors in `app/` for the same
 * reason: invented clinicians with plausible licence numbers get mistaken for
 * real ones. `DemoData.kt:19` keeps them labelled on the server side, and this
 * class's job is to report what the server actually has.
 *
 * `SeededDataViewModelTest` holds the state to the repository's answer verbatim,
 * so a fallback added later fails the suite rather than quietly making the screen
 * lie.
 *
 * ## What a good read here still does not prove
 *
 * `GET /doctors` is a public route with no bearer token, and this screen is
 * reached pre-sign-in — so it exercises reachability, the database, and the
 * `doctors` to `clinic_hours` join, and nothing about auth or any write. The
 * screen says so on itself; a developer reading "backend OK" must not infer more
 * than one route proved.
 *
 * ## The empty list is ambiguous, on purpose
 *
 * An unreachable backend is `Success(emptyList())` here, per the `AGENTS.md` rule
 * for list reads, and `RetrofitDoctorRepository` absorbs a connection failure and
 * returns an empty `Paged` before this class ever sees it. So zero rows means
 * either "the server is down" or "the seed did not run", and this layer cannot
 * tell them apart. The screen separates them by wording, in `ui/` alone —
 * deliberately not by changing the repository, where that mapping is correct for
 * every other screen.
 */
class SeededDataViewModel(
    private val doctorRepository: DoctorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeededDataUiState())
    val uiState: StateFlow<SeededDataUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { read() }
    }

    /**
     * Re-reads from the server.
     *
     * Not a cache invalidation: this screen exists to report what is true *now*,
     * and the usual reason to press it is that the server was restarted or the
     * seed was re-run. Caching the first answer would make a stale screen look
     * like a live one, which is the failure this screen is meant to catch.
     */
    fun refresh() {
        viewModelScope.launch { read() }
    }

    private suspend fun read() {
        _uiState.value = SeededDataUiState(doctors = LoadState.Loading)
        _uiState.value = SeededDataUiState(
            doctors = try {
                // `DoctorQuery()` with no filters on purpose: this screen reports
                // what the seed inserted, and a query carrying a filter could
                // return fewer rows than were created and read as a partial seed.
                //
                // `.items` drops `nextCursor`, so this shows the first page only.
                // The seed is three doctors and `MAX_PAGE_SIZE` is far larger, so
                // the page is the whole set today; if the seed ever grows past
                // the page size this screen would under-report, and that is worth
                // revisiting rather than hiding behind a second page of requests.
                LoadState.Success(doctorRepository.getDoctors(DoctorQuery()).items)
            } catch (e: BackendNotConnectedException) {
                // No backend is wired up at all. Nothing went wrong.
                LoadState.Success(emptyList())
            } catch (e: ApiFailure) {
                // `e.message` is the server's own sentence. Never a framework
                // message: Retrofit's `HttpException` reports `"HTTP 500 "`, and
                // a verification screen showing framework text would be reporting
                // on itself instead of the backend.
                LoadState.Error(e.message)
            } catch (e: Exception) {
                LoadState.Error("Couldn't read the seeded data. Try again in a moment.")
            }
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SeededDataViewModel(AppContainer.doctorRepository) }
        }
    }
}
