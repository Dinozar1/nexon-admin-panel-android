// app/src/main/java/com/example/nexonpayadminpanel/ui/login/LoginViewModel.kt
package com.example.nexonpayadminpanel.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexonpayadminpanel.data.TokenManager
import com.example.nexonpayadminpanel.retrofit.ApiService
import com.example.nexonpayadminpanel.retrofit.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    // Trzymamy stan ekranu (prywatny, żeby UI nie mogło go samo popsuć)
    private val _uiState = MutableStateFlow<LoginState>(LoginState.Idle)
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = LoginState.Error("Wypełnij wszystkie pola!")
            return
        }

        // Przechodzimy w stan ładowania
        _uiState.value = LoginState.Loading

        // Uruchamiamy Coroutine w tle (wątek sieciowy) - dokładnie tak jak Twój prowadzący uczył
        viewModelScope.launch {
            try {
                val request = LoginRequest(login = email, password = pass)
                val response = apiService.login(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success && body.accessToken != null) {
                        // Zapisujemy token bezpiecznie w pamięci
                        tokenManager.saveAccessToken(body.accessToken)
                        _uiState.value = LoginState.Success
                    } else {
                        _uiState.value = LoginState.Error(body?.message ?: "Błędne dane logowania.")
                    }
                } else {
                    _uiState.value = LoginState.Error("Błąd serwera: ${response.code()}")
                }
            } catch (e: Exception) {
                // Łapiemy np. brak internetu
                _uiState.value = LoginState.Error("Błąd sieci: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginState.Idle
    }
}