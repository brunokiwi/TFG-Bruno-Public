# TFG_Bruno
Implementación de un sistema de seguridad e iluminación para hogares utilizando ESP32

## Descripción
Sistema IoT completo para gestión de iluminación y detección de movimiento en hogares, compuesto por:
- **Backend Spring Boot**: API REST y gestor de horarios
- **App Android**: Control remoto desde dispositivos móviles
- **Dispositivos ESP32**: Hardware para luces y sensores

## Estructura del Proyecto
```
backend/
├── src/main/java/           # Backend Spring Boot
│   ├── controller/          # Endpoints REST API
│   ├── model/              # Entidades (Room, RoomSchedule)
│   ├── service/            # Lógica de negocio
│   └── mqtt/               # Comunicación MQTT
├── android-app/            # Aplicación Android
│   └── app/src/main/       # Código Kotlin
└── README.md
```

## Funcionalidades
- Control remoto de luces por habitación
- Activación/desactivación de sensores de movimiento
- Programación de horarios (puntuales e intervalos)
- Comunicación MQTT con dispositivos ESP32
- Interfaz Android intuitiva

## Tecnologías
- **Backend**: Spring Boot, H2 Database, MQTT
- **Frontend**: Android (Kotlin), Material Design
- **Hardware**: ESP32, sensores de movimiento
- **Comunicación**: MQTT, REST API

## Configuración Rápida

### Backend
```bash
cd backend
./mvnw spring-boot:run
```
El servidor estará disponible en `http://localhost:8080`

### App Android
1. Abrir `android-app/` en Android Studio
2. Cambiar la IP en `ApiService.kt` por la IP de tu servidor
3. Compilar e instalar en dispositivo

### MQTT Broker
Asegúrate de tener un broker MQTT ejecutándose en `localhost:1883`

## Endpoints Principales
- `GET /rooms` - Listar habitaciones
- `POST /rooms/{name}/light?state=true` - Controlar luz
- `POST /rooms/{name}/alarm?state=true` - Controlar sensor
- `GET /rooms/{name}/schedules` - Ver horarios
- `POST /rooms/{name}/schedules` - Crear horario

## Ejemplo de Uso
```bash
# Encender luz del salón
curl -X POST "http://localhost:8080/rooms/salon/light?state=true"

# Crear horario para encender luz a las 20:00
curl -X POST "http://localhost:8080/rooms/salon/schedules?type=light&state=true&time=20:00&name=Luz nocturna"
```

## Autor
Bruno - TFG 2025
