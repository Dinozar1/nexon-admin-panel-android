// app/src/main/java/com/example/nexonpayadminpanel/ui/dashboard/DashboardViewModel.kt
package com.example.nexonpayadminpanel.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexonpayadminpanel.R
import com.example.nexonpayadminpanel.retrofit.ApiService
import com.example.nexonpayadminpanel.retrofit.ChangeTerminalLoginRequest
import com.example.nexonpayadminpanel.retrofit.ChangeTerminalPasswordRequest
import com.example.nexonpayadminpanel.retrofit.DeleteConfigRequest
import com.example.nexonpayadminpanel.retrofit.AddConfigRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val application: Application,
    private val apiService: ApiService
) : AndroidViewModel(application) {

    // Main UI state (Loading, Success with data, Error). Replays the current state to the UI.
    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    // One-time event stream (SharedFlow). Used for Toasts so they don't reappear on screen rotation.
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Automatically fetch data from the API as soon as the ViewModel is created
    init {
        fetchConfigs()
    }

    // Fetches the list of all configurations (shops) for the Dashboard
    fun fetchConfigs() {
        _uiState.value = DashboardState.Loading

        viewModelScope.launch {
            try {
                val response = apiService.getConfigs()
                if (response.isSuccessful) {
                    val configs = response.body() ?: emptyList()
                    _uiState.value = DashboardState.Success(configs)
                } else {
                    _uiState.value = DashboardState.Error("${application.getString(R.string.error_server_code)} ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error("${application.getString(R.string.error_network_prefix)} ${e.localizedMessage}")
            }
        }
    }

    // --- TERMINAL MANAGEMENT FUNCTIONS ---

    // Updates the login credentials for physical terminal devices
    fun changeTerminalLogin(newLogin: String) {
        if (newLogin.isBlank()) return

        viewModelScope.launch {
            try {
                val request = ChangeTerminalLoginRequest(newLogin)
                val response = apiService.changeTerminalLogin(request)
                if (response.isSuccessful) {
                    _toastMessage.emit(application.getString(R.string.success_terminal_login))
                } else {
                    _toastMessage.emit("${application.getString(R.string.error_change_login)} ${response.code()}")
                }
            } catch (e: Exception) {
                _toastMessage.emit(application.getString(R.string.error_network_terminal_login))
            }
        }
    }

    // Updates the password for physical terminal devices
    fun changeTerminalPassword(newPassword: String) {
        if (newPassword.isBlank()) return

        viewModelScope.launch {
            try {
                val request = ChangeTerminalPasswordRequest(newPassword)
                val response = apiService.changeTerminalPassword(request)
                if (response.isSuccessful) {
                    _toastMessage.emit(application.getString(R.string.success_terminal_password))
                } else {
                    _toastMessage.emit("${application.getString(R.string.error_change_password)} ${response.code()}")
                }
            } catch (e: Exception) {
                _toastMessage.emit(application.getString(R.string.error_network_terminal_password))
            }
        }
    }

    // Creates a new configuration (shop profile)
    fun addNewConfig(name: String, currency: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            try {
                val response = apiService.addNewConfig(AddConfigRequest(name, currency))
                if (response.isSuccessful) {
                    _toastMessage.emit(application.getString(R.string.success_config_added))
                    fetchConfigs() // Auto-refresh the list after a successful addition
                } else {
                    _toastMessage.emit("${application.getString(R.string.error_add_config)} ${response.code()}")
                }
            } catch (e: Exception) {
                _toastMessage.emit(application.getString(R.string.error_network_general))
            }
        }
    }

    // Deletes an existing configuration
    fun deleteConfig(configId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteConfig(DeleteConfigRequest(configId))
                if (response.isSuccessful) {
                    _toastMessage.emit("${application.getString(R.string.success_config_deleted)} $configId")
                    fetchConfigs() // Auto-refresh the list after a successful deletion
                } else {
                    _toastMessage.emit(application.getString(R.string.error_delete_config))
                }
            } catch (e: Exception) {
                _toastMessage.emit(application.getString(R.string.error_network_general))
            }
        }
    }
}