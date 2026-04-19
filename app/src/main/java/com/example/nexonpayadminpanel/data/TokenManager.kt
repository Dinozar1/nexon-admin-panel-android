// data/TokenManager.kt
package com.example.nexonpayadminpanel.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    // Generate a secure master key stored safely in Android Keystore
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Create encrypted preferences (encrypts both the key names and their values)
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Save the JWT token securely to the encrypted file
    fun saveAccessToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }

    // Retrieve and automatically decrypt the JWT token
    fun getAccessToken(): String? = prefs.getString("access_token", null)

    // Remove the token safely (used for logout or session expiration)
    fun deleteTokens() {
        prefs.edit().remove("access_token").apply()
    }
}