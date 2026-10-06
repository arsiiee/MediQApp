package com.example.mediq.ui.feature.auth.register

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.ui.navigation.Screen

@Composable
fun RegisterSuccessScreen(
    navController: NavController,
    viewModel: RegisterViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = LocalMediQColors.current.accent,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Account created",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                // The name comes from the session the server returned. It used
                // to be the literal string "Jesse", which greeted every patient
                // with someone else's name. `takeIf` guards a blank name, which
                // would otherwise render "Welcome to MediQ, ."
                text = uiState.registeredName
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        "Welcome to MediQ, ${it.substringBefore(' ')}. Your number is verified and your account is ready — you can search doctors and book your first consultation."
                    }
                    ?: "Your number is verified and your account is ready.",
                textAlign = TextAlign.Center,
                color = LocalMediQColors.current.secondaryText
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                // `RetrofitAuthRepository.register` stored the session before it
                // returned, so the user is already signed in here. This went to
                // the sign-in form, which asked someone who was authenticated to
                // authenticate again.
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MediQGreen)
            ) {
                Text("Start browsing doctors", fontSize = 18.sp)
            }
        }
    }
}
