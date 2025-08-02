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

    private var globalToast: Toast? = null

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
                        else showSingleToast("No se pueden crear habitaciones en modo vacaciones")
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
                        showSingleToast(message)
                    } else {
                        showSingleToast("Error al cambiar el modo vacaciones")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSingleToast("Error de conexión: ${e.message}")
                }
            }
        }
    }

    private fun updateUIBasedOnVacationMode(isActive: Boolean) {
        updateButton.alpha = if (isActive) 0.5f else 1.0f
        updateButton.isEnabled = !isActive

        vacationModeLabel.visibility = if (isActive) View.VISIBLE else View.GONE

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
                showSingleToast("Por favor ingresa un nombre")
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
            showSingleToast("Error: Usuario no autenticado")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = apiService.createRoom(roomName, currentUser.username)

                withContext(Dispatchers.Main) {
                    if (success) {
                        showSingleToast("Habitación '$roomName' creada con éxito")
                        loadRooms()
                    } else {
                        showSingleToast("Error al crear la habitación")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSingleToast("Error de conexión: ${e.message}")
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

    private fun showSingleToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        globalToast?.cancel()
        globalToast = Toast.makeText(this, message, duration)
        globalToast?.show()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewRooms)
        roomAdapter = RoomAdapter(
            emptyList(),
            { room -> openRoomDetail(room) },
            { room -> confirmDeleteRoom(room) },
            userPreferences.isAdmin()
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = roomAdapter
    }

    private fun setupUpdateButton() {
        updateButton = findViewById(R.id.updateButton)
        updateButton.setOnClickListener {
            if (!vacationModeManager.isVacationModeActive()) {
                loadRooms()
            } else {
                showSingleToast("No se pueden actualizar datos en modo vacaciones")
            }
        }
    }

    private fun checkConnectionAndLoadRooms() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isConnected = apiService.checkServerConnection()

                withContext(Dispatchers.Main) {
                    if (isConnected) {
                        showSingleToast("Conectado al servidor correctamente")
                        loadRooms()
                    } else {
                        showSingleToast("Error: No se pudo conectar al servidor")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSingleToast("Error de conexión: ${e.message}")
                }
            }
        }
    }

    private fun loadRooms() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rooms = apiService.getAllRooms()
                withContext(Dispatchers.Main) {
                    if (rooms.isEmpty()) {
                        if (!apiService.checkServerConnection()) {
                            showSingleToast("Error: No se pudo conectar al servidor")
                        } else {
                            showSingleToast("No hay habitaciones registradas")
                        }
                    } else {
                        roomAdapter.updateRooms(rooms)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSingleToast("Error al cargar habitaciones: ${e.message}")
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
            showSingleToast("No se pueden modificar habitaciones en modo vacaciones")
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

    private fun confirmDeleteRoom(room: Room) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar habitación")
            .setMessage("¿Seguro que quieres eliminar la habitación '${room.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteRoom(room)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteRoom(room: Room) {
        val currentUser = userPreferences.getCurrentUser()
        if (currentUser == null) {
            showSingleToast("Error: Usuario no autenticado")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = apiService.deleteRoom(room.name, currentUser.username)
                withContext(Dispatchers.Main) {
                    if (success) {
                        showSingleToast("Habitación eliminada")
                        loadRooms()
                    } else {
                        showSingleToast("Error al eliminar la habitación")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSingleToast("Error de conexión: ${e.message}")
                }
            }
        }
    }
}
