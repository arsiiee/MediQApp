# check-contrast.ps1 -- colour-contrast enforcement for MediQ.
#
# Why this exists: on 2026-10-06 a white-text audit found 25 `Color.Gray` call
# sites at 3.95:1, a dark colour scheme that rendered white text on hardcoded
# white surfaces at 1.00:1, and an amber status chip at 1.85:1. Every one passed
# a green build and a green test suite, because nothing measured contrast.
# CONSTRAINTS.md already records that "a rule stated in prose, never checked" is
# this repo's recurring failure; this script is the check.
#
# This script PARSES the real token values out of Color.kt and the real status
# chip pairs out of AppointmentsScreen.kt. It deliberately does not keep its own
# copy of the hex codes: a version that measured a hardcoded list passed clean
# while MediQTextSecondary was reverted to a failing #757575, which is a check
# that lies. Change a token, and this reports the new number.
#
# Ratios use the WCAG 2.1 relative-luminance formula. Run after touching
# anything in core/designsystem/theme/.
#
# Exit codes: 0 clean, 1 violation, 2 could not run.

param()

$ErrorActionPreference = 'Stop'

$src = 'app/src/main/java/com/example/mediq'
if (-not (Test-Path "$src/core/designsystem/theme/Color.kt")) {
    Write-Error "check-contrast: colour sources not found. Run from the repo root."
    exit 2
}

function Get-Linear([double]$c) {
    $s = $c / 255.0
    if ($s -le 0.03928) { return $s / 12.92 }
    return [math]::Pow((($s + 0.055) / 1.055), 2.4)
}

function Get-Luminance([string]$hex) {
    $h = $hex.TrimStart('#')
    $r = [Convert]::ToInt32($h.Substring(0, 2), 16)
    $g = [Convert]::ToInt32($h.Substring(2, 2), 16)
    $b = [Convert]::ToInt32($h.Substring(4, 2), 16)
    return 0.2126 * (Get-Linear $r) + 0.7152 * (Get-Linear $g) + 0.0722 * (Get-Linear $b)
}

function Get-Contrast([string]$fg, [string]$bg) {
    $l1 = Get-Luminance $fg
    $l2 = Get-Luminance $bg
    return [math]::Round(([math]::Max($l1, $l2) + 0.05) / ([math]::Min($l1, $l2) + 0.05), 2)
}

$violations = @()

# ---------------------------------------------------------------------------
# 1. Token pairs, read from Color.kt.
#    'min' is the WCAG threshold for the role: 4.5 for normal body text,
#    3.0 for large text and non-text (WCAG 1.4.11).
# ---------------------------------------------------------------------------

$colorSrc = Get-Content "$src/core/designsystem/theme/Color.kt" -Raw
$themeSrc = Get-Content "$src/core/designsystem/theme/Theme.kt" -Raw

function Get-Token([string]$name, [string]$source) {
    $m = [regex]::Match($source, "val\s+$name\s*=\s*Color\(0x[0-9A-Fa-f]{8}\)")
    if (-not $m.Success) {
        $script:violations += "token       $name not found in the colour source"
        return $null
    }
    return '#' + (Get-Rgb $m.Value)
}

# Compose literals are 0xAARRGGBB: drop the leading alpha byte.
function Get-Rgb([string]$literal) {
    return $literal.Substring($literal.IndexOf('0x') + 4, 6)
}

# The dark/light neutrals the schemes depend on live in Theme.kt as inline
# Color(0x...) literals, so read those from there too.
function Get-ThemeColour([string]$name, [string]$scheme) {
    $block = [regex]::Match($themeSrc, "(?s)val $scheme = (?:dark|light)ColorScheme\((.*?)\n\)")
    if (-not $block.Success) {
        $script:violations += "scheme      $scheme not found in Theme.kt"
        return $null
    }
    $m = [regex]::Match($block.Groups[1].Value, "$name\s*=\s*(?:Color\(0x[0-9A-Fa-f]{8}\)|[A-Za-z][A-Za-z0-9]*)")
    if (-not $m.Success) {
        $script:violations += "scheme      $name not found in $scheme"
        return $null
    }
    $v = $m.Value
    if ($v -match 'Color\(0x[0-9A-Fa-f]{8}\)') { return '#' + (Get-Rgb $v) }
    # It is a token reference; resolve it against Color.kt.
    return Get-Token ($v -replace "$name\s*=\s*", '') $colorSrc
}

