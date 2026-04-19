// app/src/main/java/com/example/nexonpayadminpanel/ui/configdetails/ConfigDetailsViewModel.kt
package com.example.nexonpayadminpanel.ui.configdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexonpayadminpanel.retrofit.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Data class representing the entire UI state for the details screen
data class ConfigDetailsState(
    val isLoading: Boolean = true,
    val configName: String = "",
    val stats: AdminStatsResponse? = null,
    val wallets: List<WalletEntry> = emptyList(),
    val error: String? = null
)

class ConfigDetailsViewModel(private val apiService: ApiService) : ViewModel() {

    // Main UI state
    private val _uiState = MutableStateFlow(ConfigDetailsState())
    val uiState: StateFlow<ConfigDetailsState> = _uiState.asStateFlow()

    // One-time events for Toasts
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Holds the total balance. Null means it's hidden (eye icon closed).
    private val _balance = MutableStateFlow<String?>(null)
    val balance: StateFlow<String?> = _balance.asStateFlow()

    // States for the cascading dropdowns in the "Add Wallet" modal
    private val _availableCryptos = MutableStateFlow<List<CryptoItem>>(emptyList())
    val availableCryptos: StateFlow<List<CryptoItem>> = _availableCryptos.asStateFlow()

    private val _availableNetworks = MutableStateFlow<List<NetworkItem>>(emptyList())
    val availableNetworks: StateFlow<List<NetworkItem>> = _availableNetworks.asStateFlow()

    // Fetches both stats and wallet list when entering the screen
    fun loadInitialData(configName: String) {
        // Set loading to true and update the config name using .copy()
        _uiState.value = _uiState.value.copy(isLoading = true, configName = configName, error = null)

        viewModelScope.launch {
            try {
                // Fetch stats and wallets sequentially
                val statsRes = apiService.getStats(configName)
                val configDataRes = apiService.getConfigData(configName)

                if (statsRes.isSuccessful && configDataRes.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        stats = statsRes.body(),
                        wallets = configDataRes.body()?.entries ?: emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Błąd pobierania danych")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Błąd sieci")
            }
        }
    }

    // Toggles the visibility of the total FIAT balance
    fun toggleBalance() {
        // If balance is currently visible, simply clear it to hide it (no API call needed)
        if (_balance.value != null) {
            _balance.value = null
            return
        }

        // If hidden, fetch the balance from the backend
        viewModelScope.launch {
            try {
                val res = apiService.getTotalWalletBalance(_uiState.value.configName)
                if (res.isSuccessful) {
                    val body = res.body()
                    _balance.value = "${body?.totalBalanceFiat ?: "0.00"} ${body?.fiatCurrency ?: ""}"
                } else {
                    _toastMessage.emit("Błąd salda")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Błąd sieci")
            }
        }
    }

    // Removes a specific wallet from the current configuration
    fun deleteWallet(cryptoId: String, networkId: String, publicKey: String) {
        viewModelScope.launch {
            try {
                // Pack all required identifiers into the request body
                val req = DeleteSettingRequest(
                    cryptoId = cryptoId,
                    networkId = networkId,
                    configId = _uiState.value.configName, // Extracted securely from the current state
                    publicKey = publicKey
                )

                val res = apiService.deleteSetting(req)
                if (res.isSuccessful) {
                    _toastMessage.emit("Usunięto portfel")
                    loadInitialData(_uiState.value.configName) // Auto-refresh the wallet list
                } else {
                    _toastMessage.emit("Błąd usuwania: ${res.code()}")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Błąd sieci")
            }
        }
    }

    // Populates the first dropdown (Cryptocurrencies)
    fun loadCryptos() {
        viewModelScope.launch {
            try {
                val res = apiService.getCryptoList()
                if (res.isSuccessful) {
                    _availableCryptos.value = res.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    // Populates the second dropdown (Networks) based on the selected crypto
    fun loadNetworks(cryptoId: String) {
        viewModelScope.launch {
            try {
                val res = apiService.getNetworkList(cryptoId)
                if (res.isSuccessful) {
                    _availableNetworks.value = res.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    // Assigns a new wallet to the current configuration
    fun addWallet(cryptoId: String, networkName: String, publicKey: String) {
        viewModelScope.launch {
            try {
                val req = PostNewSettingRequest(cryptoId, networkName, publicKey, _uiState.value.configName)
                val res = apiService.postNewSetting(req)
                if (res.isSuccessful) {
                    _toastMessage.emit("Dodano portfel")
                    loadInitialData(_uiState.value.configName) // Auto-refresh the wallet list
                } else {
                    _toastMessage.emit("Błąd dodawania: ${res.code()}")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Błąd sieci")
            }
        }
    }
}