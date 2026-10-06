package com.example.mediq.ui.feature.doctors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.EmptyState
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQLightGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.model.toClinicDate
import com.example.mediq.domain.model.toClinicTime
import com.example.mediq.ui.feature.booking.BookingSelection
import com.example.mediq.ui.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailsScreen(navController: NavController, doctorId: String?) {
    val viewModel: DoctorDetailsViewModel =
        viewModel(factory = DoctorDetailsViewModel.factory(doctorId))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.doctor is LoadState.Success) {
                        Text(
                            (uiState.doctor as LoadState.Success<Doctor>).data.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (uiState.canContinue) {
                val doctor = (uiState.doctor as? LoadState.Success)?.data
                val slot = uiState.selectedSlot
                if (doctor != null && slot != null) {
                    Button(
                        onClick = {
                            BookingSelection.set(
                                BookingSelection.Selection(
                                    doctorId             = doctor.id,
                                    slotId               = slot.id,
                                    doctorDisplayName    = doctor.displayName,
                                    specialtyDisplayName = doctor.specialty.displayName,
                                    startsAt             = slot.startsAt,
                                    locationDisplay      = "${doctor.location.building} · ${doctor.location.room}",
                                    feeCentavos          = doctor.consultationFee.amountInCentavos,
                                )
                            )
                            navController.navigate(Screen.BookingFlow.createRoute(doctor.id))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MediQGreen)
                    ) {
                        Text("Continue to Booking", fontSize = 16.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        when (val doctorState = uiState.doctor) {
            is LoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LocalMediQColors.current.accent)
                }
            }

            is LoadState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    EmptyState(
                        icon = Icons.Outlined.PersonOff,
                        title = "Doctor not available",
                        description = doctorState.message,
                    )
                }
            }

            is LoadState.Success -> {
                val doctor = doctorState.data
                DoctorDetailsContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding),
                    doctor = doctor,
                    availableDates = uiState.availableDates,
                    selectedDate = uiState.selectedDate,
                    slotsState = uiState.slots,
                    selectedSlot = uiState.selectedSlot,
                    onDateSelected = { viewModel.onDateSelected(it) },
                    onSlotSelected = { viewModel.onSlotSelected(it) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DoctorDetailsContent(
    modifier: Modifier = Modifier,
    doctor: Doctor,
    availableDates: LoadState<List<AvailableDate>>,
    selectedDate: LocalDate?,
    slotsState: LoadState<List<TimeSlot>>,
    selectedSlot: TimeSlot?,
    onDateSelected: (LocalDate) -> Unit,
    onSlotSelected: (TimeSlot) -> Unit,
) {
    val colors = LocalMediQColors.current
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        // ── Doctor header ────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = doctor.specialty.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = doctor.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${doctor.yearsOfExperience} yrs experience  ·  ${doctor.licenseNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryText,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${doctor.location.building} · Floor ${doctor.location.floor} · ${doctor.location.room}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Fee
            Text(
                text = "Consultation fee: ₱${doctor.consultationFee.amountInCentavos / 100}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // ── Bio ──────────────────────────────────────────────────────────
        if (doctor.bio.isNotBlank()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = doctor.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondaryText,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Clinic hours ─────────────────────────────────────────────────
        if (doctor.clinicHours.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Clinic Hours",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                doctor.clinicHours.forEach { hours ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = hours.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = colors.secondaryText,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${hours.opensAt} – ${hours.closesAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondaryText,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Available dates ───────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Available Dates",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (availableDates) {
                is LoadState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = colors.accent,
                    strokeWidth = 2.dp,
                )

                is LoadState.Error -> Text(
                    text = availableDates.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )

                is LoadState.Success -> {
                    if (availableDates.data.isEmpty()) {
                        Text(
                            text = "No available dates this month.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryText,
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableDates.data.forEach { availDate ->
                                val isSelected = availDate.date == selectedDate
                                FilterChip(
                                    selected = isSelected,
                                    onClick  = { onDateSelected(availDate.date) },
                                    label = {
                                        Text(
                                            text = availDate.date.format(
                                                DateTimeFormatter.ofPattern("MMM d")
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    },
                                    // A fixed light pair: white on the brand green is 6.63:1, and stays so in
                                    // either mode.
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MediQGreen,
                                        selectedLabelColor     = MediQOnBrand,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Time slots ────────────────────────────────────────────────────
        if (selectedDate != null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Available Times",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                when (slotsState) {
                    is LoadState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MediQGreen,
                        strokeWidth = 2.dp,
                    )

                    is LoadState.Error -> Text(
                        text = slotsState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )

                    is LoadState.Success -> {
                        val bookable = slotsState.data.filter { it.isBookable }
                        if (bookable.isEmpty()) {
                            Text(
                                text = "No open slots on this date.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondaryText,
                            )
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                bookable.forEach { slot ->
                                    val isSelected = slot.id == selectedSlot?.id
                                    Surface(
                                        shape  = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MediQGreen else colors.outline,
                                        ),
                                        color = if (isSelected) MediQLightGreen
                                                else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .padding(vertical = 4.dp)
                                            .clickable { onSlotSelected(slot) },
                                    ) {
                                        Text(
                                            text     = slot.startsAt.toClinicTime().toString(),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            style    = MaterialTheme.typography.bodySmall,
                                            // Selected is a fixed pair: brand green on
                                            // the light green chip is 5.89:1.
                                            color    = if (isSelected) MediQGreen else colors.secondaryText,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom padding so the bottom bar doesn't overlap content.
        Spacer(modifier = Modifier.height(80.dp))
    }
}