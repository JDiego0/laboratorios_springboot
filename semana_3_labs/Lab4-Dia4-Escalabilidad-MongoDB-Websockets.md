# 🧪 LABORATORIO DÍA 4 — Arquitectura Híbrida Escalable: MongoDB, WebSockets y Spring AI

**Módulo 6.1 · Semana 3 · Proyecto ChatTech 💬**  
**Duración estimada:** 3 horas  
**Nivel:** Avanzado  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Diseñar una aplicación escalable en **Spring Boot 4** que funcione simultáneamente como API REST y aplicación web MVC (Thymeleaf).
2. Integrar **MongoDB** como base de datos NoSQL utilizando Spring Data MongoDB para persistir el historial.
3. Implementar comunicación bidireccional en tiempo real utilizando **WebSockets** (STOMP).
4. Integrar **Spring AI** para crear un asistente inteligente que participe en el chat y responda utilizando el contexto almacenado en la base de datos.

---

## 📖 Contexto de Negocio — ChatTech con Asistente Virtual

El equipo de LibroTech ha solicitado una herramienta de comunicación en tiempo real para los bibliotecarios llamada **ChatTech**. Para optimizar el tiempo, desean que un **Asistente de Inteligencia Artificial** responda automáticamente a las dudas en la sala de chat. 
El asistente debe tener "memoria" basada en los mensajes guardados en **MongoDB** para entender el contexto de la conversación.

Tu misión es conectar las piezas faltantes: implementar el acceso a MongoDB, construir la vista del chat usando Thymeleaf, y conectar el modelo de lenguaje de Spring AI al ciclo de vida del WebSocket.

---

## 🛠️ Código Base Proporcionado

Asume que el siguiente código ya existe en tu proyecto. Revísalo antes de comenzar las actividades.

**1. Dependencias (`pom.xml`)**
Asegúrate de tener las dependencias de Web, Thymeleaf, MongoDB, WebSocket y **Spring AI** para Spring Boot 4.

```xml
<!-- Spring AI Starter (Ejemplo con OpenAI) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

**2. Modelo: `Mensaje.java`**
```java
package com.chattech.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "mensajes")
public class Mensaje {
    @Id
    private String id;
    private String remitente;
    private String contenido;
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    // Getters, Setters y Constructores
}
```

**3. Configuración WebSocket: `WebSocketConfig.java`**
```java
package com.chattech.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/tema"); // Canal de suscripción
        config.setApplicationDestinationPrefixes("/app"); // Prefijo para enviar mensajes
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat-websocket").withSockJS(); // Endpoint de conexión
    }
}
```

---

## 📝 Actividades: ¡Completar la Aplicación!

El sistema requiere que apliques tus conocimientos para completar las capas faltantes.

### Actividad 1 — Capa de Datos y Lógica (MongoDB)

El sistema necesita almacenar los mensajes para que la IA tenga memoria.

1. **Crear el Repositorio:** Crea `MensajeRepository` que extienda de `MongoRepository<Mensaje, String>`.
2. **Crear el Servicio:** Crea `MensajeService`. Debe tener métodos para guardar un mensaje y obtener el historial reciente.

### Actividad 2 — Integración de Spring AI (BotService)

Crea un servicio `BotIAService` que utilice el historial de MongoDB como contexto para generar respuestas inteligentes.

```java
package com.chattech.service;

