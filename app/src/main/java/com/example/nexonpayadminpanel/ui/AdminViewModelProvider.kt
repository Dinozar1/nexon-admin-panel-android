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

object AdminViewModelProvider {
    val Factory = viewModelFactory {

        // Inicjalizator dla logowania
        initializer {
            val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AdminApplication)
            LoginViewModel(
                apiService = application.apiService,
                tokenManager = application.tokenManager
            )
        }

        // Inicjalizator dla listy sklepów (Dashboard)
        initializer {
            val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AdminApplication)
            DashboardViewModel(
                apiService = application.apiService
            )
        }

        // Szczegóły sklepu (ConfigDetails)
        initializer {
            val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AdminApplication)
            ConfigDetailsViewModel(
                apiService = application.apiService
            )
        }
    }
}