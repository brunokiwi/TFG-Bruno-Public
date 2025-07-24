package com.example.tfgiotapp

import android.content.Context
import android.content.SharedPreferences

class ServerPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("server_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val SERVER_IP_KEY = "server_ip"
        private const val DEFAULT_IP = "192.168.1.1"
    }
    
    fun saveServerIp(ip: String) {
        prefs.edit().putString(SERVER_IP_KEY, ip).apply()
    }
    
    fun getServerIp(): String {
        return prefs.getString(SERVER_IP_KEY, DEFAULT_IP) ?: DEFAULT_IP
    }
    
    fun hasCustomIp(): Boolean {
        return prefs.contains(SERVER_IP_KEY)
    }
    
    fun clearServerIp() {
        prefs.edit().remove(SERVER_IP_KEY).apply()
    }
}