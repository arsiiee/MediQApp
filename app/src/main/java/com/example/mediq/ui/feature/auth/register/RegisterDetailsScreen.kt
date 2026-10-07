package com.example.mediq.ui.feature.auth.register

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand
import com.example.mediq.ui.navigation.Screen
import kotlinx.coroutines.flow.filterIsInstance
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateOfBirthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDetailsScreen(
    navController: NavController,
    viewModel: RegisterViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    // The date field opens the picker from the text field's *own* interaction
    // source rather than from a `Modifier.clickable` wrapped around it.
    //
    // That modifier does not work reliably here: a `readOnly` OutlinedTextField
    // still installs its own pointer handlers for cursor placement, and those sit
    // inside the modifier chain, so they can consume the tap before the outer
    // clickable sees it. The symptom is a date field that looks tappable and
    // silently does nothing — and because a date of birth is *required*, that
    // leaves registration impossible to complete. Reading the interaction from
    // the component that actually handles the tap is the pattern Material 3
    // intends.
    val dateFieldInteraction = remember { MutableInteractionSource() }
    LaunchedEffect(dateFieldInteraction) {
        dateFieldInteraction.interactions
            .filterIsInstance<PressInteraction.Release>()
            .collect { if (!uiState.isLoading) showDatePicker = true }
    }

    // Advance only once the server has actually sent a code. Navigating on the
    // button press is what let this wizard walk to the end without ever
    // contacting the backend. `onStepHandled` clears the event so coming back to
    // this step does not bounce the user forward again.
    LaunchedEffect(uiState.pendingStep) {
        if (uiState.pendingStep == RegisterStep.OTP) {
            viewModel.onStepHandled()
            navController.navigate(Screen.RegisterOTP.route)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // `DatePicker` yields UTC-midnight epoch millis for the
                    // chosen day, so it is read at UTC rather than at the
                    // device's zone — which would shift the date by a day
                    // either side of the offset.
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateOfBirthChanged(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Create your account",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "We only collect what is needed to schedule your consultations.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalMediQColors.current.secondaryText
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text("Full Name", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = uiState.fullName,
            onValueChange = viewModel::onFullNameChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Maria Santos") },
            enabled = !uiState.isLoading,
            // A name is prose, so autocorrect stays on — but capitalisation of
            // each word is not wanted either, since the server stores this
            // verbatim and it ends up on a chart.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalMediQColors.current.accent,
                unfocusedBorderColor = LocalMediQColors.current.outline
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Phone Number", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = uiState.mobileNumber,
            onValueChange = viewModel::onMobileNumberChanged,
            modifier = Modifier.fillMaxWidth(),
            // `+63917…`, not `+63 917 555 0142`: the placeholder used to teach a
            // spaced format that `AuthService.normalizeMobile` rejects, and this
            // field is where the OTP is delivered.
            placeholder = { Text("+639175550142") },
            enabled = !uiState.isLoading,
            // The phone keypad. Without it the alphabetic keyboard and
            // autocorrect are live on the one field whose value the server
            // pattern-matches.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalMediQColors.current.accent,
                unfocusedBorderColor = LocalMediQColors.current.outline
            )
        )
        Text(
            text = "Used for the verification code and appointment reminders.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalMediQColors.current.secondaryText,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Date of Birth", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        // Not a typed field: free-text dates are ambiguous across formats, and
        // the server only accepts `YYYY-MM-DD`.
        OutlinedTextField(
            value = uiState.dateOfBirth?.format(dateOfBirthFormat).orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = !uiState.isLoading,
            interactionSource = dateFieldInteraction,
            placeholder = { Text("Select your date of birth") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = "Choose date of birth",
                    tint = LocalMediQColors.current.secondaryText,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalMediQColors.current.accent,
                unfocusedBorderColor = LocalMediQColors.current.outline
            )
        )
        Text(
            text = "Required. The clinic needs it on your chart.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalMediQColors.current.secondaryText,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = viewModel::requestOtp,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MediQGreen)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MediQOnBrand,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Continue", fontSize = 18.sp)
            }
        }
    }
}
