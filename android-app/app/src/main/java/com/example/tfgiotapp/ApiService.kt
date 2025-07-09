package com.example.tfgiotapp

import com.example.tfgiotapp.model.Room
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

class ApiService {
    private val client: OkHttpClient
    private val gson = Gson()

    // Cambia esta IP por la IP de tu backend (localhost no funciona en emulador)
    private val baseUrl = "http://10.0.2.2:8080" // Para emulador Android
    // Si usas dispositivo físico: "http://TU_IP_LOCAL:8080"

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    fun getAllRooms(): List<Room> {
        val request = Request.Builder()
            .url("$baseUrl/rooms")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Error: ${response.code}")

                val jsonResponse = response.body?.string() ?: "[]"
                return gson.fromJson(jsonResponse, Array<Room>::class.java).toList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun getRoomByName(roomName: String): Room? {
        val request = Request.Builder()
            .url("$baseUrl/rooms/$roomName")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null

                val jsonResponse = response.body?.string() ?: return null
                return gson.fromJson(jsonResponse, Room::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}