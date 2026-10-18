# End-to-end smoke test against a running server.
#
#   $env:MEDIQ_PORT="8099"
#   .\server\scripts\smoke.ps1
#
# Exercises the paths that unit tests cannot: the actual HTTP status of every
# route, and the booking conflict a second request must get. Writes nothing and
# changes no persistent state beyond the in-memory database it runs against.

param(
    [string]$Base = "http://127.0.0.1:$env:MEDIQ_PORT",
    [string]$Username = "demo_patient",
    [string]$Password = "demo12345",
    # The staff demo accounts share the patient's password. Separate parameters
    # because they are separate accounts and a deployment could change one.
    [string]$StaffPassword = "demo12345",
    # The origin the Angular dev server runs on. Has to match the server's
    # MEDIQ_WEB_ORIGINS or the browser blocks every call from the web app.
    [string]$WebOrigin = "http://localhost:4200"
)

$script:Pass = 0
$script:Fail = 0

function Check($name, [scriptblock]$block, $expected) {
    try {
        $actual = & $block
        if ("$actual" -eq "$expected") {
            $script:Pass++
            Write-Host "  PASS  $name"
        } else {
            $script:Fail++
            Write-Host "  FAIL  $name -> got '$actual', wanted '$expected'"
        }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ("$status" -eq "$expected") {
            $script:Pass++
            Write-Host "  PASS  $name (HTTP $status)"
        } else {
            $script:Fail++
            Write-Host "  FAIL  $name -> HTTP $status, wanted $expected; $($_.Exception.Message)"
        }
    }
}

function Status($method, $uri, $headers, $body) {
    $p = @{ Uri = $uri; Method = $method; UseBasicParsing = $true; ErrorAction = 'Stop' }
    if ($headers) { $p.Headers = $headers }
    if ($body) { $p.ContentType = "application/json"; $p.Body = ($body | ConvertTo-Json -Compress) }
    try { (Invoke-WebRequest @p).StatusCode } catch { $_.Exception.Response.StatusCode.value__ }
}

function Body($method, $uri, $headers, $body) {
    $p = @{ Uri = $uri; Method = $method; UseBasicParsing = $true; ErrorAction = 'Stop' }
    if ($headers) { $p.Headers = $headers }
    if ($body) { $p.ContentType = "application/json"; $p.Body = ($body | ConvertTo-Json -Compress) }
    try {
        Invoke-RestMethod @p
    } catch {
        # Error responses carry the JSON body the assertions need to read (the
        # conflict code, the sign-in failure message). Reading the response
        # stream here returns nothing, because Invoke-RestMethod has already
        # consumed it - ErrorDetails is the only place the body survives.
        #
        # It arrives as an already-decoded JSON array, so the payload is the
        # first element rather than the string itself.
        $raw = $_.ErrorDetails.Message
        if (-not $raw) { return $null }
        if ($raw -is [array]) { $raw = $raw[0] }
        if ($raw -is [string]) { $raw | ConvertFrom-Json } else { $raw }
    }
}

Write-Host "Smoke testing $Base"

# Discovered rather than hardcoded: ids are UUIDs and change with every seed.
$docId = (Body GET "$Base/doctors" $null).items[0].id
if (-not $docId) {
    Write-Host "  FAIL  no doctors returned - is the server running with MEDIQ_SEED_DEMO?"
    exit 1
}

# --- Public ---------------------------------------------------------------

Write-Host "`nPublic routes"
Check "health" { Status GET "$Base/health" } 200
Check "doctors list" { Status GET "$Base/doctors" } 200
Check "doctor by id" { Status GET "$Base/doctors/$docId" } 200
Check "unknown doctor is 404" { Status GET "$Base/doctors/00000000-0000-0000-0000-000000000000" } 404
Check "unknown specialty is 400" { Status GET "$Base/doctors?specialty=bogus" } 400
Check "availability month=YYYY-MM" { Status GET "$Base/doctors/$docId/availability?month=2026-12" } 200
Check "availability month omitted" { Status GET "$Base/doctors/$docId/availability" } 200
Check "availability bad month is 400" { Status GET "$Base/doctors/$docId/availability?month=nope" } 400
Check "slots" { Status GET "$Base/doctors/$docId/slots?date=2026-12-01" } 200
Check "slots without date is 400" { Status GET "$Base/doctors/$docId/slots" } 400
Check "slots bad date is 400" { Status GET "$Base/doctors/$docId/slots?date=nope" } 400

