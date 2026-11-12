package com.example.mediq.ui.screens.messages

import com.example.mediq.ui.screens.appointments.AppointmentBooking

/*
 * What the patient sees in the messages list and in a chat.
 *
 * One place so the list and the chat can share the same mock source.
 */

data class SecretaryConversation(
    val id: String,
    val secretaryName: String,
    val initials: String,
    val doctorName: String,
    val preview: String,
    val date: String,
    val unreadCount: Int,
    /** Appointment date in ISO format, or null when there is no appointment linked. */
    val appointmentDate: String? = null,
    /** Appointment time in 12-hour format, e.g. "2:00 PM", or null. */
    val appointmentTime: String? = null,
)

val MockConversations = listOf(
    SecretaryConversation(
        id = "maria_secretary",
        secretaryName = "Ms. Liza Fuentes",
        initials = "ML",
        doctorName = "Dr. Maria Elena Sandoval",
        preview = "Yes please, bring your latest CBC and...",
        date = "Sep 12 · 9:20 AM",
        unreadCount = 1,
        appointmentDate = "2026-09-25",
        appointmentTime = "2:00 PM"
    ),
    SecretaryConversation(
        id = "kathleen_secretary",
        secretaryName = "Ms. Karen Lopez",
        initials = "MK",
        doctorName = "Dr. Kathleen Lim",
        preview = "Your appointment request for Thursday...",
        date = "Sep 12 · 9:20 AM",
        unreadCount = 1,
        appointmentDate = "2026-09-24",
        appointmentTime = "10:30 AM"
    ),
    SecretaryConversation(
        id = "joel_secretary",
        secretaryName = "Mr. Jay Miller",
        initials = "MJ",
        doctorName = "Dr. Joel Marquez",
        preview = "The doctor has rescheduled your consul...",
        date = "Sep 12 · 9:20 AM",
        unreadCount = 0,
        appointmentDate = null,
        appointmentTime = null
    )
)

data class ChatMessage(
    val id: String,
    val text: String,
    val sentByPatient: Boolean,
    val time: String,
    val appointmentId: String? = null,
)

/** Build the mock history for a conversation. */
fun mockMessagesFor(conversationId: String): List<ChatMessage> = when (conversationId) {
    "maria_secretary" -> listOf(
        ChatMessage(
            id = "m1",
            text = "Good morning! Is Dr. Sandoval still available this Friday afternoon?",
            sentByPatient = true,
            time = "Sep 11 · 8:12 AM"
        ),
        ChatMessage(
            id = "m2",
            text = "Good morning, Ms. Salcedo. Dr. Sandoval has two open slots on Friday, 1:00 PM and 2:30 PM. You may reserve either one in the app.",
            sentByPatient = false,
            time = "Sep 11 · 8:31 AM"
        ),
        ChatMessage(
            id = "m3",
            text = "Thank you! Do I need to bring my previous laboratory results?",
            sentByPatient = true,
            time = "Sep 11 · 8:34 AM"
        ),
        ChatMessage(
            id = "m4",
            text = "Yes please, bring your latest CBC and lipid profile. Kindly arrive 15 minutes before your slot for registration.",
            sentByPatient = false,
            time = "Sep 11 · 8:40 AM"
        )
    )

    "kathleen_secretary" -> listOf(
        ChatMessage(
            id = "k1",
            text = "Good morning. I would like to confirm my appointment with Dr. Lim.",
            sentByPatient = true,
            time = "Sep 11 · 9:02 AM"
        ),
        ChatMessage(
            id = "k2",
            text = "Good morning! Your request is being reviewed. We will notify you once the clinic confirms the schedule.",
            sentByPatient = false,
            time = "Sep 11 · 9:15 AM"
        )
    )

    else -> listOf(
        ChatMessage(
            id = "g1",
            text = "Good morning. I wanted to ask about my appointment.",
            sentByPatient = true,
            time = "Sep 11 · 8:12 AM"
        ),
        ChatMessage(
            id = "g2",
            text = "Hello! Please send us your appointment concern and we will assist you.",
            sentByPatient = false,
            time = "Sep 11 · 8:20 AM"
        )
    )
}

/**
 * The messages list can be loading, failed, empty, or showing conversations.
 */
sealed interface MessagesUiState {
    data object Loading : MessagesUiState
    data class Error(val message: String) : MessagesUiState
    data object Empty : MessagesUiState
    data class Content(val conversations: List<SecretaryConversation>) : MessagesUiState
}

data class MessagesActions(
    val onOpenChat: (String) -> Unit = {},
    val onRetry: () -> Unit = {}
)