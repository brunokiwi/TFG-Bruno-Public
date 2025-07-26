package com.example.tfgiotapp

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    fun saveUser(user: com.example.tfgiotapp.model.User) {
        prefs.edit().apply {
            putLong("user_id", user.id)
            putString("username", user.username)
            putString("role", user.role)
            putString("rfidUid", user.rfidUid)
            putBoolean("is_logged_in", true)
            apply()
        }
    }
    
    fun getCurrentUser(): com.example.tfgiotapp.model.User? {
        return if (isLoggedIn()) {
            com.example.tfgiotapp.model.User(
                id = prefs.getLong("user_id", -1),
                username = prefs.getString("username", "") ?: "",
                role = prefs.getString("role", "") ?: "",
                rfidUid = prefs.getString("rfidUid", null)
            )
        } else null
    }
    
    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)
    
    fun isAdmin(): Boolean = prefs.getString("role", "") == "ADMIN"
    
    fun logout() {
        prefs.edit().clear().apply()
    }
}