import com.chattech.model.Mensaje;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class BotIAService {

    private final ChatClient chatClient;

    @Autowired
    private MensajeService mensajeService;

    public BotIAService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public Mensaje generarRespuestaIA(String preguntaUsuario) {
        // 1. Extraer el contexto de la base de datos (Ej: Últimos 10 mensajes)
        String historialMongo = mensajeService.obtenerHistorial()
            .stream()
            .map(m -> m.getRemitente() + ": " + m.getContenido())
            .collect(Collectors.joining("\n"));

        // 2. Construir el Prompt combinando la base de datos y la pregunta
        String prompt = "Eres el asistente de LibroTech. Usa este historial de chat como contexto:\n" 
                      + historialMongo + "\n\nResponde a: " + preguntaUsuario;

        // 3. Llamar a Spring AI
        String respuestaTexto = chatClient.prompt().user(prompt).call().content();

        // 4. Crear y guardar el mensaje del Bot
        Mensaje mensajeBot = new Mensaje();
        mensajeBot.setRemitente("LibroBot IA");
        mensajeBot.setContenido(respuestaTexto);
        return mensajeService.guardarMensaje(mensajeBot);
    }
}
```

### Actividad 3 — El Controlador de Tiempo Real (WebSocket con IA)

Debemos orquestar el flujo: recibir el mensaje del usuario, guardarlo, retransmitirlo, y luego pedirle a la IA que responda y retransmitir su respuesta usando `SimpMessagingTemplate`.

Crea la clase `ChatSocketController`:

```java
package com.chattech.controller.ws;

import com.chattech.model.Mensaje;
import com.chattech.service.MensajeService;
import com.chattech.service.BotIAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatSocketController {

    @Autowired
    private MensajeService mensajeService;

    @Autowired
    private BotIAService botIAService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/enviar") // Ruta: /app/enviar
    @SendTo("/tema/mensajes") // Difunde el mensaje del usuario a todos
    public Mensaje procesarMensajeUsuario(Mensaje mensajeRecibido) {
        
        // 1. Guardar en MongoDB el mensaje del usuario
        Mensaje mensajeGuardado = mensajeService.guardarMensaje(mensajeRecibido);
        
        // 2. Generar respuesta de la IA en un hilo separado (para no bloquear)
        new Thread(() -> {
            Mensaje respuestaIA = botIAService.generarRespuestaIA(mensajeGuardado.getContenido());
            // 3. Enviar la respuesta de la IA al canal de WebSockets
            messagingTemplate.convertAndSend("/tema/mensajes", respuestaIA);
        }).start();

        return mensajeGuardado; 
    }
}
```

### Actividad 4 — Arquitectura Híbrida: API REST y UI

1. **API REST:** Crea `MensajeRestController` (`@RestController` en `/api/mensajes`) que devuelva el historial de MongoDB.
2. **Controlador UI:** Crea `ChatUIController` (`@Controller` en `/admin/chat`) que inyecte el historial de mensajes al `Model` y retorne la vista `"chat/sala"`.

### Actividad 5 — La Vista Thymeleaf (chat/sala.html)

Utiliza Thymeleaf para renderizar el historial anterior desde MongoDB, y STOMP/JS para el tiempo real.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head>
    <title>ChatTech - Sala Inteligente</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .chat-box { border: 1px solid #ccc; height: 400px; overflow-y: scroll; padding: 15px; background-color: #f9f9f9;}
        .msg { margin-bottom: 10px; padding: 10px; border-radius: 8px; max-width: 80%;}
        .msg-usuario { background-color: #d1ecf1; margin-left: auto; text-align: right;}
        .msg-bot { background-color: #d4edda; margin-right: auto;}
        .remitente { font-weight: bold; font-size: 0.9em; display: block; margin-bottom: 4px;}
        .input-area { margin-top: 15px; display: flex; gap: 10px;}
        input[type="text"] { padding: 10px; flex-grow: 1; border: 1px solid #ccc; border-radius: 4px;}
        button { padding: 10px 20px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;}
    </style>
</head>
<body>

    <h1>Sala de Chat Asistida por IA</h1>
    
    <!-- Historial renderizado por Thymeleaf (MongoDB) -->
    <div id="chat-box" class="chat-box">
        <div th:each="msg : ${historial}" 
             th:classappend="${msg.remitente == 'LibroBot IA'} ? 'msg msg-bot' : 'msg msg-usuario'">
            <span class="remitente" th:text="${msg.remitente}">Usuario</span>
            <span th:text="${msg.contenido}">Texto del mensaje</span>
        </div>
    </div>

    <div class="input-area">
        <input type="text" id="remitente" placeholder="Tu Nombre..." style="flex-grow: 0; width: 150px;" required>
        <input type="text" id="mensaje" placeholder="Escribe un mensaje a la IA o a otros usuarios..." required>
        <button onclick="enviarMensaje()">Enviar</button>
    </div>

    <script>
        var stompClient = null;

        function conectar() {
            var socket = new SockJS('/chat-websocket');
            stompClient = Stomp.over(socket);
            stompClient.connect({}, function (frame) {
                stompClient.subscribe('/tema/mensajes', function (respuesta) {
                    mostrarMensaje(JSON.parse(respuesta.body));
                });
            });
        }

        function enviarMensaje() {
            var remitente = document.getElementById('remitente').value;
            var contenido = document.getElementById('mensaje').value;
            if(contenido.trim() === '') return;
            stompClient.send("/app/enviar", {}, JSON.stringify({'remitente': remitente, 'contenido': contenido}));
            document.getElementById('mensaje').value = '';
        }

        function mostrarMensaje(msg) {
            var chatBox = document.getElementById('chat-box');
            var nuevoMensaje = document.createElement('div');
            var claseTipo = (msg.remitente === 'LibroBot IA') ? 'msg msg-bot' : 'msg msg-usuario';
            nuevoMensaje.className = claseTipo;
            nuevoMensaje.innerHTML = "<span class='remitente'>" + msg.remitente + "</span> <span>" + msg.contenido + "</span>";
            chatBox.appendChild(nuevoMensaje);
            chatBox.scrollTop = chatBox.scrollHeight;
        }

        window.onload = conectar;
    </script>
</body>
</html>
```

