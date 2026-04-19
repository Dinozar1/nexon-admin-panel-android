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

    // In-memory storage for cookies (holds the 'laf' refresh token)
    private val cookieJar = object : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: listOf()
        }
    }

    // --- JWT DECODING LOGIC ---
    // Checks if the JWT token is expired by reading its internal payload
    private fun isTokenExpired(token: String?): Boolean {
        if (token.isNullOrEmpty()) return true
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true // Invalid format

            // Decode the middle part (payload) of the JWT
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val jsonObject = JSONObject(payload)

            // Read the expiration timestamp
            val exp = jsonObject.optLong("exp", 0)
            if (exp == 0L) return true

            // Expired if less than 10 seconds remain
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            exp <= (currentTimeSeconds + 10)
        } catch (e: Exception) {
            Log.e("JWT_DECODE", "Error decoding JWT: ${e.message}")
            true // Assume expired on error
        }
    }

    // Safely requests a new token. @Synchronized prevents multiple requests at once.
    @Synchronized
    private fun refreshTokenSync(tokenManager: TokenManager, loggingInterceptor: HttpLoggingInterceptor): String? {
        val currentToken = tokenManager.getAccessToken()

        // Stop if another thread already refreshed the token
        if (!isTokenExpired(currentToken)) {
            return currentToken
        }

        Log.d("API_AUTH", "Token expired. Starting refresh...")

        // Simple client just for the refresh request
        val refreshClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(loggingInterceptor)
            .build()

        val request = Request.Builder()
            .url("${BASE_URL}api/refreshToken")
            .get()
            .build()

        try {
            // Block thread and wait for the new token
            val response = refreshClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (bodyString != null) {
                    val jsonObject = JSONObject(bodyString)
                    val success = jsonObject.optBoolean("success", false)
                    val newAccessToken = jsonObject.optString("accessToken", "")

                    if (success && newAccessToken.isNotEmpty()) {
                        Log.d("API_AUTH", "Token successfully refreshed.")
                        tokenManager.saveAccessToken(newAccessToken) // Save new token
                        return newAccessToken
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("API_AUTH", "Refresh network error: ${e.message}")
        }

        // Logout if refresh fails completely
        Log.e("API_AUTH", "Refresh failed. Logging out...")
        tokenManager.deleteTokens()
        return null
    }

    // Builds the main API client with all interceptors attached
    fun getClient(tokenManager: TokenManager): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("API_NETWORK", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // FALLBACK: Catches 401 errors from the server and retries with a new token
        val tokenAuthenticator = Authenticator { _, response ->
            // Don't retry if the refresh or login endpoints fail
            if (response.request.url.encodedPath.contains("refreshToken") || response.request.url.encodedPath.contains("loginAuth")) {
                return@Authenticator null
            }

            val newToken = refreshTokenSync(tokenManager, loggingInterceptor)
            if (newToken != null) {
                // Retry the original request
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }
            null
        }

        // MAIN CLIENT SETUP
        val okHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()

                // Skip token logic for login and refresh endpoints
                if (originalRequest.url.encodedPath.contains("loginAuth") || originalRequest.url.encodedPath.contains("refreshToken")) {
                    return@addInterceptor chain.proceed(originalRequest)
                }

                var token = tokenManager.getAccessToken()

                // PROACTIVE CHECK: Refresh token *before* sending if it's expired
                if (isTokenExpired(token)) {
                    Log.d("API_AUTH", "Proactive check: Token expired!")
                    token = refreshTokenSync(tokenManager, loggingInterceptor)
                }

                // Attach token to headers
                val requestBuilder = originalRequest.newBuilder()
                token?.let {
                    requestBuilder.header("Authorization", "Bearer $it")
                }

                chain.proceed(requestBuilder.build()) // Send the request
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