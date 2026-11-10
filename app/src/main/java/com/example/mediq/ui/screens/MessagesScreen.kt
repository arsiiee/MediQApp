package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen

@Composable
fun MessagesScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }

    val chats = listOf(
        ChatData("Ms. Liza Fuentes", "Secretary · Dr. Maria Elena Sandoval", "Yes please, bring your latest CBC and...", "Sep 12 · 9:20 AM", 1, "ML"),
        ChatData("Ms. Karen Lopez", "Secretary · Dr. Kathleen Lim", "Your appointment request for Thursday...", "Sep 12 · 9:20 AM", 1, "MK"),
        ChatData("Mr. Jay Miller", "Secretary · Dr. Joel Marquez", "The doctor has rescheduled your consul...", "Sep 12 · 9:20 AM", 0, "MJ")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Green Header with Search Bar (Equal Height: 64.dp, smaller aligned search box)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MediQGreen)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                color = Color.White,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search clinic chats...",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Chat List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(chats) { chat ->
                ChatListItem(chat = chat, onClick = {
                    navController.navigate(Screen.ChatDetail.createRoute(chat.name))
                })
            }
        }
    }
}

data class ChatData(
    val name: String,
    val subtitle: String,
    val preview: String,
    val timestamp: String,
    val unreadCount: Int,
    val initials: String
)

@Composable
fun ChatListItem(chat: ChatData, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, MediQGreen.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MediQLightGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.initials,
                    color = MediQGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chat.subtitle,
                    color = MediQGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chat.preview,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chat.timestamp,
                    color = Color.Gray.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }

            if (chat.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MediQGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
