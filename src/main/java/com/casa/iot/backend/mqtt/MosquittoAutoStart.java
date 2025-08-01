package com.casa.iot.backend.mqtt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MosquittoAutoStart {
    
    @Autowired
    private MqttProperties mqttProperties;
    
    private Process mosquittoProcess;
    private boolean mosquittoStarted = false;
    
    @EventListener(ApplicationReadyEvent.class)
    public void startMosquitto() {
        System.out.println("Configurando MQTT...");
        
        String localIp = detectLocalIP();
        if (localIp != null && !localIp.equals("127.0.0.1")) {
            mqttProperties.setHost(localIp);
            System.out.println("IP local detectada: " + localIp);
        }
        
        if (!isMosquittoRunning()) {
            if (mqttProperties.isAutoStart()) {
                System.out.println("Iniciando Mosquitto automáticamente...");
                try {
                    startMosquittoProcess();
                    Thread.sleep(3000); 
                    
                    if (isMosquittoRunning()) {
                        System.out.println("Mosquitto iniciado correctamente en " + mqttProperties.getBrokerUrl());
                    } else {
                        System.err.println("Error: Mosquitto no pudo iniciarse");
                    }
                } catch (Exception e) {
                    System.err.println("Error iniciando Mosquitto: " + e.getMessage());
                    System.err.println("Solución: Ejecuta manualmente 'net start mosquitto'");
                }
            } else {
                System.out.println("Auto-inicio deshabilitado. Mosquitto debe ejecutarse manualmente");
            }
        } else {
            System.out.println("Mosquitto ya está ejecutándose en puerto 1883");
        }
        
        System.out.println("Configuración MQTT:");
        System.out.println("   - Broker: " + mqttProperties.getBrokerUrl());
        System.out.println("   - Client ID: " + mqttProperties.getClientId());
        System.out.println("   - Auto-start: " + mqttProperties.isAutoStart());
    }
    
    private String detectLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    
                    // buscar ip sitelocal
                    if (addr.isSiteLocalAddress() && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        // en rango de ips privada
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error detectando IP local: " + e.getMessage());
        }
        return null;
    }
    
    private boolean isMosquittoRunning() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Process process;
            
            if (os.contains("windows")) {
                process = Runtime.getRuntime().exec("netstat -an");
            } else {
                process = Runtime.getRuntime().exec("netstat -tuln");
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains(":1883") && line.contains("LISTENING")) {
                    return true;
                }
            }
            
            process.waitFor();
            return false;
            
        } catch (Exception e) {
            System.err.println("Error verificando estado de Mosquitto: " + e.getMessage());
            return false;
        }
    }
    
    private void startMosquittoProcess() throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("windows")) {
            try {
                Process serviceProcess = Runtime.getRuntime().exec("net start mosquitto");
                int exitCode = serviceProcess.waitFor();
                
                if (exitCode == 0) {
                    System.out.println("Mosquitto iniciado como servicio Windows");
                    mosquittoStarted = true;
                    return;
                }
            } catch (Exception e) {
                System.out.println("Servicio Windows no disponible, iniciando ejecutable...");
            }
            
            try {
                String mosquittoPath = "C:\\Program Files\\mosquitto\\mosquitto.exe";
                String configPath = "C:\\Program Files\\mosquitto\\mosquitto.conf";
                
                ProcessBuilder pb = new ProcessBuilder(mosquittoPath, "-c", configPath, "-v");
                mosquittoProcess = pb.start();
                
                // leer salida
                BufferedReader reader = new BufferedReader(new InputStreamReader(mosquittoProcess.getInputStream()));
                String line = reader.readLine();
                if (line != null && line.contains("mosquitto")) {
                    System.out.println("Mosquitto ejecutándose: " + line);
                    mosquittoStarted = true;
                }
                
            } catch (Exception e) {
                throw new IOException("No se pudo iniciar Mosquitto: " + e.getMessage());
            }
            
        } else {
            try {
                ProcessBuilder pb = new ProcessBuilder("mosquitto", "-d");
                mosquittoProcess = pb.start();
                Thread.sleep(1000);
                
                if (mosquittoProcess.isAlive() || isMosquittoRunning()) {
                    System.out.println("Mosquitto iniciado en Linux/Mac");
                    mosquittoStarted = true;
                } else {
                    throw new IOException("Mosquitto no pudo iniciarse");
                }
                
            } catch (Exception e) {
                throw new IOException("Error iniciando Mosquitto en Linux: " + e.getMessage());
            }
        }
    }
    
    @javax.annotation.PreDestroy
    public void stopMosquitto() {
        if (mosquittoProcess != null && mosquittoProcess.isAlive()) {
            System.out.println("Terminando Mosquitto...");
            mosquittoProcess.destroy();
            
            try {
                if (mosquittoProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    System.out.println("Mosquitto terminado correctamente");
                } else {
                    mosquittoProcess.destroyForcibly();
                    System.out.println("Mosquitto terminado correctamente");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}