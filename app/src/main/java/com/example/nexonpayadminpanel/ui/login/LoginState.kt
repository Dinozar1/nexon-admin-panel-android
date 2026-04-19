// app/src/main/java/com/example/nexonpayadminpanel/ui/login/LoginState.kt
package com.example.nexonpayadminpanel.ui.login

// Sealed interface restricts the inheritance hierarchy.
// The compiler knows exactly what states are possible, ensuring exhaustive checks.
sealed interface LoginState {

    // Initial state: waiting for user input
    object Idle : LoginState

    // In-progress state: showing a loading spinner while waiting for the API
    object Loading : LoginState

    // Success state: triggers navigation to the Dashboard
    object Success : LoginState

    // Error state: holds a specific failure message (e.g., "Invalid password")
    data class Error(val message: String) : LoginState
}