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

# The status column and the writes behind the admin screen's Enable and Disable
# buttons. Disabling has to mean something: an account that can still sign in and
# whose live token still works is a badge, not a control.
$everyAccount = @($directory.patients) + @($directory.doctors) + @($directory.secretaries)
$withStatus = @($everyAccount | Where-Object { $_.status })
if ($everyAccount.Count -ge 1 -and $withStatus.Count -eq $everyAccount.Count) {
    $script:Pass++
    Write-Host "  PASS  every account in the directory reports a status"
} else {
    $script:Fail++
    Write-Host "  FAIL  $($withStatus.Count) of $($everyAccount.Count) accounts carry a status"
}

$described = @($specialtyItems | Where-Object { $_.description })
if ($described.Count -eq $specialtyItems.Count) {
    $script:Pass++
    Write-Host "  PASS  every specialty carries its description"
} else {
    $script:Fail++
    Write-Host "  FAIL  only $($described.Count) of $($specialtyItems.Count) specialties carry a description"
}

# The demo secretary is the account this section disables, so the three checks
# below it need a token that is live at the start.
$secretary = $directory.secretaries | Where-Object { $_.username -eq 'demo_secretary' }
$adminProfile = Body GET "$Base/profile" $adminAuth
$secretaryAuth = @{ Authorization = "Bearer $($staffTokens['secretary'])" }
$statusUrl = "$Base/admin/accounts/$($secretary.id)/status"

Check "the status route needs a token" { Status PATCH $statusUrl $null @{ status = 'inactive' } } 401
Check "a doctor's token is refused on the status route" { Status PATCH $statusUrl $wrongRoleAuth @{ status = 'inactive' } } 403
Check "an unknown status is a 400" { Status PATCH $statusUrl $adminAuth @{ status = 'suspended' } } 400
Check "an unknown account is a 404" {
    Status PATCH "$Base/admin/accounts/00000000-0000-0000-0000-000000000000/status" $adminAuth @{ status = 'inactive' }
} 404
Check "an administrator cannot disable their own account" {
    Status PATCH "$Base/admin/accounts/$($adminProfile.id)/status" $adminAuth @{ status = 'inactive' }
} 400

$disabled = Body PATCH $statusUrl $adminAuth @{ status = 'inactive' }
if ($disabled.status -eq 'inactive') {
    $script:Pass++
    Write-Host "  PASS  an administrator can disable an account"
} else {
    $script:Fail++
    Write-Host "  FAIL  disabling returned status '$($disabled.status)'"
}

# The two checks that make the column a boundary rather than a label: sign-in has
# to refuse, and a token issued *before* the disable has to stop working.
Check "a disabled account can no longer sign in" {
    Status POST "$Base/auth/sign-in" $null @{ username = 'demo_secretary'; password = $StaffPassword }
} 401
Check "a disabled account's existing token stops working" { Status GET "$Base/profile" $secretaryAuth } 401

$enabled = Body PATCH $statusUrl $adminAuth @{ status = 'active' }
if ($enabled.status -eq 'active') {
    $script:Pass++
    Write-Host "  PASS  an administrator can re-enable an account"
} else {
    $script:Fail++
    Write-Host "  FAIL  re-enabling returned status '$($enabled.status)'"
}

# Disabling is reversible from the same column, so leaving the demo secretary
# enabled keeps the rest of the run and any later manual walk unchanged.
Check "a re-enabled account can sign in again" {
    Status POST "$Base/auth/sign-in" $null @{ username = 'demo_secretary'; password = $StaffPassword }
} 200

# --- Creating staff accounts ----------------------------------------------
#
# The module's only write that adds rows, and the only place the server writes a
# `doctors` row at all. A doctor account is two rows written in one transaction,
# and no store-level test can show the pair arriving over HTTP, the account
# signing in with the password the administrator chose, or the practice record
# landing where the doctor profile reads it. So all three are asserted here.
#
# Usernames carry a timestamp because a second run against the same server would
# otherwise collide with the accounts the first one made and read a 400 as a bug.

Write-Host "`nCreate staff account"

$suffix = (Get-Date).ToString("HHmmss")
$createUrl = "$Base/admin/accounts"
$newPassword = "smoke-password-1"
$doctorAccount = @{
    username     = "smoke_doctor_$suffix"
    password     = $newPassword
    fullName     = "Smoke Doctor"
    email        = "smoke_doctor_$suffix@clinic.test"
    mobileNumber = "6391700$suffix"
    dateOfBirth  = "1985-04-12"
    role         = "doctor"
    practice     = @{
        specializationId = "cardiology"
        licenseNumber    = "PRC-SMOKE-1"
        yearsExperience  = 9
        feeCentavos      = 75000
        bio              = "Created by the smoke run."
        building         = "Main"
        floor            = "2F"
        room             = "204"
        languages        = @("en", "fil")
    }
}

