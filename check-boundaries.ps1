# check-boundaries.ps1 — architecture boundary enforcement for MediQ.
#
# Enforces the rules in .mdfiles/AGENTS.md that were, until this existed,
# prose only. Each one had already been violated or silently ignored:
#
#   1. domain/ must not import Android or Compose. `:server` compiles that
#      directory as `sharedDomain`, so an Android import breaks the server
#      build — the compiler catches it, but only after you wait for a build.
#   2. ui/ must not import data/. Repositories are the only thing that should
#      know the data source (AGENTS.md, "Repositories are the only place that
#      knows the data source").
#   3. Every Retrofit*Repository call to `api.` must sit inside `call { }`.
#      This is the rule whose absence produced the bug where the client showed
#      "HTTP 401" instead of the server's message, with a fully green build.
#
# Exit codes match the floor-guard contract: 0 clean, 1 violation, 2 could not
# run. A 2 must never be read as a 0.
#
# Usage:
#   .\check-boundaries.ps1              # whole tree
#   .\check-boundaries.ps1 -ChangedOnly # only files in the working diff

param([switch]$ChangedOnly)

$ErrorActionPreference = 'Stop'

$root = 'app/src/main/java/com/example/mediq'
if (-not (Test-Path $root)) {
    Write-Error "check-boundaries: '$root' not found. Run from the repo root."
    exit 2
}

function Get-KotlinFiles {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return @() }
    return Get-ChildItem -Path $Path -Recurse -Filter *.kt -File
}

# ---------------------------------------------------------------------------
# Which files to look at.
#
# Whole-tree by default: these rules are about the current state of the code,
# not about what this change happened to touch. That is deliberate — the
# violation being guarded against is "added a plain api.foo() call", which a
# diff-scoped check only catches on the diff that introduced it and never on
# the file it was added to afterwards.
# ---------------------------------------------------------------------------

$allFiles = @()
foreach ($dir in @("$root/domain", "$root/ui", "$root/data/repository")) {
    $allFiles += Get-KotlinFiles $dir
}

if ($ChangedOnly) {
    $changed = @()
    try {
        $names = @(
            (git diff --name-only HEAD 2>$null)
            (git ls-files --others --exclude-standard 2>$null)
        ) | Where-Object { $_ -and $_.Trim() -ne '' }
        foreach ($n in $names) {
            $p = Join-Path (Get-Location) $n
            if ((Test-Path $p) -and $p.EndsWith('.kt')) { $changed += (Get-Item $p) }
        }
    } catch {
        Write-Error 'check-boundaries: not a git repository, cannot scope to the diff.'
        exit 2
    }
    $wanted = $changed | ForEach-Object { $_.FullName }
    $allFiles = $allFiles | Where-Object { $wanted -contains $_.FullName }
}

if ($allFiles.Count -eq 0) {
    Write-Output 'check-boundaries: no files to check'
    exit 0
}

$violations = @()

function Add-Violation {
    param([string]$Rule, [string]$File, [int]$Line, [string]$Detail)
    $rel = $File -replace [regex]::Escape((Get-Location).Path + '\'), ''
    $script:violations += [pscustomobject]@{ Rule = $Rule; File = $rel; Line = $Line; Detail = $Detail }
}

foreach ($file in $allFiles) {
    $lines = Get-Content $file.FullName
    $isDomain = $file.FullName -like "*\domain\*"
    $isUi     = $file.FullName -like "*\ui\*"
    $isRepo   = $file.Name -like 'Retrofit*Repository.kt'

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        $n = $i + 1

        # --- Rule 1: domain/ imports nothing Android or Compose -------------
        if ($isDomain -and $line -match '^\s*import\s+(android|androidx\.compose)') {
            Add-Violation 'domain-imports-android' $file.FullName $n $line.Trim()
        }

        # --- Rule 2: ui/ must not reference data/ ---------------------------
        # The offending import can sit on a different line than the symbol it
        # brings in, so this matches the import statement itself.
        if ($isUi -and $line -match '^\s*import\s+com\.example\.mediq\.data\.') {
            Add-Violation 'ui-imports-data' $file.FullName $n $line.Trim()
        }

        # --- Rule 3: no bare api.* call in a repository ---------------------
        #
        # The wrapper appears either on the same line (`call { api.foo() }`) or
        # on one of the lines above it, because the call is often multi-line.
        # A single-line check missed the first form and cried wolf on 15 call
        # sites, so this checks the current line first and then scans back a
        # short window. Scanning back stops at another `api.` call, because
        # that means the wrapper cannot be in between.
        if ($isRepo -and $line -match '\bapi\.\w+\s*\(') {
            $wrapped = $line -match '\bcall\s*\{'
            if (-not $wrapped) {
                for ($j = $i - 1; $j -ge 0 -and $j -ge ($i - 3); $j--) {
                    $prev = $lines[$j]
                    if ($prev -match '\bcall\s*\{') { $wrapped = $true; break }
                    if ($prev -match '\bapi\.\w+\s*\(') { break }
                }
            }
            if (-not $wrapped) {
                Add-Violation 'api-call-not-wrapped' $file.FullName $n $line.Trim()
            }
        }
    }
}

if ($violations.Count -eq 0) {
    Write-Output "check-boundaries: clean ($($allFiles.Count) files)"
    exit 0
}

Write-Output "check-boundaries: $($violations.Count) violation(s) in $($allFiles.Count) files"
Write-Output ''
foreach ($v in $violations) {
    Write-Output "  [$($v.Rule)] $($v.File):$($v.Line)"
    Write-Output "      $($v.Detail)"
}
Write-Output ''
Write-Output 'Rules and their reasons are in CONSTRAINTS.md.'
exit 1
