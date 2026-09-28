package com.example.mediq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mediq.ui.navigation.MediQNavHost
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQTheme
import com.example.mediq.ui.components.MediQBottomBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(
                android.graphics.Color.BLACK
            )
        )
        setContent {
            MediQTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Doctors.route,
                    Screen.Appointments.route,
                    Screen.Messages.route,
                    Screen.Profile.route,
                    Screen.DoctorDetails.route,
                    Screen.BookingFlow.route,
                    Screen.BookingSuccess.route,
                    Screen.AppointmentDetails.route
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            MediQBottomBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MediQNavHost(navController = navController)
                    }
                }
            }
        }
    }
}