# --- Auth -----------------------------------------------------------------

Write-Host "`nAuth"
$session = Body POST "$Base/auth/sign-in" $null @{ username = $Username; password = $Password }
if (-not $session.accessToken) {
    Write-Host "  FAIL  could not sign in as $Username - is MEDIQ_SEED_DEMO set?"
    exit 1
}
$auth = @{ Authorization = "Bearer $($session.accessToken)" }
Write-Host "  PASS  sign in as $Username"

Check "wrong password is 401" { Status POST "$Base/auth/sign-in" $null @{ username = $Username; password = "wrong-password" } } 401
Check "unknown user is 401" { Status POST "$Base/auth/sign-in" $null @{ username = "nobody-here"; password = $Password } } 401

$wrongPassword = Body POST "$Base/auth/sign-in" $null @{ username = $Username; password = "wrong-password" }
$unknownUser = Body POST "$Base/auth/sign-in" $null @{ username = "nobody-here"; password = $Password }
if ($wrongPassword.error -eq $unknownUser.error -and $wrongPassword.message -eq $unknownUser.message) {
    Write-Host "  PASS  wrong password and unknown user are indistinguishable"
    $script:Pass++
} else {
    Write-Host "  FAIL  the two sign-in failures differ: '$($wrongPassword.message)' vs '$($unknownUser.message)'"
    $script:Fail++
}

Check "authenticated route without a token is 401" { Status GET "$Base/profile" } 401
Check "profile with a token" { Status GET "$Base/profile" $auth } 200

# --- Staff and the browser ------------------------------------------------
#
# The staff half of sign-in: the roles the web signs in as, and the CORS answer
# that decides whether a browser is allowed to make the call at all. Neither is
# reachable from a store-level test — one is a JWT claim the route layer reads
# and the other is a preflight the browser makes before anything else.

Write-Host "`nStaff sign-in"

$staffAccounts = @(
    @{ username = "demo_admin"; role = "admin" },
    @{ username = "demo_doctor"; role = "doctor" },
    @{ username = "demo_secretary"; role = "secretary" }
)

$staffTokens = @{}
foreach ($account in $staffAccounts) {
    $staff = Body POST "$Base/auth/sign-in" $null @{
        username = $account.username
        password = $StaffPassword
    }
    if ($staff.profile.role -eq $account.role -and $staff.accessToken) {
        $script:Pass++
        Write-Host "  PASS  sign in as $($account.username) and get role '$($account.role)'"
        $staffTokens[$account.role] = $staff.accessToken
    } else {
        $script:Fail++
        Write-Host "  FAIL  $($account.username) returned role '$($staff.profile.role)', wanted '$($account.role)'"
    }
}

# The one staff behaviour the web already depends on: a non-patient caller reads
# every appointment rather than only their own, which is what a doctor's queue
# and a secretary's calendar are. Without it those screens are silently empty.
if ($staffTokens['doctor']) {
    $doctorAuth = @{ Authorization = "Bearer $($staffTokens['doctor'])" }
    Check "a doctor can read the appointment list" { Status GET "$Base/appointments" $doctorAuth } 200
}

# --- Admin (role-gated) ---------------------------------------------------
#
# The first routes that check *who* is calling rather than only whether the
# caller is signed in. No store-level test can prove the answer: the role comes
# out of a JWT claim the route layer reads, so the check has to be made over
# HTTP. The 403 below is the whole point of the section — a route that forgets
# its role check answers 200 and every other check here still passes.

