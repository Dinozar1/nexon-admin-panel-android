// app/src/main/java/com/example/nexonpayadminpanel/MainActivity.kt
package com.example.nexonpayadminpanel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nexonpayadminpanel.ui.AdminDestinations
import com.example.nexonpayadminpanel.ui.AdminViewModelProvider
import com.example.nexonpayadminpanel.ui.configdetails.ConfigDetailsScreen
import com.example.nexonpayadminpanel.ui.configdetails.ConfigDetailsViewModel
import com.example.nexonpayadminpanel.ui.dashboard.DashboardScreen
import com.example.nexonpayadminpanel.ui.dashboard.DashboardViewModel
import com.example.nexonpayadminpanel.ui.login.LoginScreen
import com.example.nexonpayadminpanel.ui.login.LoginViewModel
import com.example.nexonpayadminpanel.ui.theme.NexonPayAdminPanelTheme

// The main entry point of the Android application
class MainActivity : ComponentActivity() {

    // Called automatically when the app is launched
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Allows the app to draw under the status bar and navigation bar

        // Sets the Jetpack Compose UI content for this activity
        setContent {
            NexonPayAdminPanelTheme {

                // The "driver" that manages app navigation and the back stack
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // The "map" connecting route names to actual Composable screens
                    NavHost(
                        navController = navController,
                        startDestination = AdminDestinations.Login.name,
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        // --- SCREEN 1: LOGIN ---
                        composable(route = AdminDestinations.Login.name) {
                            // Inject ViewModel using our custom Factory
                            val loginViewModel: LoginViewModel = viewModel(factory = AdminViewModelProvider.Factory)

                            LoginScreen(
                                viewModel = loginViewModel,
                                onLoginSuccess = {
                                    // Navigate to Dashboard and clear the back stack
                                    // so the user can't press 'Back' to return to the login screen
                                    navController.navigate(AdminDestinations.Dashboard.name) {
                                        popUpTo(AdminDestinations.Login.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // --- SCREEN 2: DASHBOARD (Shop List) ---
                        composable(route = AdminDestinations.Dashboard.name) {
                            val dashboardViewModel: DashboardViewModel = viewModel(factory = AdminViewModelProvider.Factory)

                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onConfigClick = { configName ->
                                    // Navigate to details screen, passing the selected config name as a URL argument
                                    navController.navigate(AdminDestinations.ConfigDetails.name + "/$configName")
                                }
                            )
                        }

                        // --- SCREEN 3: CONFIG DETAILS (Stats, Wallets, Eye) ---
                        // Defines a dynamic route expecting a String argument named "configName"
                        composable(
                            route = AdminDestinations.ConfigDetails.name + "/{configName}",
                            arguments = listOf(navArgument("configName") { type = NavType.StringType })
                        ) { backStackEntry ->

                            // Extract the argument from the navigation route
                            val configName = backStackEntry.arguments?.getString("configName") ?: ""

                            val detailsViewModel: ConfigDetailsViewModel = viewModel(factory = AdminViewModelProvider.Factory)

                            // Trigger data fetch as soon as this screen is opened with a specific config name
                            LaunchedEffect(configName) {
                                detailsViewModel.loadInitialData(configName)
                            }

                            ConfigDetailsScreen(
                                viewModel = detailsViewModel,
                                onBack = { navController.popBackStack() } // Navigate to the previous screen
                            )
                        }
                    }
                }
            }
        }
    }
}