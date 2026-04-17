package com.example.nexonpayadminpanel.ui.configdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexonpayadminpanel.retrofit.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConfigDetailsState(
    val isLoading: Boolean = true,
    val configName: String = "",
    val stats: AdminStatsResponse? = null,
    val wallets: List<WalletEntry> = emptyList(),
    val error: String? = null
)

class ConfigDetailsViewModel(private val apiService: ApiService) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigDetailsState())
    val uiState: StateFlow<ConfigDetailsState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _balance = MutableStateFlow<String?>(null)
    val balance: StateFlow<String?> = _balance.asStateFlow()

    private val _availableCryptos = MutableStateFlow<List<CryptoItem>>(emptyList())
    val availableCryptos: StateFlow<List<CryptoItem>> = _availableCryptos.asStateFlow()

    private val _availableNetworks = MutableStateFlow<List<NetworkItem>>(emptyList())
    val availableNetworks: StateFlow<List<NetworkItem>> = _availableNetworks.asStateFlow()

    fun loadInitialData(configName: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, configName = configName, error = null)

        viewModelScope.launch {
            try {
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

    fun toggleBalance() {
        if (_balance.value != null) {
            _balance.value = null // Ukryj
            return
        }

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

    fun deleteWallet(cryptoId: String, networkId: String, publicKey: String) {
        viewModelScope.launch {
            try {
                // Pakujemy wszystkie wymagane dane do obiektu żądania
                val req = DeleteSettingRequest(
                    cryptoId = cryptoId,
                    networkId = networkId,
                    configId = _uiState.value.configName, // Nazwę configu mamy w stanie widoku
                    publicKey = publicKey
                )

                val res = apiService.deleteSetting(req)
                if (res.isSuccessful) {
                    _toastMessage.emit("Usunięto portfel")
                    loadInitialData(_uiState.value.configName) // Odświeżamy listę
                } else {
                    _toastMessage.emit("Błąd usuwania: ${res.code()}")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Błąd sieci")
            }
        }
    }

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

    fun addWallet(cryptoId: String, networkName: String, publicKey: String) {
        viewModelScope.launch {
            try {
                // Teraz przekazujemy cryptoId do żądania
                val req = PostNewSettingRequest(cryptoId, networkName, publicKey, _uiState.value.configName)
                val res = apiService.postNewSetting(req)
                if (res.isSuccessful) {
                    _toastMessage.emit("Dodano portfel")
                    loadInitialData(_uiState.value.configName)
                } else {
                    _toastMessage.emit("Błąd dodawania: ${res.code()}")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Błąd sieci")
            }
        }
    }
}