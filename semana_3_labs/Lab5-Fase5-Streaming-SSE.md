# 🧪 FASE 5 — Streaming: Respuestas Token por Token

**Módulo 6.1 · Semana 3 · Proyecto NexusAI 🤖**  
**Duración estimada:** 45 minutos  
**Prerrequisito:** Fase 4 completada  
**Objetivo:** Implementar streaming de respuestas tipo ChatGPT usando Server-Sent Events (SSE) y conectarlo al chat WebSocket para una experiencia de usuario profesional.

---

## 📖 Concepto Clave: ¿Por qué Streaming?

Sin streaming, el usuario envía un mensaje y espera 5-15 segundos viendo "NexusAI está escribiendo..." hasta que llega la respuesta completa. Con streaming, los tokens aparecen progresivamente — exactamente como ChatGPT.

**Spring AI soporta streaming nativamente** con `.stream()` que retorna un `Flux<String>`:

```java
// Sin streaming — espera completa
String response = chatClient.prompt().user(q).call().content(); // ⏳ 8 segundos

// Con streaming — token por token  
Flux<String> tokens = chatClient.prompt().user(q).stream().content(); // 🚀 instantáneo
```

### SSE vs WebSocket para Streaming

| Característica | SSE | WebSocket |
|---------------|-----|-----------|
| Dirección | Server → Client | Bidireccional |
| Complejidad | Baja | Media |
| Reconexión | Automática | Manual |
| Ideal para | Streaming de IA | Chat multiusuario |

**Nuestra estrategia:** Usamos **SSE** para el stream de la IA y **WebSocket** para el chat general. Lo mejor de ambos mundos.

---

## 🛠️ Código Base Proporcionado

### 1. Endpoint SSE — `api/rest/StreamController.java`

```java
package com.nexusai.api.rest;

import com.nexusai.ai.orchestration.AiAssistant;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class StreamController {

    private final AiAssistant aiAssistant;

    public StreamController(AiAssistant aiAssistant) {
        this.aiAssistant = aiAssistant;
    }

    /**
     * SSE endpoint — streams AI response token by token.
     * 
     * The browser connects with EventSource and receives
     * each token as a separate SSE event.
     *
     * Content-Type: text/event-stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestParam String prompt) {
        return aiAssistant.streamResponse(prompt)
                .map(token -> ServerSentEvent.<String>builder()
                        .data(token)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("[DONE]")
                                .build()
                ));
    }
}
```

### 2. JavaScript SSE Client (para agregar a `sala.html`)

Agrega esta función al bloque `<script>` de tu vista de chat:

```javascript
// ─── SSE Streaming ──────────────────────
function sendMessageWithStreaming() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    if (!content) return;

    // 1. Show user message immediately
    appendMessage(username, content, 'user', null);
    input.value = '';

    // 2. Save user message via WebSocket
    if (stompClient && connected) {
        stompClient.send('/app/chat.save', {}, JSON.stringify({
            sender: username, content: content, sessionId: sessionId
        }));
    }

    // 3. Create empty AI message bubble
    const container = document.getElementById('chatContainer');
    const aiDiv = document.createElement('div');
    aiDiv.className = 'message assistant';
    aiDiv.innerHTML = '<span class="sender">NexusAI</span><span id="streamTarget"></span>';
    const typingEl = document.getElementById('typingIndicator');
    container.insertBefore(aiDiv, typingEl);

    // 4. Start SSE stream
    const streamTarget = document.getElementById('streamTarget');
    const eventSource = new EventSource(
        '/api/ai/stream?prompt=' + encodeURIComponent(content)
    );

    eventSource.onmessage = function(event) {
        streamTarget.textContent += event.data;
        container.scrollTop = container.scrollHeight;
    };

    eventSource.addEventListener('done', function(event) {
        eventSource.close();
        streamTarget.removeAttribute('id'); // Allow next stream
        // Save AI response to MongoDB via WebSocket
        if (stompClient && connected) {
            stompClient.send('/app/chat.save-ai', {}, JSON.stringify({
                sender: 'NexusAI', content: streamTarget.textContent, sessionId: sessionId
            }));
        }
    });

    eventSource.onerror = function() {
        eventSource.close();
        streamTarget.textContent += ' [Error en streaming]';
    };
}
```

