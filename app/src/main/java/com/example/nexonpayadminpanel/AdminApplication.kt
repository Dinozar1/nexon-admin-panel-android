// app/src/main/java/com/example/nexonpayadminpanel/AdminApplication.kt
package com.example.nexonpayadminpanel

import android.app.Application
import com.example.nexonpayadminpanel.data.TokenManager
import com.example.nexonpayadminpanel.retrofit.ApiService
import com.example.nexonpayadminpanel.retrofit.RetrofitClient

// The base Application class. It runs before any UI or Activity starts.
class AdminApplication : Application() {

    // Global instance of TokenManager, initialized later
    lateinit var tokenManager: TokenManager
        private set // Can be read globally, but only modified inside this class

    // Global instance of the API client
    lateinit var apiService: ApiService
        private set

    // Called exactly once when the app is launched
    override fun onCreate() {
        super.onCreate()

        // Initialize secure storage using the app's global context
        tokenManager = TokenManager(applicationContext)

        // Build the single, global API client and pass the token manager to it
        apiService = RetrofitClient.getClient(tokenManager)
    }
}