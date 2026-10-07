package com.example.mediq.ui.feature.notifications

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Notification
import com.example.mediq.domain.model.NotificationType
import com.example.mediq.fake.FakeNotificationRepository
import com.example.mediq.fake.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

/**
 * Covers the notifications list.
 *
 * The one claim worth guarding is the `BackendNotConnectedException` branch, and
 * `AGENTS.md` states the rule it has to follow: on a **list** read an unreachable
 * backend is `LoadState.Success(emptyList())`, not an error, because "no
 * notifications" and "no backend" look identical to the patient and an error
 * banner for a connection problem is noise.
 *
 * Where the rest of that rule lives is itself pinned here. The
 * `ApiFailure.isNetworkFailure` half — unreachable server becomes an empty page —
 * is `RetrofitNotificationRepository`'s job, not the ViewModel's, and
 * [an unreachable server is an error here, because the repository did not absorb
 * it] is what makes that split explicit. `RetrofitDoctorRepository` once caught
 * the exception type too broadly and rendered a 500 as "you have no doctors",
 * which is a lie about a patient's data.
 */
class NotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeNotificationRepository

    /**
     * Built on first use, not in `setUp`.
     *
     * This ViewModel loads in `init`, and `MainDispatcherRule` installs an
     * `UnconfinedTestDispatcher` that runs the `init` launch eagerly to
     * completion. Constructing it in `setUp` would therefore finish the load
     * *before* a test sets `repository.notifications`, and every assertion would
     * be reading a settled empty list instead of the fixture. `lazy` defers
     * construction to the first read, which is after the fixture is in place.
     */
    private val viewModel: NotificationsViewModel by lazy { NotificationsViewModel(repository) }

    @Before
    fun setUp() {
        repository = FakeNotificationRepository()
    }

    private fun notification(id: String, title: String = "Appointment confirmed") = Notification(
        id = id,
        type = NotificationType.APPOINTMENT_CONFIRMED,
        title = title,
        body = "Your visit with Dr. Rivera is booked.",
        createdAt = Instant.parse("2026-10-01T09:00:00Z"),
    )

    // --- Loading -------------------------------------------------------------

    @Test
    fun `the notifications reach the state`() {
        repository.notifications = listOf(notification("n-1"), notification("n-2"))

        val notifications = (viewModel.uiState.value.notifications as? LoadState.Success)?.data

        assertEquals(2, notifications?.size)
        assertEquals(listOf("n-1", "n-2"), notifications?.map { it.id })
    }

    @Test
    fun `a successful load does not start out as an error`() {
        repository.notifications = listOf(notification("n-1"))

        assertTrue(viewModel.uiState.value.notifications is LoadState.Success)
    }

    @Test
    fun `a list is read from the repository on creation`() {
        // `uiState` is touched first deliberately: it is the read that constructs
        // the ViewModel, and `init` is what calls the repository. Asserting on
        // `repository` first would evaluate before anything was ever built and
        // fail for a reason that has nothing to do with the behaviour.
        viewModel.uiState.value

        assertTrue("nothing would reach the repository without a read", repository.wasCalledAtAll)
    }

    // --- The unconnected backend is an empty list, not an error ---------------

    @Test
    fun `an unconnected backend is an empty list, not an error`() {
        repository.getNotificationsError = BackendNotConnectedException()

        val state = viewModel.uiState.value.notifications

        assertEquals(
            "an unreachable backend on a list read is the expected state",
            LoadState.Success(emptyList<Notification>()),
            state,
        )
    }

    @Test
    fun `an unreachable server is an error here, because the repository did not absorb it`() {
        // Which layer owns the network-failure rule, pinned deliberately.
        //
        // `AGENTS.md` says an unreachable server on a *list read* becomes an
        // empty `Paged` — and it is `RetrofitNotificationRepository` that does
        // that, catching `ApiFailure.isNetworkFailure` before the ViewModel ever
        // sees it. So by the time a repository throws `ApiFailure`, the server was
        // reachable and answered badly, and `LoadState.Error` is the honest
        // mapping.
        //
        // Writing this test as "an unreachable server is an empty list" is the
        // plausible mistake: it passes nowhere, because the ViewModel is not the
        // component that makes that decision.
        repository.getNotificationsError = ApiFailure(0, "network_error", "Couldn't reach the clinic.")

        val state = viewModel.uiState.value.notifications

        assertTrue(
            "an ApiFailure reaching the ViewModel means the repository let it through",
            state is LoadState.Error,
        )
    }

    @Test
    fun `an HTTP failure is an error, never an empty list`() {
        // The mirror of the unconnected-backend rule. A 500 must not read as
        // "you have no notifications" — the patient has notifications and the
        // server failed to send them.
        repository.getNotificationsError = ApiFailure(500, "internal_error", "Something went wrong.")

        val state = viewModel.uiState.value.notifications

        assertTrue(
            "a server fault must not be reported as an empty inbox",
            state is LoadState.Error,
        )
        assertEquals("Something went wrong.", (state as LoadState.Error).message)
    }

    @Test
    fun `a refused request is an error carrying the server's sentence`() {
        repository.getNotificationsError =
            ApiFailure(401, "unauthorized", "Your session has ended. Sign in again.")

        val state = viewModel.uiState.value.notifications

        assertEquals(
            "Your session has ended. Sign in again.",
            (state as? LoadState.Error)?.message,
        )
    }

    @Test
    fun `an unexpected failure falls back to a message written for a person`() {
        repository.getNotificationsError = IOException("failed to connect to /10.0.2.2:8099")

        val state = viewModel.uiState.value.notifications

        assertEquals(
            "Couldn't load notifications. Try again in a moment.",
            (state as? LoadState.Error)?.message,
        )
    }

    // --- Refresh -------------------------------------------------------------

    @Test
    fun `a refresh re-reads and replaces the list`() {
        repository.notifications = listOf(notification("n-1"))
        assertEquals(1, ((viewModel.uiState.value.notifications) as LoadState.Success).data.size)

        repository.notifications = listOf(notification("n-1"), notification("n-2"))
        viewModel.refresh()

        assertEquals(2, ((viewModel.uiState.value.notifications) as LoadState.Success).data.size)
    }

    @Test
    fun `a refresh recovers from a previous error`() {
        repository.getNotificationsError = ApiFailure(500, "internal_error", "Something went wrong.")
        assertTrue(preconditionErrorIsShowing())

        repository.getNotificationsError = null
        repository.notifications = listOf(notification("n-1"))
        viewModel.refresh()

        assertTrue(
            "a retry must be able to replace the error with a real list",
            viewModel.uiState.value.notifications is LoadState.Success,
        )
    }

    private fun preconditionErrorIsShowing() = viewModel.uiState.value.notifications is LoadState.Error
}
