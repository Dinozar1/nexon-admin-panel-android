package com.example.nexonpayadminpanel.retrofit



import com.squareup.moshi.JsonClass

import retrofit2.Response

import retrofit2.http.Body

import retrofit2.http.GET
import retrofit2.http.HTTP

import retrofit2.http.POST
import retrofit2.http.Path
import com.squareup.moshi.Json


// ==========================================
// DATA MODELS (JSON to Kotlin mappings)
// ==========================================

// Request body for admin login
@JsonClass(generateAdapter = true)
data class LoginRequest(

    val login: String,

    val password: String

)


// Response from authentication endpoints
@JsonClass(generateAdapter = true)

data class AuthResponse(

    val success: Boolean,

    val accessToken: String? = null,

    val message: String? = null

)

// Represents a single configuration setup
@JsonClass(generateAdapter = true)
data class ConfigItem(
    val configname: String,
    val currency_shortname: String,
    val creationtimestamp: String?
)

// Request body for changing the physical terminal's login
@JsonClass(generateAdapter = true)
data class ChangeTerminalLoginRequest(
    val newLogin: String
)

// Request body for changing the physical terminal's password
@JsonClass(generateAdapter = true)
data class ChangeTerminalPasswordRequest(
    val newPassword: String
)
// Request body to create a new configuration
@JsonClass(generateAdapter = true)
data class AddConfigRequest(
    val configName: String,
    val currency: String = "PLN"
)

// Request body to delete a configuration
@JsonClass(generateAdapter = true)
data class DeleteConfigRequest(
    val configId: String
)



// Analytics and statistical data for a specific config from blockchain

@JsonClass(generateAdapter = true)
data class AdminStatsResponse(
    val totalOperations: Int?,
    val uniqueClients: Int?,
    val topCrypto: String?,
    val totalVolumeFiat: Double?,
    val fiatCurrency: String?
)

// Total balance across all wallets in FIAT currency in config
@JsonClass(generateAdapter = true)
data class WalletBalanceResponse(
    val totalBalanceFiat: Double?,
    val fiatCurrency: String?
)

// Holds both the config info and its associated wallets
@JsonClass(generateAdapter = true)
data class ConfigDataResponse(
    val config: ConfigItem?,
    val entries: List<WalletEntry>?
)

// Details of a single crypto wallet assigned to a config
@JsonClass(generateAdapter = true)
data class WalletEntry(
    val publickey: String,
    val networkname: String,
    val cryptocurrencyname: String,
    val shortcryptocurrencyname: String
)

// Available cryptocurrency option from the database
@JsonClass(generateAdapter = true)
data class CryptoItem(
    val cryptocurrencyname: String,
    val shortcryptocurrencyname: String
)

// Available blockchain network option for a specific crypto
@JsonClass(generateAdapter = true)
data class NetworkItem(
    val networkname: String,
    val shortnetworkname: String
)

// Request body to link a new wallet to a config
@JsonClass(generateAdapter = true)
data class PostNewSettingRequest(
    val cryptoId: String, // DODANO TO POLE
    val networkId: String,
    val publicKey: String,
    val configId: String
)

// Request body to delete a wallet from a config
@JsonClass(generateAdapter = true)
data class DeleteSettingRequest(
    val cryptoId: String,
    val networkId: String,
    val configId: String,
    val publicKey: String
)

// ==========================================
// API INTERFACE (Network endpoints definition)
// ==========================================



interface ApiService {



    // Authenticate admin and receive JWT + Refresh Token (laf cookie)
    @POST("api/loginAuth")

    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>



    // Refresh the access token (laf cookie is sent automatically by OkHttp CookieJar)

    @GET("api/refreshToken")

    suspend fun refreshToken(): Response<AuthResponse>



    // Clear session cookies on the backend

    @POST("api/logout")

    suspend fun logout(): Response<Unit>



    // Fetch the list of all configurations (shops) for the logged-in admin
    @GET("api/getConfigs")
    suspend fun getConfigs(): Response<List<ConfigItem>>

    // Update terminal credentials
    @POST("api/changeTerminalLogin")
    suspend fun changeTerminalLogin(@Body request: ChangeTerminalLoginRequest): Response<Unit>

    // Zmiana hasła terminala
    @POST("api/changeTerminalPassword")
    suspend fun changeTerminalPassword(@Body request: ChangeTerminalPasswordRequest): Response<Unit>

    // Create a new configuration
    @POST("api/addNewConfig")
    suspend fun addNewConfig(@Body request: AddConfigRequest): Response<Unit>

    // Delete a configuration. Uses @HTTP because standard @DELETE doesn't easily support request bodies
    @HTTP(method = "DELETE", path = "api/deleteConfig", hasBody = true)
    suspend fun deleteConfig(@Body request: DeleteConfigRequest): Response<Unit>

    // Get statistics for a specific config using a path variable
    @GET("api/dataForAdminPanel/{configId}")
    suspend fun getStats(@Path("configId") configId: String): Response<AdminStatsResponse>

    // Get the total FIAT balance for the hidden "eye" feature
    @GET("api/getTotalWalletBalance/{configId}")
    suspend fun getTotalWalletBalance(@Path("configId") configId: String): Response<WalletBalanceResponse>

    // Get configuration details along with its wallets
    @GET("api/getConfigData/{configId}")
    suspend fun getConfigData(@Path("configId") configId: String): Response<ConfigDataResponse>

    // Fetch master list of supported cryptocurrencies
    @GET("api/getCryptoList")
    suspend fun getCryptoList(): Response<List<CryptoItem>>

    // Fetch supported networks for a specific cryptocurrency
    @GET("api/getNetworkList/{cryptoId}")
    suspend fun getNetworkList(@Path("cryptoId") cryptoId: String): Response<List<NetworkItem>>

    // Add a new crypto wallet to the config
    @POST("api/postNewSetting")
    suspend fun postNewSetting(@Body request: PostNewSettingRequest): Response<Unit>

    // Remove a crypto wallet from the config
    @HTTP(method = "DELETE", path = "api/deleteSetting", hasBody = true)
    suspend fun deleteSetting(@Body request: DeleteSettingRequest): Response<Unit>

}