---

## 🚀 Pruebas de Escalabilidad, IA y Ejecución

1. Configura **MongoDB** y añade tu clave de API (Ej. `spring.ai.openai.api-key`) en `application.properties`.
2. Inicia la aplicación.
3. Abre **dos pestañas diferentes** en `http://localhost:8080/admin/chat`.
4. Escribe un mensaje en la Pestaña 1. 
   - Verás tu mensaje al instante en ambas pestañas.
   - Unos segundos después, **LibroBot IA** responderá usando el contexto, y su mensaje aparecerá dinámicamente.
5. Detén el servidor y vuelve a iniciarlo. Recarga la página. El historial (incluyendo las respuestas de la IA) se mantendrá intacto gracias a MongoDB y Thymeleaf (`th:each`).

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| Implementó el servicio `BotIAService` utilizando `ChatClient` de Spring AI | ☐ |
| Pasa el historial de MongoDB como contexto dinámico en el Prompt de la IA | ☐ |
| Inyectó `SimpMessagingTemplate` para retransmitir las respuestas de la IA | ☐ |
| Creó un UI Controller que inyecta los datos de Mongo hacia la vista Thymeleaf | ☐ |
| El WebChat diferencia visualmente los mensajes del Bot y del Usuario | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿Por qué ejecutamos la llamada a Spring AI dentro de un `new Thread(() -> {...})` en el Controlador de WebSockets? ¿Qué sucedería si fuera síncrono y la IA tarda 5 segundos en responder?
2. A nivel de seguridad y rendimiento, si la base de datos de MongoDB tuviera 1 millón de mensajes en esta sala, ¿sería buena idea pasar todo el resultado de `obtenerHistorial()` al Prompt de la IA? ¿Cómo lo solucionarías? *(Pista: Búsqueda Vectorial o paginación)*.
3. El patrón que acabas de implementar es una versión básica de **RAG (Retrieval-Augmented Generation)**. ¿Por qué es fundamental conectar las bases de datos transaccionales o documentales (MongoDB) a los modelos de lenguaje genéricos?
