package com.example.tfgiotapp

import android.util.Log
import com.example.tfgiotapp.model.Room
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

class ApiService {
    private val client: OkHttpClient
    private val gson = Gson()

    // Cambiar IP según tu configuración
    private val baseUrl = "http://192.168.1.57:8080"

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    fun updateLight(roomName: String, state: Boolean): Boolean {
        Log.d("ApiService", "Actualizando luz de $roomName a ${if (state) "ON" else "OFF"}")
        
        val request = Request.Builder()
            .url("$baseUrl/rooms/$roomName/light?state=$state")
            .post(RequestBody.create(null, ""))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                Log.d("ApiService", "Respuesta luz: ${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error al actualizar luz: ${e.message}", e)
            false
        }
    }

    fun updateAlarm(roomName: String, state: Boolean): Boolean {
        Log.d("ApiService", "Actualizando alarma de $roomName a ${if (state) "ON" else "OFF"}")
        
        val request = Request.Builder()
            .url("$baseUrl/rooms/$roomName/alarm?state=$state")
            .post(RequestBody.create(null, ""))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                Log.d("ApiService", "Respuesta alarma: ${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error al actualizar alarma: ${e.message}", e)
            false
        }
    }

    // Resto de métodos existentes...
    fun checkServerConnection(): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/hello")
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