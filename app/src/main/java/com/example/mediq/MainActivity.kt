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
import com.example.mediq.ui.components.MediQBottomBar
import com.example.mediq.ui.components.mainTabForRoute
import com.example.mediq.data.api.RetrofitClient
import com.example.mediq.ui.navigation.MediQNavHost
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.session.MediQSession
import com.example.mediq.ui.theme.MediQTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore the signed-in account (if any) before drawing the first
        // screen, so closing and reopening the app keeps the session.
        MediQSession.init(applicationContext)
        RetrofitClient.accessToken = MediQSession.accessToken
        android.widget.Toast.makeText(
            this,
            if (MediQSession.user.value != null) "Restored: ${MediQSession.user.value?.username}" else "No session restored",
            android.widget.Toast.LENGTH_LONG
        ).show()

        val startDestination = if (MediQSession.user.value != null) {
            Screen.Home.route
        } else {
            Screen.SignIn.route
        }

        // Every screen here has light chrome behind the system bars, so the
        // default auto-detected system bar style is correct. MediQTheme sets the
        // icon appearance to match.
        enableEdgeToEdge()

        setContent {
            MediQTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Shown exactly when the route belongs to a main tab. Derived from
                // the same mapping the bar uses for its selection, so the two can
                // never disagree.
                val showBottomBar = mainTabForRoute(currentRoute) != null

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            MediQBottomBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MediQNavHost(navController = navController, startDestination = startDestination)
                    }
                }
            }
        }
    }
}
