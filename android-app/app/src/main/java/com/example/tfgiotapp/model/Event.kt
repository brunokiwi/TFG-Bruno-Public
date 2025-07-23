package com.example.tfgiotapp.model

data class Event(
    val id: Long,
    val eventType: String,
    val action: String,
    val roomName: String?,
    val userId: String?,
    val timestamp: String,
    val details: String?,
    val source: String,
    val ipAddress: String?
)