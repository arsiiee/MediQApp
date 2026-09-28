package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary
import androidx.compose.foundation.text.BasicTextField

// ─────────────────────────────────────────────
// MOCK SECRETARY CONVERSATIONS
// ─────────────────────────────────────────────

data class SecretaryConversation(
    val id: String,
    val secretaryName: String,
    val initials: String,
    val doctorName: String,
    val preview: String,
    val date: String,
    val unreadCount: Int
)

val mockConversations = listOf(
    SecretaryConversation(
        id = "maria_secretary",
        secretaryName = "Ms. Liza Fuentes",
        initials = "ML",
        doctorName = "Dr. Maria Elena Sandoval",
        preview = "Yes please, bring your latest CBC and...",
        date = "Sep 12 · 9:20 AM",
        unreadCount = 1
    ),
    SecretaryConversation(
        id = "kathleen_secretary",
        secretaryName = "Ms. Karen Lopez",
        initials = "MK",
        doctorName = "Dr. Kathleen Lim",
        preview = "Your appointment request for Thursday...",
        date = "Sep 12 · 9:20 AM",
        unreadCount = 1
    ),
    SecretaryConversation(
        id = "joel_secretary",
        secretaryName = "Mr. Jay Miller",
        initials = "MJ",
        doctorName = "Dr. Joel Marquez",
        preview = "The doctor has rescheduled your consul...",
        date = "Sep 12 · 9:20 AM",
        unreadCount = 0
    )
)

// ─────────────────────────────────────────────
// MESSAGES LIST
// ─────────────────────────────────────────────

