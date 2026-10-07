package com.example.mediq.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.mediq.di.AppContainer
import com.example.mediq.ui.feature.appointments.AppointmentDetailsScreen
import com.example.mediq.ui.feature.appointments.AppointmentsScreen
import com.example.mediq.ui.feature.auth.register.RegisterCredentialsScreen
import com.example.mediq.ui.feature.auth.register.RegisterDetailsScreen
import com.example.mediq.ui.feature.auth.register.RegisterOTPScreen
import com.example.mediq.ui.feature.auth.register.RegisterSuccessScreen
import com.example.mediq.ui.feature.auth.register.RegisterViewModel
import com.example.mediq.ui.feature.auth.signin.SignInScreen
import com.example.mediq.ui.feature.auth.splash.SplashScreen
import com.example.mediq.ui.feature.booking.BookingFlowScreen
import com.example.mediq.ui.feature.booking.BookingSuccessScreen
import com.example.mediq.ui.feature.debug.seededdata.SeededDataScreen
import com.example.mediq.ui.feature.doctors.DoctorDetailsScreen
import com.example.mediq.ui.feature.doctors.DoctorsScreen
import com.example.mediq.ui.feature.home.HomeScreen
import com.example.mediq.ui.feature.messages.MessagesScreen
import com.example.mediq.ui.feature.notifications.NotificationsScreen
import com.example.mediq.ui.feature.profile.ProfileScreen

/**
 * The one [RegisterViewModel] for the whole wizard, taken from the enclosing
 * graph rather than from any destination.
 *
 * The four register steps are a single transaction — the name and number typed
 * on step 1 are still needed on step 3 — so a per-screen ViewModel would have
 * to pass values forward by hand. Scoping to the graph gives all four the same
 * instance, and clearing the graph's store drops the draft.
 *
 * Each `composable` receives its *own* back stack entry, and a ViewModel taken
 * from one of those would be a different instance per step, so the graph's entry
 * is looked up by its route instead.
 */
@Composable
private fun NavBackStackEntry.registerViewModel(navController: NavHostController): RegisterViewModel {
    val graphEntry = remember(this, navController) {
        navController.getBackStackEntry(Screen.RegisterFlow.route)
    }
    return viewModel(viewModelStoreOwner = graphEntry, factory = RegisterViewModel.Factory)
}

@Composable
fun MediQNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Auth Flow
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.SignIn.route) { SignInScreen(navController) }

        // Registration, nested so the four steps share one RegisterViewModel.
        navigation(
            startDestination = Screen.RegisterDetails.route,
            route = Screen.RegisterFlow.route,
        ) {
            composable(Screen.RegisterDetails.route) { entry ->
                RegisterDetailsScreen(navController, entry.registerViewModel(navController))
            }
            composable(Screen.RegisterOTP.route) { entry ->
                RegisterOTPScreen(navController, entry.registerViewModel(navController))
            }
            composable(Screen.RegisterCredentials.route) { entry ->
                RegisterCredentialsScreen(navController, entry.registerViewModel(navController))
            }
            composable(Screen.RegisterSuccess.route) { entry ->
                RegisterSuccessScreen(navController, entry.registerViewModel(navController))
            }
        }

        // Main Tabs
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Doctors.route) { DoctorsScreen(navController) }
        composable(Screen.Appointments.route) { AppointmentsScreen(navController) }
        composable(Screen.Messages.route) { MessagesScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }

        // Details
        composable(Screen.DoctorDetails.route) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
            DoctorDetailsScreen(navController, doctorId)
        }
        composable(Screen.BookingFlow.route) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
            BookingFlowScreen(navController, doctorId)
        }
        composable(Screen.BookingSuccess.route) { BookingSuccessScreen(navController) }
        composable(Screen.AppointmentDetails.route) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId")
            AppointmentDetailsScreen(navController, appointmentId)
        }
        composable(Screen.Notifications.route) { NotificationsScreen(navController) }

        // TEMPORARY. The only route with no entry point a patient can find —
        // it is reached by long-pressing the splash wordmark, never from the
        // bottom bar. Delete this block, `Screen.SeededData`, and the gesture.
        //
        // `baseUrl` comes from `AppContainer` rather than `RetrofitClient`
        // because `ui/` may not import `data/` (`check-boundaries.ps1` Rule 2).
        // The screen shows it so a wrong host is visible rather than assumed.
        composable(Screen.SeededData.route) {
            SeededDataScreen(baseUrl = AppContainer.baseUrl)
        }
    }
}
