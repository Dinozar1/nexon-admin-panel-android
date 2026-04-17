// app/src/main/java/com/example/nexonpayadminpanel/ui/login/LoginState.kt
package com.example.nexonpayadminpanel.ui.login

sealed interface LoginState {
    object Idle : LoginState             // Apka nic nie robi, czeka na wpisanie danych
    object Loading : LoginState          // Kręci się kółko, czekamy na odpowiedź serwera
    object Success : LoginState          // Udało się, można przełączyć na Dashboard
    data class Error(val message: String) : LoginState // Błąd (np. złe hasło)
}