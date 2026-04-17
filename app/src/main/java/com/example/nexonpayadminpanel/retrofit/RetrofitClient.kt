// app/src/main/java/com/example/nexonpayadminpanel/retrofit/RetrofitClient.kt
package com.example.nexonpayadminpanel.retrofit

import android.util.Base64
import android.util.Log
import com.example.nexonpayadminpanel.data.TokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.nexonpay.pl/"

    // Słoik na ciastka trzymający nasz Refresh Token ('laf')
    private val cookieJar = object : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: listOf()
        }
    }

    // --- MAGIA DEKODOWANIA JWT ---
    private fun isTokenExpired(token: String?): Boolean {
        if (token.isNullOrEmpty()) return true
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true // Niepoprawny format JWT

            // Rozkodowujemy Payload (środkowa część tokena)
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val jsonObject = JSONObject(payload)

            // Pobieramy czas wygaśnięcia (w sekundach)
            val exp = jsonObject.optLong("exp", 0)
            if (exp == 0L) return true

            // Sprawdzamy, czy czas wygaśnięcia minie za mniej niż 10 sekund (bufor bezpieczeństwa)
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            exp <= (currentTimeSeconds + 10)
        } catch (e: Exception) {
            Log.e("JWT_DECODE", "Błąd dekodowania JWT: ${e.message}")
            true // W razie błędu zakładamy, że wygasł
        }
    }

    // Zsynchronizowana funkcja do odświeżania tokena (żeby 5 requestów naraz nie odpaliło 5 odświeżeń)
    @Synchronized
    private fun refreshTokenSync(tokenManager: TokenManager, loggingInterceptor: HttpLoggingInterceptor): String? {
        val currentToken = tokenManager.getAccessToken()

        // Zabezpieczenie: jeśli jakiś inny wątek ułamek sekundy temu już odświeżył token i jest on ważny, to po prostu go zwracamy
        if (!isTokenExpired(currentToken)) {
            return currentToken
        }

        Log.d("API_AUTH", "Token wygasł w payloadzie (lub dostaliśmy 401). Rozpoczynam odświeżanie...")

        val refreshClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(loggingInterceptor)
            .build()

        val request = Request.Builder()
            .url("${BASE_URL}api/refreshToken")
            .get()
            .build()

        try {
            val response = refreshClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (bodyString != null) {
                    val jsonObject = JSONObject(bodyString)
                    val success = jsonObject.optBoolean("success", false)
                    val newAccessToken = jsonObject.optString("accessToken", "")

                    if (success && newAccessToken.isNotEmpty()) {
                        Log.d("API_AUTH", "Sukces! Pomyślnie odświeżono token.")
                        tokenManager.saveAccessToken(newAccessToken)
                        return newAccessToken
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("API_AUTH", "Błąd sieci przy odświeżaniu: ${e.message}")
        }

        Log.e("API_AUTH", "Nie udało się odświeżyć tokena. Wylogowuję...")
        tokenManager.deleteTokens()
        return null
    }

    fun getClient(tokenManager: TokenManager): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("API_NETWORK", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 1. AUTENTYKATOR (Koło ratunkowe, gdyby backend zwrócił 401 mimo ważnego payloadu)
        val tokenAuthenticator = Authenticator { _, response ->
            if (response.request.url.encodedPath.contains("refreshToken") || response.request.url.encodedPath.contains("loginAuth")) {
                return@Authenticator null
            }

            val newToken = refreshTokenSync(tokenManager, loggingInterceptor)
            if (newToken != null) {
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }
            null
        }

        // =========================================================
        // SKŁADANIE GŁÓWNEGO KLIENTA
        // =========================================================
        val okHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()

                // Pomijamy proaktywne sprawdzanie dla logowania i samego odświeżania!
                if (originalRequest.url.encodedPath.contains("loginAuth") || originalRequest.url.encodedPath.contains("refreshToken")) {
                    return@addInterceptor chain.proceed(originalRequest)
                }

                var token = tokenManager.getAccessToken()

                // 2. PROAKTYWNE SPRAWDZANIE JWT (Twój odpowiednik z Reacta!)
                // Zanim w ogóle uderzymy do API, sprawdzamy, czy token nie jest przeterminowany.
                if (isTokenExpired(token)) {
                    Log.d("API_AUTH", "Proaktywny Interceptor wykrył wygasły token!")
                    // Wstrzymujemy zapytanie i odświeżamy token synchronicznie
                    token = refreshTokenSync(tokenManager, loggingInterceptor)
                }

                val requestBuilder = originalRequest.newBuilder()
                token?.let {
                    requestBuilder.header("Authorization", "Bearer $it")
                }

                chain.proceed(requestBuilder.build())
            }
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}