$t = @{}
foreach ($n in @(
    'MediQGreen', 'MediQLightGreen', 'MediQDarkGreen', 'MediQOnBrand',
    'MediQAccentLight', 'MediQAccentDark',
    'MediQTextPrimary', 'MediQTextSecondary', 'MediQTextSecondaryDark',
    'MediQBackground', 'MediQSurface', 'MediQSurfaceVariantDark', 'MediQContainerDark',
    'MediQOutline', 'MediQOutlineDark',
    'MediQError', 'MediQErrorDark', 'MediQSuccess', 'MediQWarning'
)) { $t[$n] = Get-Token $n $colorSrc }

$dark  = @{}
$light = @{}
foreach ($n in @('background', 'surface', 'surfaceVariant', 'primary', 'error')) {
    $dark[$n]  = Get-ThemeColour $n 'DarkColorScheme'
    $light[$n] = Get-ThemeColour $n 'LightColorScheme'
}

# Each row: label, foreground, background, minimum ratio.
$rows = @(
    @{ n = 'light body text';            fg = $t.MediQTextPrimary;      bg = $light.background; min = 4.5 },
    @{ n = 'light secondary text';       fg = $t.MediQTextSecondary;    bg = $light.background; min = 4.5 },
    @{ n = 'light secondary on surface'; fg = $t.MediQTextSecondary;    bg = $light.surface;    min = 4.5 },
    @{ n = 'light secondary on raised';  fg = $t.MediQTextSecondary;    bg = $light.surfaceVariant; min = 4.5 },
    @{ n = 'light accent on background'; fg = $t.MediQAccentLight;      bg = $light.background; min = 4.5 },
    @{ n = 'light accent on surface';    fg = $t.MediQAccentLight;      bg = $light.surface;    min = 4.5 },
    @{ n = 'light accent on raised';     fg = $t.MediQAccentLight;      bg = $light.surfaceVariant; min = 4.5 },
    @{ n = 'light error on raised';      fg = $light.error;             bg = $light.surfaceVariant; min = 4.5 },
    @{ n = 'dark body text';             fg = $t.MediQOnBrand;          bg = $dark.surface;     min = 4.5 },
    @{ n = 'dark secondary text';        fg = $t.MediQTextSecondaryDark; bg = $dark.surface;    min = 4.5 },
    @{ n = 'dark secondary on raised';   fg = $t.MediQTextSecondaryDark; bg = $dark.surfaceVariant; min = 4.5 },
    @{ n = 'dark accent on background';  fg = $t.MediQAccentDark;       bg = $dark.background;  min = 4.5 },
    @{ n = 'dark accent on surface';     fg = $t.MediQAccentDark;       bg = $dark.surface;     min = 4.5 },
    @{ n = 'dark accent on raised';      fg = $t.MediQAccentDark;       bg = $dark.surfaceVariant; min = 4.5 },
    @{ n = 'dark error on background';   fg = $dark.error;              bg = $dark.background;  min = 4.5 },
    @{ n = 'dark error on raised';       fg = $dark.error;              bg = $dark.surfaceVariant; min = 4.5 },
    @{ n = 'brand button label';         fg = $t.MediQOnBrand;          bg = $t.MediQGreen;     min = 4.5 },
    @{ n = 'brand label on light tint';  fg = $t.MediQAccentLight;      bg = $t.MediQLightGreen; min = 4.5 },
    @{ n = 'brand label on deep brand';  fg = $t.MediQOnBrand;          bg = $t.MediQDarkGreen; min = 4.5 },
    @{ n = 'light outline';              fg = $t.MediQOutline;          bg = $light.background; min = 3.0 },
    @{ n = 'dark outline';               fg = $t.MediQOutlineDark;      bg = $dark.background;  min = 3.0 },
    @{ n = 'dark outline on surface';    fg = $t.MediQOutlineDark;      bg = $dark.surface;     min = 3.0 },
    @{ n = 'success on background';      fg = $t.MediQSuccess;          bg = $light.background; min = 4.5 },
    @{ n = 'warning on background';      fg = $t.MediQWarning;          bg = $light.background; min = 4.5 }
)

