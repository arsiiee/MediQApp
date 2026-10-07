package com.example.mediq.domain.model

/**
 * Raised when no backend is wired up behind a repository at all.
 *
 * This is the placeholder-backend case — `EmptyRepositories.kt` throws it — and
 * it is distinct from [ApiFailure], which means a real backend was reached and
 * refused. Both mean the same thing to a screen, though: there is nothing to
 * show and nothing went wrong, so reads map it to an empty state rather than an
 * error.
 *
 * It lives in `domain/model/` rather than `data/repository/` because ViewModels
 * catch it. They must not import from `data/` — repositories are the only place
 * that should know the data source — and seven of them were breaking that rule
 * to catch this one type. `check-boundaries.ps1` enforces the boundary; the
 * compiler cannot, because the import compiles fine.
 */
class BackendNotConnectedException : Exception(
    "No backend is wired up yet. This repository returns nothing on purpose."
)