Write-Host "`nAdmin"

if (-not $staffTokens['admin'] -or -not $staffTokens['doctor']) {
    Write-Host "  FAIL  no admin or doctor token - the sign-in above failed"
    exit 1
}
$adminAuth = @{ Authorization = "Bearer $($staffTokens['admin'])" }
$wrongRoleAuth = @{ Authorization = "Bearer $($staffTokens['doctor'])" }

Check "the account directory needs a token" { Status GET "$Base/admin/accounts" } 401
Check "the account directory answers an administrator" { Status GET "$Base/admin/accounts" $adminAuth } 200
Check "a doctor's token is refused on the account directory" { Status GET "$Base/admin/accounts" $wrongRoleAuth } 403
Check "the specialty list needs a token" { Status GET "$Base/admin/specializations" } 401
Check "the specialty list answers an administrator" { Status GET "$Base/admin/specializations" $adminAuth } 200

# A 200 with empty bodies would satisfy every check above, so the content is
# asserted too.
$directory = Body GET "$Base/admin/accounts" $adminAuth
$seededDoctor = $directory.doctors | Where-Object { $_.username -eq 'demo_doctor' }
if ($seededDoctor -and $seededDoctor.role -eq 'doctor' -and $seededDoctor.doctorId -and $seededDoctor.createdAt) {
    $script:Pass++
    Write-Host "  PASS  the directory carries the seeded staff account, its doctor link and its creation date"
} else {
    $script:Fail++
    Write-Host "  FAIL  the directory did not carry demo_doctor: $($directory | ConvertTo-Json -Compress -Depth 5)"
}

# The one thing a directory endpoint can get wrong while still answering 200.
$patientNames = @($directory.patients | ForEach-Object { $_.username })
$staffNames = @($directory.doctors.username) + @($directory.secretaries.username)
$leaked = @($staffNames | Where-Object { $patientNames -contains $_ })
$misfiledPatients = @($directory.patients | Where-Object { $_.role -ne 'patient' })
if ($patientNames.Count -ge 1 -and $leaked.Count -eq 0 -and $misfiledPatients.Count -eq 0) {
    $script:Pass++
    Write-Host "  PASS  patients and staff are in their own lists"
} else {
    $script:Fail++
    Write-Host "  FAIL  the directories overlap: patients=[$($patientNames -join ',')] leaked=[$($leaked -join ',')]"
}

$specialtyItems = @((Body GET "$Base/admin/specializations" $adminAuth).items)
$withDoctors = @($specialtyItems | Where-Object { $_.doctorCount -gt 0 })
$withoutDoctors = @($specialtyItems | Where-Object { $_.doctorCount -eq 0 })
if ($specialtyItems.Count -eq 6 -and $withDoctors.Count -ge 1 -and $withoutDoctors.Count -ge 1) {
    $script:Pass++
    Write-Host "  PASS  all six specialties are listed, some counting doctors and some a zero"
} else {
    $script:Fail++
    Write-Host "  FAIL  the specialty list is wrong: $($specialtyItems | ConvertTo-Json -Compress -Depth 5)"
}

Write-Host "`nCORS"

function Preflight($origin) {
    $options = @{
        Uri           = "$Base/auth/sign-in"
        Method        = 'OPTIONS'
        UseBasicParsing = $true
        ErrorAction   = 'Stop'
        Headers       = @{
            'Origin'                        = $origin
            'Access-Control-Request-Method' = 'POST'
            'Access-Control-Request-Headers' = 'authorization,content-type'
        }
    }
    try {
        $response = Invoke-WebRequest @options
        [pscustomobject]@{
            status      = $response.StatusCode
            allowOrigin = $response.Headers['Access-Control-Allow-Origin']
        }
    } catch {
        [pscustomobject]@{
            status      = $_.Exception.Response.StatusCode.value__
            allowOrigin = $null
        }
    }
}

