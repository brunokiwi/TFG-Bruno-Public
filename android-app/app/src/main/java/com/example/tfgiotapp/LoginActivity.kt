package com.example.tfgiotapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {
    
    private lateinit var usernameEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var loginButton: Button
    private lateinit var userPreferences: UserPreferences
    private val apiService = ApiService()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        userPreferences = UserPreferences(this)
        
        // Si ya está logueado, ir a MainActivity
        if (userPreferences.isLoggedIn()) {
            goToMainActivity()
            return
        }
        
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        usernameEdit = findViewById(R.id.usernameEdit)
        passwordEdit = findViewById(R.id.passwordEdit)
        loginButton = findViewById(R.id.loginButton)
    }
    
    private fun setupListeners() {
        loginButton.setOnClickListener {
            performLogin()
        }
    }
    
    private fun performLogin() {
        val username = usernameEdit.text.toString().trim()
        val password = passwordEdit.text.toString().trim()
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        
        loginButton.isEnabled = false
        loginButton.text = "Iniciando sesion..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.login(username, password)
                
                withContext(Dispatchers.Main) {
                    loginButton.isEnabled = true
                    loginButton.text = "Iniciar Sesion"
                    
                    if (response?.success == true && response.user != null) {
                        userPreferences.saveUser(response.user)
                        Toast.makeText(this@LoginActivity, "Bienvenido ${response.user.username}", Toast.LENGTH_SHORT).show()
                        goToMainActivity()
                    } else {
                        // Manejar diferentes tipos de error
                        val errorMessage = when (response?.message) {
                            "SERVER_UNREACHABLE" -> "No se puede conectar al servidor. "
                            "CONNECTION_TIMEOUT" -> "Tiempo de conexion agotado."
                            "HOST_NOT_FOUND" -> "No se puede encontrar el servidor. "
                            "NETWORK_ERROR" -> "Error de red. "
                            else -> response?.message ?: "Error de autenticacion"
                        }

                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loginButton.isEnabled = true
                    loginButton.text = "Iniciar Sesion"
                    Toast.makeText(this@LoginActivity, "Error de conexion: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}