package com.example.tfgiotapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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

    private lateinit var createRoomButton: Button
    private lateinit var logoutButton: Button
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        userPreferences = UserPreferences(this)
        
        // Verificar autenticación
        if (!userPreferences.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.mainlayout)

        setupRecyclerView()
        setupUpdateButton()
        setupAdminButtons()
        
        requestNotificationPermission()
        subscribeToMovementAlerts()
        checkConnectionAndLoadRooms()
    }
    
    private fun setupAdminButtons() {
        createRoomButton = findViewById(R.id.createRoomButton)
        logoutButton = findViewById(R.id.logoutButton)
        
        // Mostrar botón crear habitación solo a admins
        if (userPreferences.isAdmin()) {
            createRoomButton.visibility = View.VISIBLE
            createRoomButton.setOnClickListener {
                showCreateRoomDialog()
            }
        } else {
            createRoomButton.visibility = View.GONE
        }
        
        logoutButton.setOnClickListener {
            logout()
        }

    }
    
    private fun showCreateRoomDialog() {
        val builder = AlertDialog.Builder(this)
        val input = EditText(this)
        input.hint = "Nombre de la habitación"
        
        builder.setTitle("Crear Nueva Habitación")
        builder.setView(input)
        
        builder.setPositiveButton("Crear") { _, _ ->
            val roomName = input.text.toString().trim()
            if (roomName.isNotEmpty()) {
                createNewRoom(roomName)
            } else {
                Toast.makeText(this, "Por favor ingresa un nombre", Toast.LENGTH_SHORT).show()
            }
        }
        
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        
        builder.show()
    }
    
    private fun createNewRoom(roomName: String) {
        val currentUser = userPreferences.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = apiService.createRoom(roomName, currentUser.username)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@MainActivity, "Habitación '$roomName' creada exitosamente", Toast.LENGTH_SHORT).show()
                        loadRooms()
                    } else {
                        Toast.makeText(this@MainActivity, "Error al crear la habitación", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun logout() {
        userPreferences.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    
    override fun onResume() {
        super.onResume()
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
                        "Error de conexión: ${e.message}",
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