---

## 📝 Actividades — Tu Turno

### Actividad 5.1 — Implementar `streamResponse()` en `OllamaAssistant`

```java
// TODO: Implementa el método streamResponse en OllamaAssistant.java
//
// Reemplaza la implementación temporal de la Fase 2 con streaming real:
//
// @Override
// public Flux<String> streamResponse(String question) {
//     return chatClient.prompt()
//             .user(question)
//             .stream()
//             .content();
// }
//
// NOTA: Necesitas que tu proyecto incluya spring-boot-starter-webflux
// para que Flux funcione. Ya lo agregamos en el pom.xml de la Fase 1.
```

### Actividad 5.2 — Endpoint para Guardar Mensajes desde Stream

```java
// TODO: En ChatSocketController, agrega dos @MessageMapping adicionales:
//
// @MessageMapping("/chat.save")     → Guarda un mensaje del usuario en MongoDB (sin broadcast)
// @MessageMapping("/chat.save-ai")  → Guarda la respuesta de la IA en MongoDB (sin broadcast)
//
// Estos endpoints son usados por el JavaScript de streaming para persistir
// los mensajes sin duplicar broadcasts en el WebSocket.
```

### Actividad 5.3 — Toggle entre modo normal y streaming en el frontend

```html
<!-- TODO: Agrega un toggle visual en sala.html que permita al usuario -->
<!-- elegir entre "Modo Normal" y "Modo Streaming".                     -->
<!--                                                                     -->
<!-- Requisitos:                                                         -->
<!-- 1. Un botón/switch en el header que alterne entre modos             -->
<!-- 2. En modo normal: usa sendMessage() (WebSocket completo)           -->
<!-- 3. En modo streaming: usa sendMessageWithStreaming() (SSE)          -->
<!-- 4. Cambia el texto del botón "Enviar" según el modo activo          -->
<!-- 5. Guarda la preferencia en localStorage                            -->
```

### Actividad 5.4 — Verificación

```bash
# Test SSE directo desde terminal (verás tokens llegando uno por uno)
curl -N "http://localhost:8080/api/ai/stream?prompt=Explain%20what%20is%20Docker%20in%203%20sentences"
```

1. Abre el chat en `http://localhost:8080/chat`
2. Activa el modo streaming
3. Envía un mensaje — deberías ver las palabras apareciendo progresivamente
4. La experiencia debe ser similar a ChatGPT

---

## ✅ Criterios de Evaluación — Fase 5

| Criterio | Cumple |
|----------|--------|
| `streamResponse()` usa `chatClient.prompt().stream().content()` | ☐ |
| El endpoint SSE retorna `text/event-stream` con tokens individuales | ☐ |
| El JavaScript consume SSE con `EventSource` y actualiza el DOM progresivamente | ☐ |
| Existe un toggle funcional entre modo normal y streaming | ☐ |
| Los mensajes streameados se persisten en MongoDB al completarse | ☐ |
| El evento "done" cierra el stream correctamente | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿Por qué SSE es más apropiado que WebSocket para streaming de IA? (Pista: piensa en la dirección del flujo de datos)
2. ¿Qué sucede si el usuario cierra la pestaña mientras un stream está en progreso? ¿Cómo manejarías la cancelación del lado del servidor?
3. ¿Cuál es la ventaja de enviar un evento `"done"` al final del stream en vez de simplemente cerrar la conexión?

---

> ⬅️ **Anterior:** [Fase 4 — Chat Thymeleaf WebSockets](Lab5-Fase4-Chat-Thymeleaf-WebSockets.md)  
> ➡️ **Siguiente:** [Fase 6 — Tools / Function Calling](Lab5-Fase6-Tools-FunctionCalling.md)