$allowed = Preflight $WebOrigin
if ($allowed.status -eq 200 -and $allowed.allowOrigin -eq $WebOrigin) {
    $script:Pass++
    Write-Host "  PASS  the preflight from $WebOrigin is allowed, with the origin echoed back"
} else {
    $script:Fail++
    Write-Host "  FAIL  preflight from $WebOrigin returned $($allowed.status), allow-origin '$($allowed.allowOrigin)'"
}

# The negative half is the one that matters. An allow-list that answers every
# origin is not an allow-list, and the failure is invisible in the positive test.
# A different *port* on an allowed host is used deliberately: Ktor's `allowHost`
# ignores the port, so this case is what tells the two mechanisms apart.
$denied = Preflight "http://localhost:4201"
if ($denied.status -ne 200 -and -not $denied.allowOrigin) {
    $script:Pass++
    Write-Host "  PASS  a neighbouring port on an allowed host is refused ($($denied.status))"
} else {
    $script:Fail++
    Write-Host "  FAIL  localhost:4201 was allowed: status $($denied.status), allow-origin '$($denied.allowOrigin)'"
}

# --- Booking --------------------------------------------------------------

Write-Host "`nBooking"
$slot = (Body GET "$Base/doctors/$docId/slots?date=2026-12-01" $auth).items |
    Where-Object { $_.status -eq 'available' } |
    Select-Object -First 1

if (-not $slot) {
    Write-Host "  FAIL  no bookable slot on the test date"
    exit 1
}

$booking = @{ slotId = $slot.id; reasonForVisit = "Smoke test booking"; confirmedByPatient = $true }
$appointment = Body POST "$Base/appointments" $auth $booking
if ($appointment.id) {
    Write-Host "  PASS  book a slot (appointment $($appointment.id))"
    $script:Pass++
} else {
    Write-Host "  FAIL  could not book slot $($slot.id)"
    $script:Fail++
}

# The double-booking guarantee, over HTTP rather than in-process.
$conflict = Body POST "$Base/appointments" $auth $booking
if ($conflict.error -eq 'slot_taken') {
    Write-Host "  PASS  the same slot is refused the second time (409 slot_taken)"
    $script:Pass++
} else {
    Write-Host "  FAIL  booking the same slot twice returned '$($conflict.error)'"
    $script:Fail++
}

Check "cancel" { Status DELETE "$Base/appointments/$($appointment.id)" $auth } 200
$rebook = Body POST "$Base/appointments" $auth $booking
if ($rebook.id) {
    Write-Host "  PASS  a cancelled slot can be booked again"
    $script:Pass++
} else {
    Write-Host "  FAIL  could not rebook after cancelling"
    $script:Fail++
}
Check "cancel the rebooking" { Status DELETE "$Base/appointments/$($rebook.id)" $auth } 200

# --- Profile --------------------------------------------------------------

Write-Host "`nProfile"
$before = Body GET "$Base/profile" $auth
$updated = Body PUT "$Base/profile" $auth @{ fullName = "Smoke Test Name" }
if ($updated.email -eq $before.email -and $updated.mobileNumber -eq $before.mobileNumber -and
    $updated.dateOfBirth -eq $before.dateOfBirth -and $updated.fullName -eq 'Smoke Test Name') {
    Write-Host "  PASS  a name-only edit leaves the other fields alone"
    $script:Pass++
} else {
    Write-Host "  FAIL  partial profile edit changed other fields"
    $script:Fail++
}
Body PUT "$Base/profile" $auth @{ fullName = $before.fullName } | Out-Null

# --- Sign out -------------------------------------------------------------

Write-Host "`nSign out"
Check "sign out" { Status POST "$Base/auth/sign-out" $auth } 200
Check "the token is dead immediately after sign out" { Status GET "$Base/profile" $auth } 401

Write-Host "`n$($script:Pass) passed, $($script:Fail) failed"
if ($script:Fail -gt 0) { exit 1 }
