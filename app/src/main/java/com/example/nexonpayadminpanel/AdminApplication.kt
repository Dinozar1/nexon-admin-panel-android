package com.example.nexonpayadminpanel

import android.app.Application
import com.example.nexonpayadminpanel.data.TokenManager
import com.example.nexonpayadminpanel.retrofit.ApiService
import com.example.nexonpayadminpanel.retrofit.RetrofitClient

class AdminApplication : Application() {

    lateinit var tokenManager: TokenManager
        private set

    lateinit var apiService: ApiService
        private set

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(applicationContext)
        apiService = RetrofitClient.getClient(tokenManager)
    }
}