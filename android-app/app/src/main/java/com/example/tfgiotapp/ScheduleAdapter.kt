package com.example.tfgiotapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgiotapp.model.Schedule

class ScheduleAdapter(
    private var schedules: List<Schedule>,
    private val onDeleteClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val scheduleName: TextView = view.findViewById(R.id.scheduleName)
        val scheduleDetails: TextView = view.findViewById(R.id.scheduleDetails)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = schedules[position]
        
        holder.scheduleName.text = schedule.name ?: "ID: ${schedule.id}"
        
        // Información del dispositivo
        val deviceText = when (schedule.type) {
            "light" -> "Luz"
            "alarm" -> "Sensor"
            else -> schedule.type
        }
        
        // Información del tipo de horario
        val scheduleTypeText = when (schedule.scheduleType) {
            "puntual" -> "Puntual"
            "interval" -> "Intervalo"
            else -> "Desconocido"
        }
        
        // Información de tiempo
        val timeInfo = when {
            schedule.time != null -> "Hora: ${schedule.time}"
            schedule.startTime != null && schedule.endTime != null -> 
                "Desde: ${schedule.startTime} hasta: ${schedule.endTime}"
            else -> "Sin hora definida"
        }
        
        val stateText = if (schedule.state) "ON" else "OFF"
        
        holder.scheduleDetails.text = "Dispositivo: $deviceText | Tipo: $scheduleTypeText | Estado: $stateText | $timeInfo"
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(schedule)
        }
    }

    override fun getItemCount() = schedules.size

    fun updateSchedules(newSchedules: List<Schedule>) {
        schedules = newSchedules
        notifyDataSetChanged()
    }
}