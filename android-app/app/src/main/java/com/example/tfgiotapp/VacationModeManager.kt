package com.example.tfgiotapp

import android.content.Context
import android.content.SharedPreferences

class VacationModeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vacation_mode", Context.MODE_PRIVATE)
    
    fun setVacationModeActive(isActive: Boolean) {
        prefs.edit().putBoolean("vacation_mode_active", isActive).apply()
    }
    
    fun isVacationModeActive(): Boolean {
        return prefs.getBoolean("vacation_mode_active", false)
    }
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}