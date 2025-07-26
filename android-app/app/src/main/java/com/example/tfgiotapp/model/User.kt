package com.example.tfgiotapp.model

data class User(
    val id: Long,
    val username: String,
    val role: String,
    val rfidUid: String? 
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: User?
)