foreach ($r in $rows) {
    if ($null -eq $r.fg -or $null -eq $r.bg) { continue }
    $ratio = Get-Contrast $r.fg $r.bg
    if ($ratio -lt $r.min) {
        $violations += "contrast  $($r.n): $($r.fg) on $($r.bg) = $ratio, needs $($r.min)"
    }
}

# ---------------------------------------------------------------------------
# 2. Status chip pairs, read from AppointmentsScreen.kt.
# ---------------------------------------------------------------------------

$apptSrc = Get-Content "$src/ui/feature/appointments/AppointmentsScreen.kt" -Raw
$chips = [regex]::Matches($apptSrc, 'AppointmentStatus\.(\w+)\s*->\s*Color\(0x([0-9A-Fa-f]{8})\)\s*to\s*Color\(0x([0-9A-Fa-f]{8})\)')
if ($chips.Count -eq 0) {
    $violations += 'chips      no status chip pairs found in AppointmentsScreen.kt'
}
foreach ($c in $chips) {
    # The source reads `container to label`, so the label is the foreground.
    $bg = '#' + $c.Groups[2].Value.Substring(2, 6)
    $fg = '#' + $c.Groups[3].Value.Substring(2, 6)
    $ratio = Get-Contrast $fg $bg
    if ($ratio -lt 4.5) {
        $violations += "contrast  status chip $($c.Groups[1].Value): $fg on $bg = $ratio, needs 4.5"
    }
}

# ---------------------------------------------------------------------------
# 3. Ratchet on raw colour literals in ui/.
#    A raw literal is how the original 25 `Color.Gray` sites got there: nothing
#    ties them to a token, so nothing measures them. The floor is 4, all in
#    SplashScreen, where white sits on the brand green at 6.63:1 and the pairing
#    is correct in both modes. New ones must earn their place.
# ---------------------------------------------------------------------------

$ratchet = 4
$raw = Get-ChildItem "$src/ui" -Recurse -Filter *.kt |
    Select-String -Pattern 'Color\.(Gray|LightGray|DarkGray|White|Black)|Color\(0xFFD32F2F\)' |
    Where-Object { $_.Line -notmatch '^\s*//' }

if ($raw.Count -gt $ratchet) {
    $violations += "raw-colours  $($raw.Count) raw colour literals in ui/, ratchet is ${ratchet}:"
    foreach ($h in $raw) {
        $rel = $h.Path -replace [regex]::Escape((Get-Location).Path + '\'), ''
        $violations += "raw-colours    $rel`:$($h.LineNumber)  $($h.Line.Trim())"
    }
}

# ---------------------------------------------------------------------------
# 4. No screen may hardcode its page background or app bar. This is the exact
#    defect that made the dark scheme unusable: theme text resolving to white
#    and landing on a hardcoded white surface, at 1.00:1.
#
#    Scoped to page/app-bar backgrounds on purpose. A white *filled button* is
#    correct -- SplashScreen has one, labelled MediQGreen at 6.63:1 -- so
#    `containerColor = Color.White` is not matched here.
# ---------------------------------------------------------------------------

$hardcoded = Get-ChildItem "$src/ui" -Recurse -Filter *.kt |
    Select-String -Pattern '\.background\(Color\.White\)|topAppBarColors\([^)]*containerColor\s*=\s*Color\.White'

foreach ($h in $hardcoded) {
    $rel = $h.Path -replace [regex]::Escape((Get-Location).Path + '\'), ''
    $violations += "hardcoded-bg  $rel`:$($h.LineNumber)  $($h.Line.Trim())"
}

if ($violations.Count -eq 0) {
    Write-Output "check-contrast: clean ($($rows.Count) token pairs, $($chips.Count) status chips, raw-colour ratchet ${ratchet})"
    exit 0
}

Write-Output "check-contrast: $($violations.Count) violation(s)"
Write-Output ''
foreach ($v in $violations) { Write-Output "  $v" }
Write-Output ''
Write-Output 'Fix with a token from core/designsystem/theme/, then re-measure.'
exit 1