package com.example.mediq.fake

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps `Dispatchers.Main` for a test dispatcher.
 *
 * Every ViewModel in this app dispatches through `viewModelScope`, which is
 * hardwired to `Dispatchers.Main`. On the JVM that dispatcher has no
 * implementation and throws `IllegalStateException` the moment a coroutine is
 * launched, so no ViewModel here can be unit tested without this rule.
 *
 * `UnconfinedTestDispatcher` rather than the standard one so a `viewModelScope`
 * launch runs its body eagerly to completion. These tests assert on state after
 * a repository call that never really suspends, and eager execution means they
 * read the settled state without every one of them having to call
 * `advanceUntilIdle()` first — which is the usual way an un-awaited coroutine
 * turns into a passing test that proves nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {

    override fun starting(description: Description) =
        Dispatchers.setMain(UnconfinedTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}
