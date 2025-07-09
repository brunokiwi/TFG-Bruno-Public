package com.example.tfgiotapp

import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.tfgiotapp.model.Room
import com.example.tfgiotapp.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomDetailActivity : ComponentActivity() {
    private lateinit var roomNameTextView: TextView
    private lateinit var lightSwitch: Switch
    private lateinit var detectSwitch: Switch
    private val apiService = ApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)

        roomNameTextView = findViewById(R.id.roomNameDetail)
        lightSwitch = findViewById(R.id.lightSwitch)
        detectSwitch = findViewById(R.id.detectSwitch)

        val roomName = intent.getStringExtra("roomName") ?: ""
        loadRoomDetail(roomName)
    }

    private fun loadRoomDetail(roomName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val room = apiService.getRoomByName(roomName)
                withContext(Dispatchers.Main) {
                    if (room != null) {
                        displayRoomInfo(room)
                    } else {
                        Toast.makeText(this@RoomDetailActivity, "Habitación no encontrada", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RoomDetailActivity, "Error al cargar detalle", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayRoomInfo(room: Room) {
        roomNameTextView.text = room.name
        
        // Temporalmente quitar listeners para evitar llamadas no deseadas
        lightSwitch.setOnCheckedChangeListener(null)
        detectSwitch.setOnCheckedChangeListener(null)
        
        // Establecer el estado de los switches
        lightSwitch.isChecked = room.lightOn
        detectSwitch.isChecked = room.detectOn
        
        // Restaurar listeners
        val roomName = room.name
        lightSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateLight(roomName, isChecked)
        }
        
        detectSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateDetect(roomName, isChecked)
        }
    }
    
    private fun updateLight(roomName: String, state: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = apiService.updateLight(roomName, state)
                withContext(Dispatchers.Main) {
                    if (!success) {
                        // Revertir el switch sin activar el listener
                        lightSwitch.setOnCheckedChangeListener(null)
                        lightSwitch.isChecked = !state
                        lightSwitch.setOnCheckedChangeListener { _, isChecked ->
                            updateLight(roomName, isChecked)
                        }
                        Toast.makeText(this@RoomDetailActivity, "Error al actualizar la luz", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Revertir el switch sin activar el listener
                    lightSwitch.setOnCheckedChangeListener(null)
                    lightSwitch.isChecked = !state
                    lightSwitch.setOnCheckedChangeListener { _, isChecked ->
                        updateLight(roomName, isChecked)
                    }
                    Toast.makeText(this@RoomDetailActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateDetect(roomName: String, state: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = apiService.updateAlarm(roomName, state)
                withContext(Dispatchers.Main) {
                    if (!success) {
                        // Revertir el switch sin activar el listener
                        detectSwitch.setOnCheckedChangeListener(null)
                        detectSwitch.isChecked = !state
                        detectSwitch.setOnCheckedChangeListener { _, isChecked ->
                            updateDetect(roomName, isChecked)
                        }
                        Toast.makeText(this@RoomDetailActivity, "Error al actualizar el detector", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Revertir el switch sin activar el listener
                    detectSwitch.setOnCheckedChangeListener(null)
                    detectSwitch.isChecked = !state
                    detectSwitch.setOnCheckedChangeListener { _, isChecked ->
                        updateDetect(roomName, isChecked)
                    }
                    Toast.makeText(this@RoomDetailActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}