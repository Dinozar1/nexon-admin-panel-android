package com.example.nexonpayadminpanel.retrofit



import com.squareup.moshi.JsonClass

import retrofit2.Response

import retrofit2.http.Body

import retrofit2.http.GET
import retrofit2.http.HTTP

import retrofit2.http.POST
import retrofit2.http.Path
import com.squareup.moshi.Json


@JsonClass(generateAdapter = true)

data class LoginRequest(

    val login: String,

    val password: String

)



@JsonClass(generateAdapter = true)

data class AuthResponse(

    val success: Boolean,

    val accessToken: String? = null,

    val message: String? = null

)

@JsonClass(generateAdapter = true)
data class ConfigItem(
    val configname: String,
    val currency_shortname: String,
    val creationtimestamp: String? // Używamy String, bo API pewnie zwraca datę w formacie ISO
)

@JsonClass(generateAdapter = true)
data class ChangeTerminalLoginRequest(
    val newLogin: String
)

@JsonClass(generateAdapter = true)
data class ChangeTerminalPasswordRequest(
    val newPassword: String
)

@JsonClass(generateAdapter = true)
data class AddConfigRequest(
    val configName: String,
    val currency: String = "PLN"
)

@JsonClass(generateAdapter = true)
data class DeleteConfigRequest(
    val configId: String
)



// --- MODELE DLA SZCZEGÓŁÓW KONFIGURACJI ---

@JsonClass(generateAdapter = true)
data class AdminStatsResponse(
    val totalOperations: Int?,
    val uniqueClients: Int?,
    val topCrypto: String?,
    val totalVolumeFiat: Double?,
    val fiatCurrency: String?
)

@JsonClass(generateAdapter = true)
data class WalletBalanceResponse(
    val totalBalanceFiat: Double?,
    val fiatCurrency: String?
)

@JsonClass(generateAdapter = true)
data class ConfigDataResponse(
    val config: ConfigItem?,
    val entries: List<WalletEntry>?
)

@JsonClass(generateAdapter = true)
data class WalletEntry(
    val publickey: String,
    val networkname: String,
    val cryptocurrencyname: String,
    val shortcryptocurrencyname: String
)

@JsonClass(generateAdapter = true)
data class CryptoItem(
    val cryptocurrencyname: String,
    val shortcryptocurrencyname: String
)

@JsonClass(generateAdapter = true)
data class NetworkItem(
    val networkname: String,
    val shortnetworkname: String
)

@JsonClass(generateAdapter = true)
data class PostNewSettingRequest(
    val cryptoId: String, // DODANO TO POLE
    val networkId: String,
    val publicKey: String,
    val configId: String
)

@JsonClass(generateAdapter = true)
data class DeleteSettingRequest(
    val cryptoId: String,
    val networkId: String,
    val configId: String,
    val publicKey: String
)

// --- INTERFEJS API ---



interface ApiService {



// Logowanie Admina

    @POST("api/loginAuth")

    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>



// Odświeżanie tokena (ciasteczko laf poleci automatycznie dzięki CookieJar)

    @GET("api/refreshToken")

    suspend fun refreshToken(): Response<AuthResponse>



// Wylogowanie

    @POST("api/logout")

    suspend fun logout(): Response<Unit>



    // Pobieranie listy konfiguracji (sklepów)
    @GET("api/getConfigs")
    suspend fun getConfigs(): Response<List<ConfigItem>>

    // Zmiana loginu terminala
    @POST("api/changeTerminalLogin")
    suspend fun changeTerminalLogin(@Body request: ChangeTerminalLoginRequest): Response<Unit>

    // Zmiana hasła terminala
    @POST("api/changeTerminalPassword")
    suspend fun changeTerminalPassword(@Body request: ChangeTerminalPasswordRequest): Response<Unit>

    // Dodawanie nowej konfiguracji
    @POST("api/addNewConfig")
    suspend fun addNewConfig(@Body request: AddConfigRequest): Response<Unit>

    // Usuwanie konfiguracji (Retrofit wymaga @HTTP dla DELETE z body)
    @HTTP(method = "DELETE", path = "api/deleteConfig", hasBody = true)
    suspend fun deleteConfig(@Body request: DeleteConfigRequest): Response<Unit>

    // --- SZCZEGÓŁY KONFIGURACJI ---

    @GET("api/dataForAdminPanel/{configId}")
    suspend fun getStats(@Path("configId") configId: String): Response<AdminStatsResponse>

    @GET("api/getTotalWalletBalance/{configId}")
    suspend fun getTotalWalletBalance(@Path("configId") configId: String): Response<WalletBalanceResponse>

    @GET("api/getConfigData/{configId}")
    suspend fun getConfigData(@Path("configId") configId: String): Response<ConfigDataResponse>

    @GET("api/getCryptoList")
    suspend fun getCryptoList(): Response<List<CryptoItem>>

    @GET("api/getNetworkList/{cryptoId}")
    suspend fun getNetworkList(@Path("cryptoId") cryptoId: String): Response<List<NetworkItem>>

    @POST("api/postNewSetting")
    suspend fun postNewSetting(@Body request: PostNewSettingRequest): Response<Unit>

    @HTTP(method = "DELETE", path = "api/deleteSetting", hasBody = true)
    suspend fun deleteSetting(@Body request: DeleteSettingRequest): Response<Unit>

}