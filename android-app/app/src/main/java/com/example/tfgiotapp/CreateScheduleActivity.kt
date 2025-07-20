package com.example.tfgiotapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateScheduleActivity : ComponentActivity() {
    private lateinit var scheduleNameEdit: EditText
    private lateinit var deviceTypeGroup: RadioGroup
    private lateinit var scheduleTypeGroup: RadioGroup
    private lateinit var actionGroup: RadioGroup
    private lateinit var timeEdit: EditText
    private lateinit var startTimeEdit: EditText
    private lateinit var endTimeEdit: EditText
    private lateinit var punctualLayout: LinearLayout
    private lateinit var intervalLayout: LinearLayout
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    
    private val apiService = ApiService()
    private var roomName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_schedule)

        roomName = intent.getStringExtra("roomName") ?: ""
        
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        scheduleNameEdit = findViewById(R.id.scheduleNameEdit)
        deviceTypeGroup = findViewById(R.id.deviceTypeGroup)
        scheduleTypeGroup = findViewById(R.id.scheduleTypeGroup)
        actionGroup = findViewById(R.id.actionGroup)
        timeEdit = findViewById(R.id.timeEdit)
        startTimeEdit = findViewById(R.id.startTimeEdit)
        endTimeEdit = findViewById(R.id.endTimeEdit)
        punctualLayout = findViewById(R.id.punctualLayout)
        intervalLayout = findViewById(R.id.intervalLayout)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)
    }

    private fun setupListeners() {
        scheduleTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.punctualRadio -> {
                    punctualLayout.visibility = View.VISIBLE
                    intervalLayout.visibility = View.GONE
                }
                R.id.intervalRadio -> {
                    punctualLayout.visibility = View.GONE
                    intervalLayout.visibility = View.VISIBLE
                }
            }
        }
        
        saveButton.setOnClickListener {
            createSchedule()
        }
        
        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun createSchedule() {
        val name = scheduleNameEdit.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa un nombre", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceType = when (deviceTypeGroup.checkedRadioButtonId) {
            R.id.lightRadio -> "light"
            R.id.alarmRadio -> "alarm"
            else -> "light"
        }

        val isPunctual = scheduleTypeGroup.checkedRadioButtonId == R.id.punctualRadio

        if (isPunctual) {
            val time = timeEdit.text.toString().trim()
            if (time.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa la hora", Toast.LENGTH_SHORT).show()
                return
            }

            val state = actionGroup.checkedRadioButtonId == R.id.onRadio

            createPunctualSchedule(name, deviceType, state, time)
        } else {
            val startTime = startTimeEdit.text.toString().trim()
            val endTime = endTimeEdit.text.toString().trim()
            
            if (startTime.isEmpty() || endTime.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa ambas horas", Toast.LENGTH_SHORT).show()
                return
            }

            createIntervalSchedule(name, deviceType, startTime, endTime)
        }
    }

    private fun createPunctualSchedule(name: String, type: String, state: Boolean, time: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = apiService.createPunctualSchedule(roomName, name, type, state, time)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@CreateScheduleActivity, "Horario creado exitosamente", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@CreateScheduleActivity, "Error al crear horario", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateScheduleActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createIntervalSchedule(name: String, type: String, startTime: String, endTime: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = apiService.createIntervalSchedule(roomName, name, type, startTime, endTime)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@CreateScheduleActivity, "Horario creado exitosamente", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@CreateScheduleActivity, "Error al crear horario", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateScheduleActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}