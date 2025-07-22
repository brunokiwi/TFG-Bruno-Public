package com.example.tfgiotapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgiotapp.RoomAdapter
import com.example.tfgiotapp.model.Room
import com.example.tfgiotapp.ApiService
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomAdapter: RoomAdapter
    private lateinit var updateButton: Button
    private val apiService = ApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainlayout)

        setupRecyclerView()
        setupUpdateButton()
        // notificatio nstuf
        requestNotificationPermission()
        subscribeToMovementAlerts()
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

    private fun setupUpdateButton() {
        updateButton = findViewById(R.id.updateButton)
        updateButton.setOnClickListener {
            loadRooms()
        }
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Permiso de notificaciones concedido")
        } else {
            Log.d("MainActivity", "Permiso de notificaciones denegado")
        }
    }

    private fun subscribeToMovementAlerts() {
        FirebaseMessaging.getInstance().subscribeToTopic("movement_alerts")
            .addOnCompleteListener { task ->
                var msg = "Suscrito a alertas de movimiento"
                if (!task.isSuccessful) {
                    msg = "Error en suscripcion"
                }
                Log.d("MainActivity", msg)
            }
    }
}