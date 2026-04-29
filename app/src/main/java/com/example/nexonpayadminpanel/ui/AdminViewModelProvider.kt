// app/src/main/java/com/example/nexonpayadminpanel/ui/AdminViewModelProvider.kt
package com.example.nexonpayadminpanel.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nexonpayadminpanel.AdminApplication
import com.example.nexonpayadminpanel.ui.configdetails.ConfigDetailsViewModel
import com.example.nexonpayadminpanel.ui.dashboard.DashboardViewModel
import com.example.nexonpayadminpanel.ui.login.LoginViewModel

// Central factory responsible for creating ViewModels with their required dependencies
object AdminViewModelProvider {

    val Factory = viewModelFactory {

        // Initializer for the Login Screen
        initializer {
            // 1. Get the global application instance to access shared tools
            val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AdminApplication)

            // 2. Build the ViewModel and inject the necessary tools (API and Token storage)
            LoginViewModel(
                application = application,
                apiService = application.apiService,
                tokenManager = application.tokenManager
            )
        }

        // Initializer for the Dashboard (Shops List) Screen
        initializer {
            val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AdminApplication)

            // This ViewModel only needs network access, not the token manager directly
            DashboardViewModel(
                application = application,
                apiService = application.apiService
            )
        }

        // Initializer for the Configuration Details (Shop Details) Screen
        initializer {
            val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AdminApplication)

            ConfigDetailsViewModel(
                application = application,
                apiService = application.apiService
            )
        }
    }
}