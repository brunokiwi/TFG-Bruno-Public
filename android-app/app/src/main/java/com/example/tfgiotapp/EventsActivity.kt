package com.example.tfgiotapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgiotapp.model.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventsActivity : ComponentActivity() {
    private lateinit var titleText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private lateinit var refreshButton: Button
    private val apiService = ApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        setupViews()
        loadEvents()
    }

    private fun setupViews() {
        titleText = findViewById(R.id.titleText)
        recyclerView = findViewById(R.id.eventsRecyclerView)
        refreshButton = findViewById(R.id.refreshButton)

        titleText.text = "Registro de Eventos"

        eventAdapter = EventAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = eventAdapter

        refreshButton.setOnClickListener {
            loadEvents()
        }
    }

    private fun loadEvents() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = apiService.getAllEvents()
                withContext(Dispatchers.Main) {
                    eventAdapter.updateEvents(events)
                    if (events.isEmpty()) {
                        Toast.makeText(this@EventsActivity, "No hay eventos registrados", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@EventsActivity, "Cargados ${events.size} eventos", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EventsActivity, "Error al cargar eventos: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}