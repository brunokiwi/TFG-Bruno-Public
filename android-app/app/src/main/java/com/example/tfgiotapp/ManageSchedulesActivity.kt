package com.example.tfgiotapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgiotapp.model.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageSchedulesActivity : ComponentActivity() {
    private lateinit var titleText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var addScheduleButton: Button
    private val apiService = ApiService()
    private var roomName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_schedules)

        roomName = intent.getStringExtra("roomName") ?: ""
        
        setupViews()
        loadSchedules()
    }

    private fun setupViews() {
        titleText = findViewById(R.id.titleText)
        recyclerView = findViewById(R.id.schedulesRecyclerView)
        addScheduleButton = findViewById(R.id.addScheduleButton)
        
        // Cambio aquí: mostrar "Horarios : nombreHabitacion"
        titleText.text = "Horarios : $roomName"
        
        scheduleAdapter = ScheduleAdapter(emptyList()) { schedule ->
            deleteSchedule(schedule)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = scheduleAdapter
        
        addScheduleButton.setOnClickListener {
            val intent = Intent(this, CreateScheduleActivity::class.java)
            intent.putExtra("roomName", roomName)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSchedules()
    }

    private fun loadSchedules() {
        val serverPreferences = ServerPreferences(this)
        val savedIp = serverPreferences.getServerIp()
        apiService.setServerIp(savedIp)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val schedules = apiService.getRoomSchedules(roomName)
                withContext(Dispatchers.Main) {
                    scheduleAdapter.updateSchedules(schedules)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageSchedulesActivity, "Error al cargar horarios", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteSchedule(schedule: Schedule) {
        val serverPreferences = ServerPreferences(this)
        val savedIp = serverPreferences.getServerIp()
        apiService.setServerIp(savedIp)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = apiService.deleteSchedule(roomName, schedule.id)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@ManageSchedulesActivity, "Horario eliminado", Toast.LENGTH_SHORT).show()
                        loadSchedules()
                    } else {
                        Toast.makeText(this@ManageSchedulesActivity, "Error al eliminar horario", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageSchedulesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}