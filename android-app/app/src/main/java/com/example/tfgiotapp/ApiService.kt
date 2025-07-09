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

    // ip del pc "http://TU_IP_LOCAL:8080"
    private val baseUrl = "http://192.168.1.57:8080"

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    // check conexion
    fun checkServerConnection(): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/hello")  // Endpoint simple para verificar conexión
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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