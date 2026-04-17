// app/src/main/java/com/example/nexonpayadminpanel/ui/dashboard/DashboardViewModel.kt
package com.example.nexonpayadminpanel.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    // Używamy SharedFlow do wysyłania jednorazowych zdarzeń (np. komunikatu po zmianie hasła)
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        fetchConfigs()
    }

    fun fetchConfigs() {
        _uiState.value = DashboardState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getConfigs()
                if (response.isSuccessful) {
                    val configs = response.body() ?: emptyList()
                    _uiState.value = DashboardState.Success(configs)
                } else {
                    _uiState.value = DashboardState.Error("Błąd serwera: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error("Błąd sieci: ${e.localizedMessage}")
            }
        }
    }

    // --- NOWE FUNKCJE DO ZARZĄDZANIA TERMINALEM ---

    fun changeTerminalLogin(newLogin: String) {
        if (newLogin.isBlank()) return

        viewModelScope.launch {
            try {
                val request = ChangeTerminalLoginRequest(newLogin)
                val response = apiService.changeTerminalLogin(request)
                if (response.isSuccessful) {
                    _toastMessage.emit("Pomyślnie zmieniono login terminala!")
                } else {
                    _toastMessage.emit("Błąd zmiany loginu: ${response.code()}")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Błąd sieci przy zmianie loginu.")
            }
        }
    }

    fun changeTerminalPassword(newPassword: String) {
        if (newPassword.isBlank()) return

        viewModelScope.launch {
            try {
                val request = ChangeTerminalPasswordRequest(newPassword)
                val response = apiService.changeTerminalPassword(request)
                if (response.isSuccessful) {
                    _toastMessage.emit("Pomyślnie zmieniono hasło terminala!")
                } else {
                    _toastMessage.emit("Błąd zmiany hasła: ${response.code()}")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Błąd sieci przy zmianie hasła.")
            }
        }
    }

    fun addNewConfig(name: String, currency: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            try {
                val response = apiService.addNewConfig(AddConfigRequest(name, currency))
                if (response.isSuccessful) {
                    _toastMessage.emit("Dodano nową konfigurację!")
                    fetchConfigs() // Odświeżamy listę
                } else {
                    _toastMessage.emit("Błąd dodawania: ${response.code()}")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Błąd sieci.")
            }
        }
    }

    fun deleteConfig(configId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteConfig(DeleteConfigRequest(configId))
                if (response.isSuccessful) {
                    _toastMessage.emit("Usunięto konfigurację: $configId")
                    fetchConfigs() // Odświeżamy listę
                } else {
                    _toastMessage.emit("Błąd usuwania.")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Błąd sieci.")
            }
        }
    }


}