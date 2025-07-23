package com.example.tfgiotapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgiotapp.model.Event
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EventAdapter(
    private var events: List<Event>
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventTitle: TextView = view.findViewById(R.id.eventTitle)
        val eventDetails: TextView = view.findViewById(R.id.eventDetails)
        val eventTime: TextView = view.findViewById(R.id.eventTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        
        // Título del evento
        val title = when (event.eventType) {
            "USER_ACTION" -> "Acción Usuario: ${event.action}"
            "SYSTEM_ACTION" -> "Sistema: ${event.action}"
            "MOVEMENT_DETECTED" -> "Movimiento Detectado"
            "SCHEDULE_EXECUTED" -> "Horario Ejecutado: ${event.action}"
            "LOGIN_ATTEMPT" -> if (event.action == "LOGIN_SUCCESS") "Login Exitoso" else "Login Fallido"
            else -> event.action
        }
        holder.eventTitle.text = title

        // Detalles
        val details = buildString {
            if (!event.roomName.isNullOrEmpty()) {
                append("Habitación: ${event.roomName}\n")
            }
            if (!event.userId.isNullOrEmpty()) {
                append("Usuario: ${event.userId}\n")
            }
            append("Fuente: ${event.source}")
            if (!event.ipAddress.isNullOrEmpty()) {
                append("\nIP: ${event.ipAddress}")
            }
        }
        holder.eventDetails.text = details

        // Formatear tiempo
        try {
            val dateTime = LocalDateTime.parse(event.timestamp)
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            holder.eventTime.text = dateTime.format(formatter)
        } catch (e: Exception) {
            holder.eventTime.text = event.timestamp
        }
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }
}