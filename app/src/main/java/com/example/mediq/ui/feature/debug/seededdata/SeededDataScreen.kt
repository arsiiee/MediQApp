package com.example.mediq.ui.feature.debug.seededdata

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mediq.core.designsystem.component.EmptyState
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Money
import com.example.mediq.domain.model.dataOrEmpty
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * A temporary screen that answers one question: **is the backend actually
 * working?** Long-press the splash wordmark to open it.
 *
 * ## Temporary
 *
 * Delete alongside [SeededDataViewModel], the `Screen.SeededData` route, its one
 * line in `MediQNavHost`, and the splash gesture. Nothing else depends on it.
 *
 * ## Every value on this screen came from the server
 *
 * There is no fallback row, no placeholder, and no cached copy. That is what makes
 * the screen a verification tool rather than decoration — a hardcoded list renders
 * identically with the backend dead, so it would report a healthy clinic while
 * proving nothing. Press **Re-check** with the server stopped and the list empties;
 * that is the screen telling the truth.
 *
 * ## What a good result here does *not* prove
 *
 * This screen exercises one public, unauthenticated route, `GET /doctors`. It says
 * nothing about the session, about any write, or about a field being *renamed*
 * rather than added. A clean read here is one route working, not a healthy
 * backend — which is why the caveat is printed on the screen instead of only in
 * the KDoc, where the person holding the phone will never see it.
 *
 * ## Zero rows is genuinely ambiguous, and the wording admits it
 *
 * `RetrofitDoctorRepository.getDoctors` returns an empty page when the server is
 * unreachable (`RetrofitDoctorRepository.kt:38-40`), which is correct for every
 * other screen in the app. So by the time this screen renders "no doctors", the
 * server may be down *or* the seed may not have run, and nothing available at this
 * layer tells the two apart. The empty state names both causes rather than guessing
 * one, because a message that says the wrong thing about a dead backend costs a
 * diagnostic pass — the exact trap [baseUrl] exists to reduce.
 *
 * @param baseUrl the host the app is actually calling, passed in from
 *   `MediQNavHost` because `ui/` may not import `data/`
 *   (`check-boundaries.ps1` Rule 2). Shown rather than assumed: a green screen
 *   pointed at the wrong machine is the dangerous failure, and `10.0.2.2` is
 *   unroutable on a physical phone while the server answers `127.0.0.1` fine.
 */
@Composable
fun SeededDataScreen(baseUrl: String) {
    val viewModel: SeededDataViewModel = viewModel(factory = SeededDataViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMediQColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Text(
            text = "Seeded data",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Read live from $baseUrl",
            color = colors.secondaryText,
            fontSize = 13.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = viewModel::refresh) { Text("Re-check") }
            Text(
                text = "Runs the read again. Restart the server or re-seed, then press it.",
                color = colors.secondaryText,
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                when (val doctors = state.doctors) {
                    is LoadState.Loading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                    }

                    is LoadState.Error -> ProblemCard(
                        icon = Icons.Outlined.CloudOff,
                        title = "The server answered with an error",
                        detail = doctors.message,
                    )

                    is LoadState.Success -> if (doctors.data.isEmpty()) {
                        ProblemCard(
                            icon = Icons.Outlined.PersonSearch,
                            title = "No doctors came back",
                            detail = "The read succeeded and the list was empty. Either " +
                                "MEDIQ_SEED_DEMO is not set on the server, or the database has " +
                                "no rows. This screen cannot tell those two apart, because an " +
                                "unreachable server is also reported as an empty list — so " +
                                "check that the server is running as well as that it is seeded.",
                        )
                    } else {
                        Text(
                            text = "${doctors.data.size} doctor(s) read from the server",
                            color = colors.secondaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            items(state.doctors.dataOrEmpty, key = { it.id }) { doctor ->
                DoctorRow(doctor = doctor)
            }

            item { DemoCredentialsCard() }
            item { ScopeFootnote() }
        }
    }
}

@Composable
private fun DoctorRow(doctor: Doctor) {
    val colors = LocalMediQColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.accent.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = doctor.initials,
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(text = doctor.displayName, fontWeight = FontWeight.Bold)
                Text(text = doctor.specialty.displayName, color = colors.secondaryText, fontSize = 13.sp)
                Text(
                    text = "${doctor.location.building} — ${doctor.location.floor} — ${doctor.location.room}",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                )
                Text(
                    text = "${doctor.yearsOfExperience} years · ${doctor.consultationFee.format()}",
                    fontSize = 12.sp,
                )
                // The licence is on the row rather than behind a tap because
                // "is the seed labelled DEMO-*" is a question this screen exists
                // to answer, and `DEMO-PRC-0001` is what proves the row is the
                // seed and not a real clinician.
                Text(
                    text = "Licence ${doctor.licenseNumber}",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Clinic hours (read from clinic_hours)",
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                )
                if (doctor.clinicHours.isEmpty()) {
                    Text(
                        text = "none returned — the join found no rows",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                    )
                } else {
                    doctor.clinicHours.forEach { hours ->
                        Text(
                            text = "${hours.dayOfWeek.label()} ${hours.opensAt.clock()}–${hours.closesAt.clock()}",
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProblemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = LocalMediQColors.current.accent)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = title, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = detail, fontSize = 13.sp, textAlign = TextAlign.Start)
        }
    }
}

/**
 * The demo sign-in pair, so a developer can prove the seed produced a usable
 * account and not just readable doctors.
 *
 * This is the **only** literal on the screen. It is here because it is already
 * public in three places — `DemoData.kt:19` prints it to stdout,
 * `README.md:92-93` documents it, and the seed's own output names it — and
 * hiding it behind a compile flag would imply a protection this app does not have.
 * The password is weak *because* the account is demo-only: `DemoData.kt:19` uses
 * licence numbers that cannot be a real PRC format, and this must never point at a
 * database holding real patients.
 *
 * Shown as plain text on purpose. This is a development tool, so the masking and
 * `KeyboardType.Password` discipline that `check-boundaries.ps1` Rule 4 enforces
 * for patient input does not apply — there is no input here, only a label. No
 * clipboard either: `LocalClipboardManager` is deprecated in this Compose BOM and
 * the system clipboard is readable by other apps, which is a worse trade than
 * typing eight characters.
 */
@Composable
private fun DemoCredentialsCard() {
    val colors = LocalMediQColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.outline),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Demo sign-in", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                text = "Only exists when the server runs with MEDIQ_SEED_DEMO=true. Demo account — not a real clinician or patient.",
                color = colors.secondaryText,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "$DEMO_USERNAME / $DEMO_PASSWORD", fontSize = 14.sp)
        }
    }
}

/**
 * The caveat, on the screen.
 *
 * A verification screen that reports "backend fine" without saying what it did not
 * test invites exactly the over-trust this screen is vulnerable to. A developer
 * holding the phone will not read a KDoc, so the caveat has to be on the glass.
 */
@Composable
private fun ScopeFootnote() {
    Text(
        text = "This proves one thing: GET /doctors returned over HTTP. It says nothing " +
            "about sign-in, any write, or a renamed response field.",
        color = LocalMediQColors.current.secondaryText,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    )
}

private const val DEMO_USERNAME = "demo_patient"
private const val DEMO_PASSWORD = "demo12345"

/** Formats centavos as pesos. Display is the UI's job — see `DoctorsScreen.kt:198`. */
private fun Money.format(): String = "₱$pesos"

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun LocalTime.clock(): String = format(CLOCK)

/** Monday-first, so a schedule reads in the order a clinic week runs. */
private fun DayOfWeek.label(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
