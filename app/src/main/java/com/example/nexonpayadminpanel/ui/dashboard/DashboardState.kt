// app/src/main/java/com/example/nexonpayadminpanel/ui/dashboard/DashboardState.kt
package com.example.nexonpayadminpanel.ui.dashboard

import com.example.nexonpayadminpanel.retrofit.ConfigItem

// Sealed interface defines a strictly limited set of possible UI states for the Dashboard
sealed interface DashboardState {

    // Data is being fetched from the API; tells the UI to show a loading spinner
    object Loading : DashboardState

    // Data successfully loaded; carries the actual list of shops (configs) to be displayed
    data class Success(val configs: List<ConfigItem>) : DashboardState

    // API or network failure; carries the specific error message to display
    data class Error(val message: String) : DashboardState
}