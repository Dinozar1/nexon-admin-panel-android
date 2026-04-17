// app/src/main/java/com/example/nexonpayadminpanel/ui/dashboard/DashboardState.kt
package com.example.nexonpayadminpanel.ui.dashboard

import com.example.nexonpayadminpanel.retrofit.ConfigItem

sealed interface DashboardState {
    object Loading : DashboardState
    data class Success(val configs: List<ConfigItem>) : DashboardState
    data class Error(val message: String) : DashboardState
}