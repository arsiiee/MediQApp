package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(navController: NavController, chatName: String) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    val messages = remember {
        mutableStateListOf(
            ChatMessage("Good morning! Is Dr. Sandoval still available this Friday afternoon?", false, "Sep 11 · 8:12 AM"),
            ChatMessage("Good morning, Ms. Salcedo. Dr. Sandoval has two open slots on Friday, 1:00 PM and 2:30 PM. You may reserve either one in the app.", true, "Sep 11 · 8:31 AM"),
            ChatMessage("Thank you! Do I need to bring my previous laboratory results?", false, "Sep 11 · 8:34 AM"),
            ChatMessage("Yes please, bring your latest CBC and lipid profile. Kindly arrive 15 minutes before your slot for registration.", true, "Sep 11 · 8:40 AM")
        )
    }

    val sendMessage = {
        if (messageText.isNotBlank()) {
            messages.add(ChatMessage(messageText, false, "Just now"))
            messageText = ""
            coroutineScope.launch {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = chatName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text(text = "Clinic secretary · replies within an hour", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MediQGreen)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        placeholder = { Text("Type your message...", color = Color.Gray) },
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MediQSurface,
                            unfocusedContainerColor = MediQSurface,
                            disabledContainerColor = MediQSurface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage() })
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = { sendMessage() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MediQGreen, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MediQSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "For appointment concerns only. This channel is not for medical advice or emergencies.",
                    modifier = Modifier.padding(12.dp),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg)
                }
            }
        }
    }
}

data class ChatMessage(val text: String, val isReceived: Boolean, val timestamp: String)

@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isReceived) Alignment.Start else Alignment.End
    ) {
        Surface(
            color = if (message.isReceived) MediQSurface else MediQGreen,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isReceived) 4.dp else 16.dp,
                bottomEnd = if (message.isReceived) 16.dp else 4.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(14.dp),
                color = if (message.isReceived) Color.Black else Color.White,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message.timestamp,
            color = Color.Gray.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}
