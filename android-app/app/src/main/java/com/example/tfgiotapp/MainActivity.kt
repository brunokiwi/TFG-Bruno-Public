package com.example.tfgiotapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgiotapp.RoomAdapter
import com.example.tfgiotapp.model.Room
import com.example.tfgiotapp.ApiService
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomAdapter: RoomAdapter
    private lateinit var updateButton: Button
    private val apiService = ApiService()

    private lateinit var userPreferences: UserPreferences
    private lateinit var vacationModeManager: VacationModeManager
    private lateinit var serverPreferences: ServerPreferences

    private lateinit var menuButton: Button
    private lateinit var vacationModeLabel: TextView

    private var rfidRegistrationInProgress = false
    private var rfidRegistrationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userPreferences = UserPreferences(this)
        vacationModeManager = VacationModeManager(this)
        serverPreferences = ServerPreferences(this)

        if (!userPreferences.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupApiService()

        setContentView(R.layout.mainlayout)

        menuButton = findViewById(R.id.menuButton)
        vacationModeLabel = findViewById(R.id.vacationModeLabel)

        menuButton.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

            // boton rojo cerrar sesion
            val logoutMenuItem = popup.menu.findItem(R.id.menu_logout)
            val spannableTitle = android.text.SpannableString(logoutMenuItem.title)
            spannableTitle.setSpan(
                android.text.style.ForegroundColorSpan(android.graphics.Color.RED),
                0, spannableTitle.length, 0
            )
            logoutMenuItem.title = spannableTitle

            // permisos
            popup.menu.findItem(R.id.menu_create_room).isVisible = userPreferences.isAdmin()
            popup.menu.findItem(R.id.menu_events).isVisible = userPreferences.isAdmin()
            popup.menu.findItem(R.id.menu_vacation_mode).isVisible = userPreferences.isAdmin()
            popup.menu.findItem(R.id.menu_rfid).isVisible = true

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_create_room -> {
                        if (!vacationModeManager.isVacationModeActive()) showCreateRoomDialog()
                        else Toast.makeText(this, "No se pueden crear habitaciones en modo vacaciones", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.menu_events -> {
                        startActivity(Intent(this, EventsActivity::class.java))
                        true
                    }
                    R.id.menu_vacation_mode -> {
                        toggleVacationMode()
                        true
                    }
                    R.id.menu_rfid -> {
                        showRfidDialog()
                        true
                    }
                    R.id.menu_logout -> {
                        logout()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        setupRecyclerView()
        setupUpdateButton()

        requestNotificationPermission()
        subscribeToMovementAlerts()
        checkConnectionAndLoadRooms()
        checkVacationModeStatus()
    }

    private fun setupApiService() {
        val savedIp = serverPreferences.getServerIp()
        apiService.setServerIp(savedIp)
    }

    private fun checkVacationModeStatus() {
        val serverPreferences = ServerPreferences(this)
        val savedIp = serverPreferences.getServerIp()
        apiService.setServerIp(savedIp)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isActive = apiService.getVacationModeStatus()
                withContext(Dispatchers.Main) {
                    vacationModeManager.setVacationModeActive(isActive)
                    updateUIBasedOnVacationMode(isActive)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al verificar estado del modo vacaciones: ${e.message}")
            }
        }
    }

    private fun toggleVacationMode() {
        val isCurrentlyActive = vacationModeManager.isVacationModeActive()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = if (isCurrentlyActive) {
                    apiService.deactivateVacationMode()
                } else {
                    apiService.activateVacationMode()
                }

                withContext(Dispatchers.Main) {
                    if (success) {
                        val newState = !isCurrentlyActive
                        vacationModeManager.setVacationModeActive(newState)
                        updateUIBasedOnVacationMode(newState)

                        val message = if (newState) {
                            "Modo vacaciones activado"
                        } else {
                            "Modo vacaciones desactivado"
                        }
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Error al cambiar el modo vacaciones",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateUIBasedOnVacationMode(isActive: Boolean) {
        // Deshabilita botones si es necesario (puedes ajustar según tus necesidades)
        updateButton.alpha = if (isActive) 0.5f else 1.0f
        updateButton.isEnabled = !isActive

        // Muestra el aviso de modo vacaciones
        vacationModeLabel.visibility = if (isActive) View.VISIBLE else View.GONE

        // Cambia el color de las cards de habitaciones
        roomAdapter.setVacationModeActive(isActive)
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
                        Toast.makeText(
                            this@MainActivity,
                            "Habitación '$roomName' creada con éxito",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadRooms()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Error al crear la habitación",
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

    private fun logout() {
        userPreferences.logout()
        vacationModeManager.clear()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        loadRooms()
        checkVacationModeStatus()
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
            if (!vacationModeManager.isVacationModeActive()) {
                loadRooms()
            } else {
                Toast.makeText(
                    this,
                    "No se pueden actualizar datos en modo vacaciones",
                    Toast.LENGTH_SHORT
                ).show()
            }
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
                            "Conectado al servidor correctamente",
                            Toast.LENGTH_LONG
                        ).show()

                        loadRooms()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Error: No se pudo conectar al servidor",
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
                        Toast.makeText(
                            this@MainActivity,
                            "No hay habitaciones registradas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error al cargar habitaciones: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun openRoomDetail(room: Room) {
        if (!vacationModeManager.isVacationModeActive()) {
            val intent = Intent(this, RoomDetailActivity::class.java)
            intent.putExtra("roomName", room.name)
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                "No se pueden modificar habitaciones en modo vacaciones",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {

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

    private fun showRfidDialog() {
        val currentUser = userPreferences.getCurrentUser() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val rfidUid = apiService.getRfidUid(currentUser.username)
            withContext(Dispatchers.Main) {
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Tarjeta RFID")
                val message = if (rfidUid != null && rfidUid.isNotEmpty())
                    "UID actual: $rfidUid"
                else
                    "No tienes tarjeta RFID registrada"
                builder.setMessage(message)
                builder.setPositiveButton("Registrar nueva") { _, _ ->
                    registerRfid(currentUser.username)
                }
                builder.setNegativeButton("Cerrar", null)
                builder.show()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerRfid(username: String) {
        if (rfidRegistrationInProgress) return
        rfidRegistrationInProgress = true

        var infoDialog: AlertDialog? = null
        rfidRegistrationJob = CoroutineScope(Dispatchers.IO).launch {
            apiService.initiateRfidRegistration(username)
        }
        
        infoDialog = AlertDialog.Builder(this)
            .setTitle("Registro de tarjeta RFID")
            .setMessage("Pase una tarjeta RFID por el sensor para asociarla a su usuario. Tiene 30 segundos.")
            .setCancelable(false)
            .setNegativeButton("Cancelar") { _, _ ->
                cancelRfidRegistration(username)
                infoDialog?.dismiss()
            }
            .create()

        infoDialog.show()
    }

    private fun cancelRfidRegistration(username: String) {
        rfidRegistrationJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            apiService.cancelRfidRegistration(username)
        }
        rfidRegistrationInProgress = false
    }
}
