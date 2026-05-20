# 🧪 FASE 4 — Chat Web con Thymeleaf + WebSockets (STOMP)

**Módulo 6.1 · Semana 3 · Proyecto NexusAI 🤖**  
**Duración estimada:** 90 minutos  
**Prerrequisito:** Fase 3 completada  
**Objetivo:** Construir la interfaz web de chat en tiempo real usando Thymeleaf, WebSockets con STOMP, y conectar el frontend al backend de IA.

---

## 📖 Concepto Clave: WebSockets + Thymeleaf para Chat con IA

A diferencia de HTTP (request-response), los WebSockets mantienen una **conexión bidireccional persistente**. Esto permite:
- El usuario envía un mensaje → el servidor lo recibe instantáneamente.
- El servidor (o la IA) responde → el cliente lo recibe sin hacer polling.
- Múltiples usuarios ven los mensajes en tiempo real.

**STOMP** (Simple Text Oriented Messaging Protocol) es un protocolo sobre WebSockets que nos da:
- Canales de suscripción (`/topic/...`)
- Destinos de envío (`/app/...`)
- Semántica pub/sub familiar

---

## 🛠️ Código Base Proporcionado

### 1. Configuración WebSocket — `config/WebSocketConfig.java`

```java
package com.nexusai.config;

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
        // Canales a los que el frontend se suscribe
        config.enableSimpleBroker("/topic");
        // Prefijo para mensajes que el frontend envía al servidor
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Fallback para navegadores sin WebSocket nativo
    }
}
```

### 2. DTOs para WebSocket — `api/ws/dto/`

```java
package com.nexusai.api.ws.dto;

// Mensaje que llega del frontend
public record ChatInputMessage(
    String sender,
    String content,
    String sessionId
) {}
```

```java
package com.nexusai.api.ws.dto;

import java.time.LocalDateTime;

// Mensaje que se envía al frontend
public record ChatOutputMessage(
    String sender,
    String content,
    String type,  // "user", "assistant", "system", "typing"
    LocalDateTime timestamp
) {
    public static ChatOutputMessage user(String sender, String content) {
        return new ChatOutputMessage(sender, content, "user", LocalDateTime.now());
    }

    public static ChatOutputMessage assistant(String content) {
        return new ChatOutputMessage("NexusAI", content, "assistant", LocalDateTime.now());
    }

    public static ChatOutputMessage typing() {
        return new ChatOutputMessage("NexusAI", "", "typing", LocalDateTime.now());
    }

    public static ChatOutputMessage system(String content) {
        return new ChatOutputMessage("System", content, "system", LocalDateTime.now());
    }
}
```

### 3. Vista Thymeleaf — `templates/chat/sala.html`

