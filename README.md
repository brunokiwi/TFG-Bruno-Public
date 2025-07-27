# TFG_Bruno
Implementación de un sistema de seguridad e iluminación para hogares utilizando ESP32, MQTT, Spring Boot y Android.

## Descripción
Sistema IoT completo para gestión de iluminación, sensores de movimiento y control de acceso RFID en hogares, compuesto por:
- **Backend Spring Boot**: API REST, lógica de negocio, integración con MQTT y notificaciones push (Firebase)
- **App Android**: Control remoto desde dispositivos móviles, notificaciones en tiempo real
- **Dispositivos ESP32**: Hardware para luces, sensores de movimiento y lector RFID, comunicación por MQTT y configuración sencilla vía WiFiManager

## Estructura del Proyecto
```
backend/
├── src/main/java/           # Backend Spring Boot
│   ├── controller/          # Endpoints REST API
│   ├── model/               # Entidades (Room, RoomSchedule, User, Event)
│   ├── service/             # Lógica de negocio y notificaciones
│   └── mqtt/                # Comunicación MQTT
├── src/main/resources/
│   ├── application.properties   # Configuración del backend
│   └── firebase-service-account.json # Clave de Firebase para notificaciones
├── android-app/             # Aplicación Android (Kotlin)
│   └── app/src/main/        # Código fuente de la app
├── hardware/
│   └── codigoESP32/         # Código Arduino para ESP32
│       └── codigoESP32.ino
└── README.md
```

## Funcionalidades
- Control remoto de luces y sensores por habitación
- Activación/desactivación de sensores de movimiento y alarmas
- Programación de horarios (puntuales e intervalos)
- Registro y uso de tarjetas RFID para control de acceso
- Comunicación MQTT entre backend y dispositivos ESP32
- Notificaciones push en tiempo real (Firebase Cloud Messaging)
- Configuración sencilla del ESP32 mediante portal WiFiManager (WiFi y parámetros MQTT)
- Simulación de eventos y respuestas para pruebas desde el backend

## Tecnologías
- **Backend**: Spring Boot, MySQL, MQTT (Eclipse Paho), Firebase Admin SDK
- **Frontend**: Android (Kotlin), Material Design 3, OkHttp, Gson, Firebase Cloud Messaging
- **Hardware**: ESP32, sensores PIR, relés, lector MFRC522 RFID
- **Comunicación**: MQTT, REST API, WiFiManager

## Instalación y Configuración

### 1. Backend
1. Clona el repositorio y entra en la carpeta `backend`.
2. Configura la base de datos en `src/main/resources/application.properties`.
3. Coloca tu archivo `firebase-service-account.json` en `src/main/resources/`.
4. Compila y ejecuta:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
   El servidor estará disponible en `http://localhost:8080`.

### 2. App Android
1. Abre `android-app/` en Android Studio.
2. Configura la IP del backend en la app (en el login o en `ApiService.kt`).
3. Compila e instala en tu dispositivo Android.
4. La app permite gestionar habitaciones, horarios, sensores, luces y recibir notificaciones push.

### 3. ESP32 (Hardware)
1. Instala Arduino IDE y las siguientes librerías:
   - WiFiManager
   - PubSubClient
   - MFRC522
   - ArduinoJson
   - Preferences
2. Abre `hardware/codigoESP32/codigoESP32.ino` y sube el código al ESP32.
3. Al primer arranque, el ESP32 creará una red WiFi llamada `CasaIoT-Setup`.
4. Conéctate a esa red desde tu móvil/PC y accede al portal cautivo para configurar:
   - SSID y contraseña de tu WiFi
   - Dirección y puerto del broker MQTT
   - Client ID (opcional)
5. El ESP32 guardará estos datos y se conectará automáticamente en futuros reinicios.

### 4. Broker MQTT
- Puedes usar Mosquitto u otro broker MQTT accesible desde el backend y el ESP32.
- Por defecto, el sistema espera el broker en `192.168.1.57:1883`, pero puedes cambiarlo en la configuración.

## Endpoints Principales del Backend
- `GET /rooms` - Listar habitaciones
- `GET /rooms/{roomName}` - Obtener detalles de una habitación
- `POST /rooms` - Crear nueva habitación
- `POST /rooms/{roomName}/remove` - Eliminar habitación
- `POST /rooms/{roomName}/light?state=true` - Encender/apagar luz
- `POST /rooms/{roomName}/alarm?state=true` - Activar/desactivar sensor
- `GET /rooms/{roomName}/schedules` - Ver horarios
- `POST /rooms/{roomName}/schedules` - Crear horario
- `POST /simulation/...` - Simular eventos y respuestas para pruebas

## Notas y Consejos
- El ESP32 puede ser reseteado para reconfigurar WiFi y MQTT usando el método `wm.resetSettings()` en el código.
- El backend y la app están preparados para trabajar en red local o en la nube, según la configuración del broker MQTT y la base de datos.
- Consulta la documentación de cada submódulo para detalles avanzados y troubleshooting.

## Autor
Bruno - TFG