package com.example.tfgiotapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {
    
    private lateinit var serverIpEdit: EditText
    private lateinit var testConnectionButton: Button
    private lateinit var connectionStatus: TextView
    private lateinit var usernameEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var loginButton: Button
    private lateinit var userPreferences: UserPreferences
    private lateinit var serverPreferences: ServerPreferences
    private val apiService = ApiService()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        userPreferences = UserPreferences(this)
        serverPreferences = ServerPreferences(this)
        
        // Si ya esta logueado, ir a MainActivity
        if (userPreferences.isLoggedIn()) {
            // Configurar IP guardada antes de ir a MainActivity
            setupSavedServerIp()
            goToMainActivity()
            return
        }
        
        setupViews()
        setupListeners()
        loadSavedServerIp()
    }
    
    private fun setupViews() {
        serverIpEdit = findViewById(R.id.serverIpEdit)
        testConnectionButton = findViewById(R.id.testConnectionButton)
        connectionStatus = findViewById(R.id.connectionStatus)
        usernameEdit = findViewById(R.id.usernameEdit)
        passwordEdit = findViewById(R.id.passwordEdit)
        loginButton = findViewById(R.id.loginButton)
    }
    
    private fun setupListeners() {
        testConnectionButton.setOnClickListener {
            testServerConnection()
        }
        
        loginButton.setOnClickListener {
            performLogin()
        }
    }
    
    private fun loadSavedServerIp() {
        val savedIp = serverPreferences.getServerIp()
        serverIpEdit.setText(savedIp)
        apiService.setServerIp(savedIp)
        
        if (serverPreferences.hasCustomIp()) {
            updateConnectionStatus("IP guardada: $savedIp", android.R.color.holo_blue_dark)
        }
    }
    
    private fun setupSavedServerIp() {
        val savedIp = serverPreferences.getServerIp()
        apiService.setServerIp(savedIp)
    }
    
    private fun testServerConnection() {
        val serverIp = serverIpEdit.text.toString().trim()
        
        if (serverIp.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa la IP del servidor", Toast.LENGTH_SHORT).show()
            return
        }
        
        testConnectionButton.isEnabled = false
        testConnectionButton.text = "Probando..."
        updateConnectionStatus("Probando conexión...", android.R.color.holo_orange_dark)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Configurar la nueva IP
                apiService.setServerIp(serverIp)
                
                // Probar conexión
                val isConnected = apiService.testConnection()
                
                withContext(Dispatchers.Main) {
                    testConnectionButton.isEnabled = true
                    testConnectionButton.text = "Probar"
                    
                    if (isConnected) {
                        // Guardar IP si la conexión es exitosa
                        serverPreferences.saveServerIp(serverIp)
                        updateConnectionStatus("✅ Conectado: ${apiService.getServerUrl()}", android.R.color.holo_green_dark)
                        Toast.makeText(this@LoginActivity, "Conexión exitosa", Toast.LENGTH_SHORT).show()
                    } else {
                        updateConnectionStatus("❌ Error de conexión", android.R.color.holo_red_dark)
                        Toast.makeText(this@LoginActivity, "No se pudo conectar al servidor", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    testConnectionButton.isEnabled = true
                    testConnectionButton.text = "Probar"
                    updateConnectionStatus("❌ Error: ${e.message}", android.R.color.holo_red_dark)
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateConnectionStatus(message: String, colorRes: Int) {
        connectionStatus.text = message
        connectionStatus.setTextColor(ContextCompat.getColor(this, colorRes))
    }
    
    private fun performLogin() {
        val serverIp = serverIpEdit.text.toString().trim()
        val username = usernameEdit.text.toString().trim()
        val password = passwordEdit.text.toString().trim()
        
        if (serverIp.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa la IP del servidor", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Configurar IP antes del login
        apiService.setServerIp(serverIp)
        
        loginButton.isEnabled = false
        loginButton.text = "Iniciando sesion..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.login(username, password)
                
                withContext(Dispatchers.Main) {
                    loginButton.isEnabled = true
                    loginButton.text = "Iniciar Sesión"
                    
                    if (response?.success == true && response.user != null) {
                        // Guardar IP exitosa y usuario
                        serverPreferences.saveServerIp(serverIp)
                        userPreferences.saveUser(response.user)
                        
                        Toast.makeText(this@LoginActivity, "Bienvenido ${response.user.username}", Toast.LENGTH_SHORT).show()
                        goToMainActivity()
                    } else {
                        val errorMessage = when (response?.message) {
                            "SERVER_UNREACHABLE" -> "No se puede conectar al servidor."
                            "CONNECTION_TIMEOUT" -> "Tiempo de conexión agotado."
                            "HOST_NOT_FOUND" -> "No se puede encontrar el servidor."
                            "NETWORK_ERROR" -> "Error de red."
                            else -> response?.message ?: "Error de autenticación"
                        }

                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loginButton.isEnabled = true
                    loginButton.text = "Iniciar Sesión"
                    Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}