Crea `src/main/resources/templates/chat/sala.html`:

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NexusAI — Chat de Soporte Inteligente</title>

    <!-- SockJS + STOMP para WebSockets -->
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>

    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">

    <style>
        :root {
            --bg-primary: #0f0f23;
            --bg-secondary: #1a1a3e;
            --bg-chat: #12122b;
            --accent: #6c63ff;
            --accent-hover: #5a52d5;
            --text-primary: #e8e8f0;
            --text-secondary: #9090b0;
            --msg-user: #2d2d6b;
            --msg-ai: #1e3a5f;
            --msg-system: #3d2d1e;
            --border: #2a2a5a;
            --success: #4ade80;
            --danger: #f87171;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            font-family: 'Inter', sans-serif;
            background: var(--bg-primary);
            color: var(--text-primary);
            height: 100vh;
            display: flex;
            flex-direction: column;
        }

        /* ─── Header ─────────────────────────── */
        .header {
            background: var(--bg-secondary);
            border-bottom: 1px solid var(--border);
            padding: 16px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .header h1 {
            font-size: 1.3rem;
            font-weight: 600;
            background: linear-gradient(135deg, var(--accent), #a78bfa);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .status-badge {
            display: flex; align-items: center; gap: 8px;
            font-size: 0.85rem; color: var(--text-secondary);
        }
        .status-dot {
            width: 8px; height: 8px; border-radius: 50%;
            background: var(--success);
            animation: pulse 2s infinite;
        }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.4; }
        }

        /* ─── Chat Container ─────────────────── */
        .chat-container {
            flex: 1;
            overflow-y: auto;
            padding: 20px 24px;
            display: flex;
            flex-direction: column;
            gap: 16px;
            scroll-behavior: smooth;
        }

        /* ─── Messages ───────────────────────── */
        .message {
            max-width: 75%;
            padding: 14px 18px;
            border-radius: 16px;
            line-height: 1.6;
            font-size: 0.95rem;
            animation: fadeIn 0.3s ease-out;
            word-wrap: break-word;
        }
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .message.user {
            background: var(--msg-user);
            align-self: flex-end;
            border-bottom-right-radius: 4px;
        }
        .message.assistant {
            background: var(--msg-ai);
            align-self: flex-start;
            border-bottom-left-radius: 4px;
            border-left: 3px solid var(--accent);
        }
        .message.system {
            background: var(--msg-system);
            align-self: center;
            font-size: 0.85rem;
            color: var(--text-secondary);
            border-radius: 8px;
        }

        .message .sender {
            font-weight: 600;
            font-size: 0.8rem;
            color: var(--accent);
            display: block;
            margin-bottom: 6px;
        }
        .message.user .sender { color: #a78bfa; }

        .message .time {
            font-size: 0.7rem;
            color: var(--text-secondary);
            margin-top: 6px;
            display: block;
            text-align: right;
        }

        /* ─── Typing indicator ───────────────── */
        .typing-indicator {
            display: none;
            align-self: flex-start;
            padding: 14px 18px;
            background: var(--msg-ai);
            border-radius: 16px;
            border-left: 3px solid var(--accent);
        }
        .typing-indicator.active { display: flex; gap: 4px; align-items: center; }
        .typing-dot {
            width: 8px; height: 8px; border-radius: 50%;
            background: var(--accent);
            animation: typing 1.4s infinite;
        }
        .typing-dot:nth-child(2) { animation-delay: 0.2s; }
        .typing-dot:nth-child(3) { animation-delay: 0.4s; }
        @keyframes typing {
            0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
            30% { transform: translateY(-8px); opacity: 1; }
        }

        /* ─── Input Area ─────────────────────── */
        .input-area {
            background: var(--bg-secondary);
            border-top: 1px solid var(--border);
            padding: 16px 24px;
            display: flex;
            gap: 12px;
        }
        .input-area input {
            flex: 1;
            padding: 14px 18px;
            background: var(--bg-chat);
            border: 1px solid var(--border);
            border-radius: 12px;
            color: var(--text-primary);
            font-family: 'Inter', sans-serif;
            font-size: 0.95rem;
            outline: none;
            transition: border-color 0.2s;
        }
        .input-area input:focus { border-color: var(--accent); }
        .input-area button {
            padding: 14px 28px;
            background: var(--accent);
            color: white;
            border: none;
            border-radius: 12px;
            font-family: 'Inter', sans-serif;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.2s, transform 0.1s;
        }
        .input-area button:hover { background: var(--accent-hover); }
        .input-area button:active { transform: scale(0.97); }
        .input-area button:disabled {
            background: var(--border);
            cursor: not-allowed;
        }

        /* ─── Historial renderizado por Thymeleaf ── */
        .history-divider {
            text-align: center;
            color: var(--text-secondary);
            font-size: 0.8rem;
            padding: 8px 0;
        }
    </style>
</head>
<body>

    <!-- ═══ Header ═══ -->
    <header class="header">
        <h1>🤖 NexusAI — Soporte Inteligente</h1>
        <div class="status-badge">
            <span class="status-dot" id="statusDot"></span>
            <span id="statusText">Conectando...</span>
        </div>
    </header>

    <!-- ═══ Chat Messages ═══ -->
    <div class="chat-container" id="chatContainer">

        <!-- Historial previo renderizado por Thymeleaf (desde MongoDB) -->
        <div th:if="${not #lists.isEmpty(history)}" class="history-divider">
            ── Historial anterior ──
        </div>
        <div th:each="msg : ${history}"
             th:classappend="'message ' + ${msg.type}"
             class="message">
            <span class="sender" th:text="${msg.sender}">User</span>
            <span th:text="${msg.content}">Message content</span>
        </div>
        <div th:if="${not #lists.isEmpty(history)}" class="history-divider">
            ── En vivo ──
        </div>

        <!-- Typing indicator -->
        <div class="typing-indicator" id="typingIndicator">
            <span style="font-size:0.85rem; color: var(--accent); margin-right: 8px;">NexusAI</span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
        </div>
    </div>

    <!-- ═══ Input ═══ -->
    <div class="input-area">
        <input type="text" id="messageInput"
               placeholder="Escribe tu pregunta al asistente de IA..."
               autocomplete="off">
        <button id="sendBtn" onclick="sendMessage()" disabled>Enviar</button>
    </div>

    <!-- ═══ JavaScript: WebSocket Logic ═══ -->
    <script th:inline="javascript">
        const sessionId = /*[[${sessionId}]]*/ 'default-session';
        const username  = /*[[${username}]]*/ 'Agent';

        let stompClient = null;
        let connected = false;

        // ─── Connect ─────────────────────────────
        function connect() {
            const socket = new SockJS('/ws-chat');
            stompClient = Stomp.over(socket);
            stompClient.debug = null; // Disable noisy STOMP logs

            stompClient.connect({}, function(frame) {
                connected = true;
                updateStatus(true);
                document.getElementById('sendBtn').disabled = false;

                // Subscribe to the chat topic
                stompClient.subscribe('/topic/chat/' + sessionId, function(response) {
                    const msg = JSON.parse(response.body);
                    handleIncomingMessage(msg);
                });

                // Notify entry
                appendSystemMessage('Conectado al chat. El asistente NexusAI está listo.');

            }, function(error) {
                connected = false;
                updateStatus(false);
                console.error('WebSocket error:', error);
                setTimeout(connect, 5000); // Retry
            });
        }

        // ─── Send Message ────────────────────────
        function sendMessage() {
            const input = document.getElementById('messageInput');
            const content = input.value.trim();
            if (!content || !connected) return;

            const message = {
                sender: username,
                content: content,
                sessionId: sessionId
            };

            stompClient.send('/app/chat.send', {}, JSON.stringify(message));
            input.value = '';
            input.focus();
        }

        // ─── Handle Incoming ─────────────────────
        function handleIncomingMessage(msg) {
            if (msg.type === 'typing') {
                showTyping(true);
                return;
            }
            showTyping(false);
            appendMessage(msg.sender, msg.content, msg.type, msg.timestamp);
        }

        // ─── Append Message to DOM ───────────────
        function appendMessage(sender, content, type, timestamp) {
            const container = document.getElementById('chatContainer');
            const div = document.createElement('div');
            div.className = 'message ' + type;

            const time = timestamp
                ? new Date(timestamp).toLocaleTimeString('es-CO', {hour:'2-digit', minute:'2-digit'})
                : new Date().toLocaleTimeString('es-CO', {hour:'2-digit', minute:'2-digit'});

            div.innerHTML =
                '<span class="sender">' + escapeHtml(sender) + '</span>' +
                '<span>' + escapeHtml(content) + '</span>' +
                '<span class="time">' + time + '</span>';

            const typingEl = document.getElementById('typingIndicator');
            container.insertBefore(div, typingEl);
            container.scrollTop = container.scrollHeight;
        }

        function appendSystemMessage(text) {
            appendMessage('System', text, 'system', null);
        }

        // ─── Typing Indicator ────────────────────
        function showTyping(show) {
            const el = document.getElementById('typingIndicator');
            el.classList.toggle('active', show);
            if (show) {
                document.getElementById('chatContainer').scrollTop =
                    document.getElementById('chatContainer').scrollHeight;
            }
        }

        // ─── Status Badge ────────────────────────
        function updateStatus(isConnected) {
            document.getElementById('statusDot').style.background =
                isConnected ? 'var(--success)' : 'var(--danger)';
            document.getElementById('statusText').textContent =
                isConnected ? 'Conectado — Ollama (Llama 3)' : 'Desconectado';
        }

        // ─── Utilities ──────────────────────────
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        // Enter key to send
        document.getElementById('messageInput').addEventListener('keypress', function(e) {
            if (e.key === 'Enter') sendMessage();
        });

        // ─── Initialize ─────────────────────────
        window.onload = connect;
    </script>

</body>
</html>
```

---

## 📝 Actividades — Tu Turno

### Actividad 4.1 — Controlador UI (Thymeleaf)

```java
// TODO: Crea api/ui/ChatUIController.java
//
// Requisitos:
// 1. Anotado con @Controller (NO @RestController)
// 2. Ruta base: /chat
// 3. Método GET que:
//    a. Genere un sessionId único (UUID.randomUUID().toString())
//    b. Cargue el historial desde MongoDB usando ChatMessageRepository
//    c. Inyecte "history", "sessionId" y "username" en el Model
//    d. Retorne "chat/sala" (la vista Thymeleaf)
// 4. Agrega un @RequestParam opcional para "username" con default "Agent"
```

### Actividad 4.2 — Controlador WebSocket

```java
// TODO: Crea api/ws/ChatSocketController.java
//
// Requisitos:
// 1. Anotado con @Controller
// 2. Inyecta: AiAssistant, ChatMessageRepository, SimpMessagingTemplate
// 3. Método con @MessageMapping("/chat.send") que:
//    a. Reciba un ChatInputMessage
//    b. Guarde el mensaje del usuario en MongoDB
//    c. Envíe el mensaje del usuario a /topic/chat/{sessionId}
//    d. Envíe un indicador de "typing" a /topic/chat/{sessionId}
//    e. En un hilo separado (@Async o CompletableFuture):
//       - Llame a aiAssistant.chat(content)
//       - Guarde la respuesta de la IA en MongoDB
//       - Envíe la respuesta a /topic/chat/{sessionId}
//
// IMPORTANTE: La llamada a la IA DEBE ser asíncrona.
// Si es síncrona, el WebSocket se bloquea 5-10 segundos.
//
// Estructura sugerida:
//
// @MessageMapping("/chat.send")
// public void handleMessage(ChatInputMessage input) {
//     // 1. Save user message to MongoDB
//     ChatMessage userMsg = new ChatMessage(input.sessionId(), input.sender(), 
//                                           input.content(), "USER");
//     chatMessageRepository.save(userMsg);
//
//     // 2. Broadcast user message
//     messagingTemplate.convertAndSend("/topic/chat/" + input.sessionId(),
//         ChatOutputMessage.user(input.sender(), input.content()));
//
//     // 3. Show typing indicator
//     messagingTemplate.convertAndSend("/topic/chat/" + input.sessionId(),
//         ChatOutputMessage.typing());
//
//     // 4. AI response (async)
//     CompletableFuture.runAsync(() -> {
//         try {
//             String aiResponse = aiAssistant.chat(input.content());
//             ChatMessage aiMsg = new ChatMessage(input.sessionId(), 
//                                  "NexusAI", aiResponse, "ASSISTANT");
//             chatMessageRepository.save(aiMsg);
//             messagingTemplate.convertAndSend("/topic/chat/" + input.sessionId(),
//                 ChatOutputMessage.assistant(aiResponse));
//         } catch (Exception e) {
//             messagingTemplate.convertAndSend("/topic/chat/" + input.sessionId(),
//                 ChatOutputMessage.system("Error: " + e.getMessage()));
//         }
//     });
// }
```

### Actividad 4.3 — Fragmentos Reutilizables (Thymeleaf)

```html
<!-- TODO: Crea templates/layout/fragments.html con fragmentos para: -->
<!--
    1. th:fragment="navbar" — Barra de navegación con links a:
       - /chat (Sala de Chat)
       - /dashboard (Dashboard — la crearemos luego)
       - /api/ai/chat (API REST)
    2. th:fragment="footer" — Pie de página con "NexusAI © 2026"
    3. Actualiza sala.html para usar th:replace con estos fragmentos
-->
```

### Actividad 4.4 — Verificación

1. Inicia la aplicación y abre `http://localhost:8080/chat`
2. Envía un mensaje — debe aparecer instantáneamente en la burbuja azul (usuario).
3. Después de 2-5 segundos, NexusAI debe responder con una burbuja verde (asistente).
4. Abre **dos pestañas** en `http://localhost:8080/chat?username=Agent1` y `?username=Agent2` — los mensajes deben ser visibles en ambas pestañas del mismo sessionId.
5. Recarga la página — el historial previo debe cargarse desde MongoDB.

---

## ✅ Criterios de Evaluación — Fase 4

| Criterio | Cumple |
|----------|--------|
| `WebSocketConfig` configura broker y endpoints STOMP correctamente | ☐ |
| `ChatUIController` inyecta historial MongoDB al Model y retorna la vista | ☐ |
| `ChatSocketController` orquesta: guardar → broadcast → IA → broadcast | ☐ |
| La llamada a la IA es **asíncrona** (no bloquea el WebSocket) | ☐ |
| El indicador de "typing" aparece mientras la IA procesa | ☐ |
| La vista diferencia visualmente mensajes de usuario, IA y sistema | ☐ |
| El historial persiste en MongoDB y se muestra al recargar | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿Por qué la llamada a la IA DEBE ser asíncrona en el contexto de WebSockets? ¿Qué pasaría con otros usuarios conectados si fuera síncrona?
2. ¿Cuál es la diferencia entre `@SendTo` y usar `SimpMessagingTemplate.convertAndSend()`? ¿Por qué usamos el segundo en este caso?
3. Observa que usamos `escapeHtml()` en el JavaScript. ¿Qué vulnerabilidad prevenimos con esto? (Pista: XSS)

---

> ⬅️ **Anterior:** [Fase 3 — Prompt Engineering](Lab5-Fase3-Prompt-Engineering.md)  
> ➡️ **Siguiente:** [Fase 5 — Streaming SSE](Lab5-Fase5-Streaming-SSE.md)
