// app/src/main/java/com/example/nexonpayadminpanel/ui/login/LoginViewModel.kt
package com.example.nexonpayadminpanel.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexonpayadminpanel.R
import com.example.nexonpayadminpanel.data.TokenManager
import com.example.nexonpayadminpanel.retrofit.ApiService
import com.example.nexonpayadminpanel.retrofit.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val application: Application,
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : AndroidViewModel(application) {

    // Internal mutable state. Only the ViewModel can change this.
    private val _uiState = MutableStateFlow<LoginState>(LoginState.Idle)

    // Public read-only state exposed to the UI component.
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    // Handles the login logic and network request
    fun login(email: String, pass: String) {
        // Basic input validation
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = LoginState.Error(application.getString(R.string.error_empty_fields))
            return
        }

        // Notify the UI to show a loading spinner
        _uiState.value = LoginState.Loading

        // Launch a background coroutine to avoid freezing the main UI thread
        viewModelScope.launch {
            try {
                // Prepare and send the API request
                val request = LoginRequest(login = email, password = pass)
                val response = apiService.login(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success && body.accessToken != null) {
                        // Save the JWT token securely using EncryptedSharedPreferences
                        tokenManager.saveAccessToken(body.accessToken)

                        // Notify the UI that login was successful (triggers navigation)
                        _uiState.value = LoginState.Success
                    } else {
                        // Handle logical errors from the backend (e.g., wrong password)
                        val errorMsg = body?.message ?: application.getString(R.string.error_invalid_credentials)
                        _uiState.value = LoginState.Error(errorMsg)
                    }
                } else {
                    // Handle HTTP errors (e.g., 401 Unauthorized, 500 Server Error)
                    _uiState.value = LoginState.Error("${application.getString(R.string.error_server_code)} ${response.code()}")
                }
            } catch (e: Exception) {
                // Catch network exceptions (e.g., no internet connection, timeout)
                _uiState.value = LoginState.Error("${application.getString(R.string.error_network_prefix)} ${e.localizedMessage}")
            }
        }
    }

    // Resets the state to prevent accidental re-navigation
    fun resetState() {
        _uiState.value = LoginState.Idle
    }
}