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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexonPayAdminPanelTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AdminDestinations.Login.name,
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        // --- EKRAN 1: LOGOWANIE ---
                        composable(route = AdminDestinations.Login.name) {
                            val loginViewModel: LoginViewModel = viewModel(factory = AdminViewModelProvider.Factory)
                            LoginScreen(
                                viewModel = loginViewModel,
                                onLoginSuccess = {
                                    navController.navigate(AdminDestinations.Dashboard.name) {
                                        popUpTo(AdminDestinations.Login.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // --- EKRAN 2: DASHBOARD (Lista sklepów) ---
                        composable(route = AdminDestinations.Dashboard.name) {
                            val dashboardViewModel: DashboardViewModel = viewModel(factory = AdminViewModelProvider.Factory)
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onConfigClick = { configName ->
                                    // FIX: Teraz faktycznie nawigujemy do szczegółów, przekazując nazwę
                                    navController.navigate(AdminDestinations.ConfigDetails.name + "/$configName")
                                }
                            )
                        }

                        // --- EKRAN 3: SZCZEGÓŁY KONFIGURACJI (Statystyki, Portfele, Oko) ---
                        composable(
                            route = AdminDestinations.ConfigDetails.name + "/{configName}",
                            arguments = listOf(navArgument("configName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            // Wyciągamy nazwę configu z argumentów nawigacji
                            val configName = backStackEntry.arguments?.getString("configName") ?: ""

                            val detailsViewModel: ConfigDetailsViewModel = viewModel(factory = AdminViewModelProvider.Factory)

                            // Załadowanie danych przy wejściu na ekran
                            LaunchedEffect(configName) {
                                detailsViewModel.loadInitialData(configName)
                            }

                            ConfigDetailsScreen(
                                viewModel = detailsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}