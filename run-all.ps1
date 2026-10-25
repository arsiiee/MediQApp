# run-all.ps1 — Start all MediQ services at once
# Save this at repo root, then run:  .\run-all.ps1
#
# Delegates to run-mediq-server.ps1, which launches each server in its
# own window: Ktor (8099), Hono BFF (3000), Angular frontend (4200).

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "=== MediQ: Starting all services ===" -ForegroundColor Cyan
Write-Host ""

& "$repoRoot\run-mediq-server.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "run-mediq-server.ps1 exited with code $LASTEXITCODE." -ForegroundColor Red
    exit $LASTEXITCODE
}