@Composable
fun MessagesScreen(
    navController: NavController
) {
    var searchQuery by remember {
        mutableStateOf("")
    }

    val filteredConversations = mockConversations.filter { conversation ->
        conversation.secretaryName.contains(
            searchQuery,
            ignoreCase = true
        ) ||
                conversation.doctorName.contains(
                    searchQuery,
                    ignoreCase = true
                )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F6))
    ) {

        // ─────────────────────────
        // SEARCH AREA
        // ─────────────────────────

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(MediQGreen)
                .padding(
                    horizontal = 12.dp,
                    vertical = 14.dp
                ),
            contentAlignment = Alignment.Center
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MediQTextSecondary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                BasicTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 12.sp,
                        color = MediQTextPrimary
                    ),
                    decorationBox = { innerTextField ->

                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search clinic chats...",
                                fontSize = 12.sp,
                                color = MediQTextSecondary
                            )
                        }

                        innerTextField()
                    }
                )
            }
        }
        // ─────────────────────────
        // CONVERSATIONS
        // ─────────────────────────

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(
                items = filteredConversations,
                key = { it.id }
            ) { conversation ->

                ConversationCard(
                    conversation = conversation,
                    onClick = {
                        navController.navigate(
                            Screen.MessageChat.createRoute(
                                conversation.id
                            )
                        )
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// CONVERSATION CARD
// ─────────────────────────────────────────────

@Composable
private fun ConversationCard(
    conversation: SecretaryConversation,
    onClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        color = Color.White
    ) {

        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE7F3EA)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.initials,
                    color = MediQGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(11.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = conversation.secretaryName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MediQTextPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Secretary · ${conversation.doctorName}",
                    fontSize = 11.sp,
                    color = MediQGreen,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = conversation.preview,
                    fontSize = 12.sp,
                    color = MediQTextPrimary,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = conversation.date,
                    fontSize = 10.sp,
                    color = MediQTextSecondary
                )
            }

            if (conversation.unreadCount > 0) {

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MediQGreen),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = conversation.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// CHAT MESSAGE DATA
// ─────────────────────────────────────────────

data class ChatMessage(
    val text: String,
    val sentByPatient: Boolean,
    val time: String
)

private fun mockMessagesFor(
    conversationId: String
): List<ChatMessage> {

    return when (conversationId) {

        "maria_secretary" -> listOf(
            ChatMessage(
                text = "Good morning! Is Dr. Sandoval still available this Friday afternoon?",
                sentByPatient = true,
                time = "Sep 11 · 8:12 AM"
            ),
            ChatMessage(
                text = "Good morning, Ms. Salcedo. Dr. Sandoval has two open slots on Friday, 1:00 PM and 2:30 PM. You may reserve either one in the app.",
                sentByPatient = false,
                time = "Sep 11 · 8:31 AM"
            ),
            ChatMessage(
                text = "Thank you! Do I need to bring my previous laboratory results?",
                sentByPatient = true,
                time = "Sep 11 · 8:34 AM"
            ),
            ChatMessage(
                text = "Yes please, bring your latest CBC and lipid profile. Kindly arrive 15 minutes before your slot for registration.",
                sentByPatient = false,
                time = "Sep 11 · 8:40 AM"
            )
        )

        "kathleen_secretary" -> listOf(
            ChatMessage(
                text = "Good morning. I would like to confirm my appointment with Dr. Lim.",
                sentByPatient = true,
                time = "Sep 11 · 9:02 AM"
            ),
            ChatMessage(
                text = "Good morning! Your request is being reviewed. We will notify you once the clinic confirms the schedule.",
                sentByPatient = false,
                time = "Sep 11 · 9:15 AM"
            )
        )

        else -> listOf(
            ChatMessage(
                text = "Good morning. I wanted to ask about my appointment.",
                sentByPatient = true,
                time = "Sep 11 · 8:12 AM"
            ),
            ChatMessage(
                text = "Hello! Please send us your appointment concern and we will assist you.",
                sentByPatient = false,
                time = "Sep 11 · 8:20 AM"
            )
        )
    }
}

// ─────────────────────────────────────────────
// SECRETARY CHAT
// ─────────────────────────────────────────────

@Composable
fun SecretaryChatScreen(
    navController: NavController,
    conversationId: String
) {

    val conversation = mockConversations.firstOrNull {
        it.id == conversationId
    } ?: mockConversations.first()

    var messageText by remember {
        mutableStateOf("")
    }

    val messages = remember(conversationId) {
        mutableStateListOf(
            *mockMessagesFor(conversationId).toTypedArray()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F6))
    ) {

        // ─────────────────────────
        // CHAT HEADER
        // ─────────────────────────

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(MediQGreen)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {
                    // Always return to the Messages list.
                    navController.navigate(Screen.Messages.route) {
                        launchSingleTop = true
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back to messages",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {

                Text(
                    text = conversation.secretaryName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Clinic secretary · replies within an hour",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp
                )
            }
        }

        // ─────────────────────────
        // NOTICE
        // ─────────────────────────

        Text(
            text = "For appointment concerns only. This channel is not for medical advice or emergencies.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 24.dp,
                    vertical = 10.dp
                ),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = MediQTextSecondary
        )

        // ─────────────────────────
        // MESSAGES
        // ─────────────────────────

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(messages) { message ->

                ChatBubble(
                    message = message
                )
            }
        }

        // ─────────────────────────
        // MESSAGE INPUT
        // ─────────────────────────

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = messageText,
                onValueChange = {
                    messageText = it
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Type your message...",
                        fontSize = 12.sp,
                        color = MediQTextSecondary
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 12.sp,
                    color = MediQTextPrimary
                ),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF3F5F4),
                    unfocusedContainerColor = Color(0xFFF3F5F4),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {

                    if (messageText.isNotBlank()) {

                        messages.add(
                            ChatMessage(
                                text = messageText.trim(),
                                sentByPatient = true,
                                time = "Now"
                            )
                        )

                        messageText = ""
                    }
                },
                modifier = Modifier.size(44.dp),
                containerColor = MediQGreen,
                contentColor = Color.White
            ) {

                Icon(
                    imageVector = Icons.Outlined.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// CHAT BUBBLE
// ─────────────────────────────────────────────

@Composable
private fun ChatBubble(
    message: ChatMessage
) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.sentByPatient) {
            Alignment.End
        } else {
            Alignment.Start
        }
    ) {

        Surface(
            modifier = Modifier.widthIn(
                max = 290.dp
            ),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (message.sentByPatient) 12.dp else 3.dp,
                bottomEnd = if (message.sentByPatient) 3.dp else 12.dp
            ),
            color = if (message.sentByPatient) {
                MediQGreen
            } else {
                Color.White
            }
        ) {

            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = if (message.sentByPatient) {
                    Color.White
                } else {
                    MediQTextPrimary
                }
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = message.time,
            fontSize = 9.sp,
            color = MediQTextSecondary
        )
    }
}