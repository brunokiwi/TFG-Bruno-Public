package com.example.tfgiotapp.model

data class Schedule(
    val id: Long,
    val type: String,
    val state: Boolean,
    val time: String?,
    val startTime: String?,
    val endTime: String?
)