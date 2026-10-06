# README

The project README lives at the **repository root**, in `../README.md`. GitHub
only renders a README from the root, so keeping it there means the repo landing
page and the working copy are the same file.

This folder holds the rest of the standing documentation:

| File | What it is |
|---|---|
| `AGENTS.md` | Orientation for a new session or contributor: build environment quirks, the layered architecture, conventions, and the reasoning behind each rule. |
| `CONSTRAINTS.md` | The quality bar as numbers, with a command per rule and a ratchet per metric that must not worsen. |
| `CHANGELOG.md` | What changed, grouped by impact rather than by commit. |
| `check-boundaries.ps1` | Enforces the architecture boundaries `AGENTS.md` states in prose. |
| `check-contrast.ps1` | Enforces WCAG AA contrast and the raw-colour ratchet. |
| `MediQ_App_Development_Spec.md.pdf` | The original product spec this was built from. |

Run both check scripts from the repository root, since they resolve paths
relative to the working directory:

```powershell
.\.mdfiles\check-boundaries.ps1
.\.mdfiles\check-contrast.ps1
```

Per-work planning lives in `tasks/` at the repo root: `plan.md` is the design
record, `todo.md` is the task checklist with acceptance criteria.
