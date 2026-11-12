package com.example.mediq.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQPageHeader
import com.example.mediq.ui.components.MediQNotice
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.session.MediQSession
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorContainer
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Registration step 1: who the patient is.
 *
 * Job: collect a name and an email (required) plus an optional mobile number
 * and date of birth. Primary action: continue to the one-time-code step.
 */
@Composable
fun RegisterDetailsScreen(
    navController: NavController,
    state: RegisterState = AuthSamples.registerIdle
) {
    RegisterDetailsContent(
        state = state,
        actions = RegisterActions(
            onBack = { navController.popBackStack() },
            onDetailsContinue = { fullName, email, phoneNumber, dateOfBirth ->
                MediQSession.beginRegistration(fullName, email, phoneNumber, dateOfBirth)
                MediQSession.generateLocalOtp()
                navController.navigate(Screen.RegisterOTP.route)
            }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDetailsContent(
    state: RegisterState,
    actions: RegisterActions,
    initialErrors: DetailsErrors = DetailsErrors(null, null, null, null),
    modifier: Modifier = Modifier
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var dateOfBirth by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(initialErrors) }
    var showDobPicker by rememberSaveable { mutableStateOf(false) }

    val submitting = state == RegisterState.Submitting
    val dobInteraction = remember { MutableInteractionSource() }

    LaunchedEffect(dobInteraction) {
        dobInteraction.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release && !submitting) {
                showDobPicker = true
            }
        }
    }

    if (showDobPicker) {
        val todayUtcStart = remember { startOfTodayUtcMillis() }
        val initialSelected = remember(dateOfBirth) { parseDobToUtcMillis(dateOfBirth) }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelected,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcStart
                override fun isSelectableYear(year: Int): Boolean =
                    year in 1900..Calendar.getInstance().get(Calendar.YEAR)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDobPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            dateOfBirth = formatUtcDateMillis(millis)
                        }
                        showDobPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            dateOfBirth = ""
                            showDobPicker = false
                        }
                    ) { Text("Clear") }
                    TextButton(onClick = { showDobPicker = false }) { Text("Cancel") }
                }
            }
        ) {
            DatePicker(state = pickerState, title = null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQBackground)
    ) {
        MediQPageHeader(title = REGISTRATION_TITLE, onBack = actions.onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MediQSpacing.gutter)
        ) {
            Spacer(Modifier.height(MediQSpacing.xl))

            RegistrationProgress(currentStep = 1)

            Spacer(Modifier.height(MediQSpacing.xl))

            Text(
                text = "Your details",
                style = MaterialTheme.typography.titleLarge,
                color = MediQTextPrimary
            )

            Spacer(Modifier.height(MediQSpacing.xs))

            Text(
                text = "We only collect what is needed to schedule your consultations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary
            )

            Spacer(Modifier.height(MediQSpacing.xxl))

            AuthField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Full name",
                placeholder = AuthSamples.FULL_NAME,
                error = errors.fullName,
                enabled = !submitting
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            AuthField(
                value = email,
                onValueChange = { email = it },
                label = "Email address",
                placeholder = "name@example.com",
                error = errors.email,
                hint = "Used for verification and sign-in.",
                enabled = !submitting,
                keyboardType = KeyboardType.Text
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            AuthField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = "Mobile number",
                placeholder = AuthSamples.PHONE_NUMBER,
                error = errors.phoneNumber,
                hint = "Optional. Used for appointment reminders.",
                enabled = !submitting,
                keyboardType = KeyboardType.Phone
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            AuthField(
                value = dateOfBirth,
                onValueChange = { },
                label = "Date of birth",
                placeholder = "Select date",
                error = errors.dateOfBirth,
                hint = "Optional. Tap to choose your birthday.",
                enabled = !submitting,
                readOnly = true,
                interactionSource = dobInteraction,
                keyboardType = KeyboardType.Text,
                trailingIcon = {
                    IconButton(
                        onClick = { if (!submitting) showDobPicker = true },
                        enabled = !submitting
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Select date of birth"
                        )
                    }
                }
            )

            if (state is RegisterState.Error) {
                Spacer(Modifier.height(MediQSpacing.md))

                MediQNotice(
                    icon = Icons.Outlined.CloudOff,
                    message = state.message,
                    container = MediQErrorContainer,
                    content = MediQError
                )
            }

            Spacer(Modifier.height(MediQSpacing.xxl))

            AuthPrimaryButton(
                label = if (submitting) "Sending code..." else "Continue",
                enabled = !submitting,
                onClick = {
                    val checked = AuthValidation.validateDetails(fullName, email, phoneNumber, dateOfBirth)
                    errors = checked
                    if (checked.isClean) {
                        actions.onDetailsContinue(fullName.trim(), email.trim(), phoneNumber.trim(), dateOfBirth.trim())
                    }
                }
            )

            Spacer(Modifier.height(MediQSpacing.xxl))
        }
    }
}

private fun formatUtcDateMillis(utcTimeMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(utcTimeMillis))
}

private fun parseDobToUtcMillis(value: String): Long? {
    if (value.isBlank()) return null
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        formatter.parse(value.trim())?.time
    } catch (_: Exception) {
        null
    }
}

private fun startOfTodayUtcMillis(): Long {
    val local = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return utc.timeInMillis
}