Check "creating a staff account needs a token" {
    Status POST $createUrl $null @{ username = 'anon'; password = $newPassword; fullName = 'A'; mobileNumber = '639170000000'; dateOfBirth = '1990-01-01'; role = 'admin' }
} 401
Check "a doctor's token is refused on the create route" {
    Status POST $createUrl $wrongRoleAuth @{ username = 'wrong-role'; password = $newPassword; fullName = 'W'; mobileNumber = '639170000001'; dateOfBirth = '1990-01-01'; role = 'admin' }
} 403
Check "a patient account cannot be created here" {
    Status POST $createUrl $adminAuth @{ username = "smoke_patient_$suffix"; password = $newPassword; fullName = 'P'; mobileNumber = "6391701$suffix"; dateOfBirth = '1990-01-01'; role = 'patient' }
} 400
Check "a doctor account with no practice details is refused" {
    Status POST $createUrl $adminAuth @{ username = "smoke_bare_$suffix"; password = $newPassword; fullName = 'B'; mobileNumber = "6391702$suffix"; dateOfBirth = '1990-01-01'; role = 'doctor' }
} 400
Check "a duplicate username is a 400, not a constraint error" {
    Status POST $createUrl $adminAuth @{ username = 'demo_admin'; password = $newPassword; fullName = 'D'; mobileNumber = "6391703$suffix"; dateOfBirth = '1990-01-01'; role = 'admin' }
} 400
Check "a secretary assigned to a doctor that is not there is refused" {
    Status POST $createUrl $adminAuth @{ username = "smoke_orphan_$suffix"; password = $newPassword; fullName = 'O'; mobileNumber = "6391705$suffix"; dateOfBirth = '1990-01-01'; role = 'secretary'; assignedDoctorId = '00000000-0000-0000-0000-000000000000' }
} 400

$createdDoctor = Body POST $createUrl $adminAuth $doctorAccount
if ($createdDoctor.role -eq 'doctor' -and $createdDoctor.status -eq 'active' -and $createdDoctor.doctorId) {
    $script:Pass++
    Write-Host "  PASS  an administrator can create a doctor account, practice record and all"
} else {
    $script:Fail++
    Write-Host "  FAIL  the created doctor came back as: $($createdDoctor | ConvertTo-Json -Compress -Depth 5)"
}

# The link is only a link if what it points at is the practice record that was
# sent, readable through the same route a patient's doctor profile uses.
$newPractice = Body GET "$Base/doctors/$($createdDoctor.doctorId)" $null
if ($newPractice.specialty -eq 'cardiology' -and
    $newPractice.licenseNumber -eq 'PRC-SMOKE-1' -and
    $newPractice.consultationFee.amountInCentavos -eq 75000 -and
    $newPractice.location.room -eq '204' -and
    @($newPractice.languages).Count -eq 2) {
    $script:Pass++
    Write-Host "  PASS  the created doctor's practice record reads back from the doctor profile"
} else {
    $script:Fail++
    Write-Host "  FAIL  the practice record came back as: $($newPractice | ConvertTo-Json -Compress -Depth 5)"
}

Check "the created account can sign in with the password the administrator set" {
    Status POST "$Base/auth/sign-in" $null @{ username = $doctorAccount.username; password = $newPassword }
} 200

$secretaryAccount = @{
    username         = "smoke_secretary_$suffix"
    password         = $newPassword
    fullName         = "Smoke Secretary"
    email            = "smoke_secretary_$suffix@clinic.test"
    mobileNumber     = "6391704$suffix"
    dateOfBirth      = "1993-07-02"
    role             = "secretary"
    assignedDoctorId = $createdDoctor.doctorId
}
$createdSecretary = Body POST $createUrl $adminAuth $secretaryAccount
if ($createdSecretary.role -eq 'secretary' -and $createdSecretary.doctorId -eq $createdDoctor.doctorId) {
    $script:Pass++
    Write-Host "  PASS  an administrator can create a secretary assigned to a doctor"
} else {
    $script:Fail++
    Write-Host "  FAIL  the created secretary came back as: $($createdSecretary | ConvertTo-Json -Compress -Depth 5)"
}

# A 201 that never reaches the directory would leave the account unmanageable —
# it could be signed in as but never seen, so never disabled.
$afterCreate = Body GET "$Base/admin/accounts" $adminAuth
$listedDoctor = $afterCreate.doctors | Where-Object { $_.username -eq $doctorAccount.username }
$listedSecretary = $afterCreate.secretaries | Where-Object { $_.username -eq $secretaryAccount.username }
if ($listedDoctor -and $listedDoctor.doctorId -eq $createdDoctor.doctorId -and $listedSecretary) {
    $script:Pass++
    Write-Host "  PASS  the created accounts appear in the directory they will be managed from"
} else {
    $script:Fail++
    Write-Host "  FAIL  created accounts missing from the directory (doctor=$($listedDoctor.username), secretary=$($listedSecretary.username))"
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

# Expiry / revoked token booking assertion: a 401 on booking must fail loudly and never re-issue
$expiredAuth = @{ Authorization = "Bearer expired-or-invalid-token" }
Check "booking with an expired token is refused (401)" { Status POST "$Base/appointments" $expiredAuth $booking } 401

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
