package com.example.tfgiotapp

import android.util.Log
import com.example.tfgiotapp.model.Event
import com.example.tfgiotapp.model.LoginResponse
import com.example.tfgiotapp.model.Room
import com.example.tfgiotapp.model.Schedule
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

class ApiService {
    private val client: OkHttpClient
    private val gson = Gson()

    // IP dinamica en lugar de hardcodeada
    private var baseUrl = "http://192.168.1.57:8080" // IP por defecto

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    // Nuevo método para actualizar la IP del servidor
    fun setServerIp(serverIp: String) {
        baseUrl = if (serverIp.startsWith("http://") || serverIp.startsWith("https://")) {
            serverIp
        } else {
            "http://$serverIp:8080"
        }
        Log.d("ApiService", "Servidor actualizado a: $baseUrl")
    }

    fun getServerUrl(): String = baseUrl

    // Método para validar conectividad con la IP configurada
    fun testConnection(): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/hello")
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error probando conexión: ${e.message}")
            false
        }
    }

    fun updateLight(roomName: String, state: Boolean): Boolean {
        Log.d("ApiService", "Actualizando luz de $roomName a ${if (state) "ON" else "OFF"}")

        val request = Request.Builder()
            .url("$baseUrl/rooms/$roomName/light?state=$state")
            .post(RequestBody.create(null, ""))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                Log.d("ApiService", "Respuesta comando luz: ${response.code}")
                
                if (response.isSuccessful) {
                    // Parsear la nueva respuesta del backend
                    val responseBody = response.body?.string()
                    Log.d("ApiService", "Respuesta luz: $responseBody")
                    
                    try {
                        val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                        val status = jsonResponse.get("status")?.asString
                        Log.d("ApiService", "Status comando luz: $status")
                        
                        // Comando enviado correctamente (aunque pendiente de confirmación IoT)
                        status == "PENDING"
                    } catch (e: Exception) {
                        Log.w("ApiService", "Error parsing respuesta luz: ${e.message}")
                        true // Asumir éxito si no se puede parsear
                    }
                } else {
                    false
                }
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
                Log.d("ApiService", "Respuesta comando sensor: ${response.code}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("ApiService", "Respuesta sensor: $responseBody")
                    
                    try {
                        val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                        val status = jsonResponse.get("status")?.asString
                        Log.d("ApiService", "Status comando sensor: $status")
                        
                        status == "PENDING"
                    } catch (e: Exception) {
                        Log.w("ApiService", "Error parsing respuesta sensor: ${e.message}")
                        true
                    }
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error al actualizar alarma: ${e.message}", e)
            false
        }
    }

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

    fun getRoomSchedules(roomName: String): List<Schedule> {
        val request = Request.Builder()
            .url("$baseUrl/rooms/$roomName/schedules")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()

                val jsonResponse = response.body?.string() ?: "[]"
                return gson.fromJson(jsonResponse, Array<Schedule>::class.java).toList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun createPunctualSchedule(roomName: String, name: String, type: String, state: Boolean, time: String): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/rooms/$roomName/schedules?name=$name&type=$type&state=$state&time=$time")
            .post(RequestBody.create(null, ""))
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

    fun createIntervalSchedule(roomName: String, name: String, type: String, startTime: String, endTime: String): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/rooms/$roomName/schedules?name=$name&type=$type&state=true&startTime=$startTime&endTime=$endTime")
            .post(RequestBody.create(null, ""))
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

    fun deleteSchedule(roomName: String, scheduleId: Long): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/rooms/$roomName/schedules/$scheduleId")
            .delete()
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

    fun login(username: String, password: String): LoginResponse? {
        val credentials = mapOf(
            "username" to username,
            "password" to password
        )

        val jsonBody = gson.toJson(credentials)
        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            jsonBody
        )

        val request = Request.Builder()
            .url("$baseUrl/auth/login")
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: return null

                if (response.isSuccessful) {
                    gson.fromJson(responseBody, LoginResponse::class.java)
                } else {
                    // Error HTTP específico (401, 403, etc.)
                    val errorResponse = try {
                        gson.fromJson(responseBody, LoginResponse::class.java)
                    } catch (e: Exception) {
                        LoginResponse(false, "Error de autenticacion: ${response.code}", null)
                    }
                    errorResponse
                }
            }
        } catch (e: java.net.ConnectException) {
            Log.e("ApiService", "Error de conexion - servidor no alcanzable: ${e.message}", e)
            LoginResponse(false, "SERVER_UNREACHABLE", null)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("ApiService", "Timeout de conexion: ${e.message}", e)
            LoginResponse(false, "CONNECTION_TIMEOUT", null)
        } catch (e: java.net.UnknownHostException) {
            Log.e("ApiService", "Host no encontrado: ${e.message}", e)
            LoginResponse(false, "HOST_NOT_FOUND", null)
        } catch (e: Exception) {
            Log.e("ApiService", "Error general en login: ${e.message}", e)
            LoginResponse(false, "NETWORK_ERROR", null)
        }
    }

    fun createRoom(roomName: String, username: String): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/rooms?roomName=$roomName&username=$username")
            .post(RequestBody.create(null, ""))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                    jsonResponse.get("success")?.asBoolean ?: false
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error creando habitacion: ${e.message}", e)
            false
        }
    }

     fun getAllEvents(): List<Event> {
        Log.d("ApiService", "Obteniendo todos los eventos")

        val request = Request.Builder()
            .url("$baseUrl/events/recent?hours=168") // últimas 168 horas (7 días)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val eventListType = object : TypeToken<List<Event>>() {}.type
                    gson.fromJson(responseBody, eventListType) ?: emptyList()
                } else {
                    Log.e("ApiService", "Error al obtener eventos: ${response.code}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error al obtener eventos: ${e.message}", e)
            emptyList()
        }
    }

    fun activateVacationMode(): Boolean {
        Log.d("ApiService", "Activando modo vacaciones")

        val request = Request.Builder()
            .url("$baseUrl/vacation-mode/activate")
            .post(RequestBody.create(null, ""))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("ApiService", "Modo vacaciones activado")
                    true
                } else {
                    Log.e("ApiService", "Error al activar modo vacaciones: ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error al activar modo vacaciones: ${e.message}", e)
            false
        }
    }

    fun deactivateVacationMode(): Boolean {
        Log.d("ApiService", "Desactivando modo vacaciones")

        val request = Request.Builder()
            .url("$baseUrl/vacation-mode/deactivate")
            .post(RequestBody.create(null, ""))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("ApiService", "Modo vacaciones desactivado")
                    true
                } else {
                    Log.e("ApiService", "Error al desactivar modo vacaciones: ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error al desactivar modo vacaciones: ${e.message}", e)
            false
        }
    }

    fun getVacationModeStatus(): Boolean {
        Log.d("ApiService", "Obteniendo estado del modo vacaciones")

        val request = Request.Builder()
            .url("$baseUrl/vacation-mode/status")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                    val isActive = jsonResponse.get("active")?.asBoolean ?: false
                    Log.d("ApiService", "Estado modo vacaciones: $isActive")
                    isActive
                } else {
                    Log.e("ApiService", "Error al obtener estado modo vacaciones: ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error al obtener estado modo vacaciones: ${e.message}", e)
            false
        }
    }
}