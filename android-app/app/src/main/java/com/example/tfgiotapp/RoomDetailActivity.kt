package com.example.tfgiotapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.tfgiotapp.model.Room
import com.example.tfgiotapp.model.Schedule
import com.example.tfgiotapp.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomDetailActivity : ComponentActivity() {
    private lateinit var roomNameTextView: TextView
    private lateinit var lightSwitch: Switch
    private lateinit var detectSwitch: Switch
    private lateinit var schedulesTitle: TextView
    private lateinit var schedulesText: TextView
    private lateinit var updateButton: Button
    private lateinit var editSchedulesButton: Button
    private val apiService = ApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)

        roomNameTextView = findViewById(R.id.roomNameDetail)
        lightSwitch = findViewById(R.id.lightSwitch)
        detectSwitch = findViewById(R.id.detectSwitch)
        schedulesTitle = findViewById(R.id.schedulesTitle)
        schedulesText = findViewById(R.id.schedulesText)
        updateButton = findViewById(R.id.updateButton)
        editSchedulesButton = findViewById(R.id.editSchedulesButton)

        val roomName = intent.getStringExtra("roomName") ?: ""
        
        updateButton.setOnClickListener {
            loadRoomDetail(roomName)
        }
        
        editSchedulesButton.setOnClickListener {
            val intent = Intent(this, ManageSchedulesActivity::class.java)
            intent.putExtra("roomName", roomName)
            startActivity(intent)
        }
        
        loadRoomDetail(roomName)
    }

    private fun loadRoomDetail(roomName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val room = apiService.getRoomByName(roomName)
                val schedules = apiService.getRoomSchedules(roomName)

                withContext(Dispatchers.Main) {
                    if (room != null) {
                        displayRoomInfo(room)
                        displaySchedules(roomName, schedules)
                    } else {
                        Toast.makeText(this@RoomDetailActivity, "Habitación no encontrada", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RoomDetailActivity, "Error al cargar detalle", Toast.LENGTH_SHORT).show()
                    schedulesText.text = "Error al cargar horarios"
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

    private fun displaySchedules(roomName: String, schedules: List<Schedule>) {
        schedulesTitle.text = "Horarios:" 
        
        if (schedules.isEmpty()) {
            schedulesText.text = "No hay horarios programados"
        } else {
            val schedulesInfo = schedules.joinToString("\n\n") { schedule ->
                val timeInfo = when {
                    schedule.time != null -> "Hora: ${schedule.time}"
                    schedule.startTime != null && schedule.endTime != null -> 
                        "Desde: ${schedule.startTime} hasta: ${schedule.endTime}"
                    else -> "Sin hora definida"
                }
                
                val stateText = if (schedule.state) "ON" else "OFF"
                val displayName = if (schedule.name != null) schedule.name else "ID: ${schedule.id}"
                "$displayName\nTipo: ${schedule.type}\nEstado: $stateText\n$timeInfo"
            }
            schedulesText.text = schedulesInfo
        }
    }

    private fun updateLight(roomName: String, state: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = apiService.updateLight(roomName, state)
                withContext(Dispatchers.Main) {
                    if (success) {
                        // MENSAJE ACTUALIZADO para reflejar el nuevo flujo
                        Toast.makeText(this@RoomDetailActivity, 
                            "Comando enviado",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        // Revertir el switch si falla el envío
                        lightSwitch.setOnCheckedChangeListener(null)
                        lightSwitch.isChecked = !state
                        lightSwitch.setOnCheckedChangeListener { _, isChecked ->
                            updateLight(roomName, isChecked)
                        }
                        Toast.makeText(this@RoomDetailActivity, 
                            "Error al enviar comando", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lightSwitch.setOnCheckedChangeListener(null)
                    lightSwitch.isChecked = !state
                    lightSwitch.setOnCheckedChangeListener { _, isChecked ->
                        updateLight(roomName, isChecked)
                    }
                    Toast.makeText(this@RoomDetailActivity, 
                        "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateDetect(roomName: String, state: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = apiService.updateAlarm(roomName, state)
                withContext(Dispatchers.Main) {
                    if (success) {
                        // MENSAJE ACTUALIZADO
                        Toast.makeText(this@RoomDetailActivity, 
                            "Comando enviado",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        detectSwitch.setOnCheckedChangeListener(null)
                        detectSwitch.isChecked = !state
                        detectSwitch.setOnCheckedChangeListener { _, isChecked ->
                            updateDetect(roomName, isChecked)
                        }
                        Toast.makeText(this@RoomDetailActivity, 
                            "Error al enviar comando al sensor", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    detectSwitch.setOnCheckedChangeListener(null)
                    detectSwitch.isChecked = !state
                    detectSwitch.setOnCheckedChangeListener { _, isChecked ->
                        updateDetect(roomName, isChecked)
                    }
                    Toast.makeText(this@RoomDetailActivity, 
                        "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}