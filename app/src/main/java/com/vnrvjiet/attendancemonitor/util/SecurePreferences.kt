package com.vnrvjiet.attendancemonitor.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException

object SecurePreferences {
    private const val PREFS_NAME = "secure_attendance_prefs"
    private const val TAG = "SecurePreferences"

    fun getPrefs(context: Context): SharedPreferences {
        return try {
            createPrefs(context)
        } catch (e: Exception) {
            // Catch specific security/IO exceptions that indicate corruption or key loss
            if (e is GeneralSecurityException || e is IOException || e.message?.contains("AEADBadTagException") == true) {
                Log.e(TAG, "Corrupted encrypted preferences detected. Wiping for recovery.", e)
                
                // 1. Wipe the physical shared preferences file
                context.deleteSharedPreferences(PREFS_NAME)
                
                // 2. Re-attempt creation (this will generate a fresh file and master key mapping)
                try {
                    createPrefs(context)
                } catch (retryException: Exception) {
                    Log.e(TAG, "Critical: Total failure of Secure Keystore. Falling back to unencrypted storage.", retryException)
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            } else {
                // Unexpected error, rethrow to avoid hiding logical bugs
                throw e
            }
        }
    }

    private fun createPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
