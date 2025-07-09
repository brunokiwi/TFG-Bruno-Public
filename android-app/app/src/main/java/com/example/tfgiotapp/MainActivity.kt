package com.example.tfgiotapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgiotapp.RoomAdapter
import com.example.tfgiotapp.model.Room
import com.example.tfgiotapp.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomAdapter: RoomAdapter
    private val apiService = ApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainlayout)

        setupRecyclerView()
        checkConnectionAndLoadRooms()
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar datos cuando regrese de otra actividad
        loadRooms()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewRooms)
        roomAdapter = RoomAdapter(emptyList()) { room ->
            openRoomDetail(room)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = roomAdapter
    }

    private fun checkConnectionAndLoadRooms() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // probar conexion
                val isConnected = apiService.checkServerConnection()

                withContext(Dispatchers.Main) {
                    if (isConnected) {
                        Toast.makeText(
                            this@MainActivity,
                            "✅ Conectado al servidor correctamente",
                            Toast.LENGTH_LONG
                        ).show()

                        // 2. Si está conectado, cargar habitaciones
                        loadRooms()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "❌ Error: No se pudo conectar al servidor",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "❌ Error de conexión: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadRooms() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rooms = apiService.getAllRooms()
                withContext(Dispatchers.Main) {
                    roomAdapter.updateRooms(rooms)
                    if (rooms.isEmpty()) {
                        Toast.makeText(this@MainActivity, "No hay habitaciones registradas", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error al cargar habitaciones: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openRoomDetail(room: Room) {
        val intent = Intent(this, RoomDetailActivity::class.java)
        intent.putExtra("roomName", room.name)
        startActivity(intent)
    }
}