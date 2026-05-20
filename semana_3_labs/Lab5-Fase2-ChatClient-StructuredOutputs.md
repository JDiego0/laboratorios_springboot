# 🧪 FASE 2 — ChatClient y Structured Outputs

**Módulo 6.1 · Semana 3 · Proyecto NexusAI 🤖**  
**Duración estimada:** 45 minutos  
**Prerrequisito:** Fase 1 completada  
**Objetivo:** Dominar el `ChatClient` de Spring AI, implementar la abstracción `AiAssistant`, y usar structured outputs con Java records para obtener respuestas tipadas del LLM.

---

## 📖 Concepto Clave: Structured Outputs

La mayoría de tutoriales de IA hacen esto:

```java
String response = chatClient.prompt().user("Analiza este ticket").call().content();
// response = "El ticket parece ser de prioridad alta porque..."
// ¿Y ahora qué? ¿Parsear con regex? ¿Buscar palabras clave? 💀
```

**Esto es un anti-patrón.** En sistemas reales necesitas **datos estructurados**, no texto libre.

### La solución: `.entity()`

Spring AI puede instruir al LLM para que responda con un JSON que mapea directamente a un Java `record`:

```java
// El LLM responde con un JSON que Spring AI convierte automáticamente
TicketAnalysis result = chatClient.prompt()
    .user("Analyze: Server crashes every Monday at 3am")
    .call()
    .entity(TicketAnalysis.class);

// result.priority()     → "CRITICAL"
// result.category()     → "Infrastructure"
// result.estimatedTime() → "2 hours"
```

**¿Cómo funciona internamente?**
1. Spring AI genera un **JSON Schema** a partir de tu `record`.
2. Envía el schema como parte del prompt al LLM.
3. El LLM responde en formato JSON.
4. Spring AI **deserializa** el JSON a tu objeto Java.

Esto elimina: parsing manual, regex, respuestas inconsistentes, JSON roto.

---

## 🛠️ Código Base Proporcionado

### 1. Configuración del ChatClient — `ai/config/AiConfig.java`

```java
package com.nexusai.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * ChatClient es la API principal de Spring AI.
     * Es análogo a RestClient o WebClient — una interfaz fluida 
     * para interactuar con modelos de lenguaje.
     *
     * IMPORTANTE: Usamos ChatModel (interface), NO OllamaChatModel.
     * Spring Boot auto-configura el ChatModel según las dependencias.
     */
    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    You are NexusAI, an intelligent technical support assistant 
                    for NexusTech Solutions. You help support agents analyze tickets,
                    answer technical questions, and provide solutions.
                    Always respond in a structured, professional manner.
                    If you don't know something, say so clearly.
                    """)
                .build();
    }
}
```

> 💡 **Observa:** Usamos `ChatModel` (la interface de Spring AI), no `OllamaChatModel` ni `OpenAiChatModel`. Esto es desacoplamiento. Spring Boot inyecta la implementación correcta según tu `application.yml`.

---

### 2. DTOs para Structured Output — `ai/models/`

Estos records definen la **estructura** que esperamos del LLM:

**`ai/models/TicketAnalysis.java`**
```java
package com.nexusai.ai.models;

import java.util.List;

/**
 * Structured output for ticket analysis.
 * Spring AI generates a JSON Schema from this record
 * and instructs the LLM to respond in this format.
 */
public record TicketAnalysis(
    String priority,          // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    String category,          // "Infrastructure", "Software", "Network", etc.
    String summary,           // One-line summary of the issue
    List<String> suggestedActions, // Recommended steps to resolve
    String estimatedTime,     // "30 minutes", "2 hours", etc.
    double confidenceScore    // 0.0 to 1.0 — how confident the AI is
) {}
```

**`ai/models/SupportResponse.java`**
```java
package com.nexusai.ai.models;

import java.util.List;

/**
 * Structured response for support chat interactions.
 */
public record SupportResponse(
    String answer,            // The main response text
    String tone,              // "technical", "friendly", "urgent"
    List<String> relatedTopics, // Topics the user might also need
    boolean needsEscalation   // Whether to escalate to a human
) {}
```

---

### 3. Interface de Abstracción — `ai/orchestration/AiAssistant.java`

```java
package com.nexusai.ai.orchestration;

import com.nexusai.ai.models.TicketAnalysis;
import com.nexusai.ai.models.SupportResponse;
import reactor.core.publisher.Flux;

/**
 * Core abstraction for AI interactions.
 * 
 * RULE: All AI calls in the application MUST go through this interface.
 * NEVER inject ChatClient or ChatModel directly into controllers or services.
 * 
 * This allows us to:
 * - Swap providers (Ollama → OpenAI → Claude)
 * - Add fallback logic
 * - Mock for testing
 * - Balance costs between models
 */
public interface AiAssistant {

    /**
     * Analyze a support ticket and return structured analysis.
     */
    TicketAnalysis analyzeTicket(String ticketTitle, String ticketDescription);

    /**
     * Generate a support response for a user question.
     */
    SupportResponse answerQuestion(String question);

    /**
     * Stream a response token-by-token (for chat UI).
     */
    Flux<String> streamResponse(String question);

    /**
     * Simple chat — returns plain text response.
     */
    String chat(String userMessage);
}
```

---

## 📝 Actividades — Tu Turno

### Actividad 2.1 — Implementar `OllamaAssistant`

Crea la implementación de `AiAssistant` que usa Ollama como proveedor:

**`ai/orchestration/OllamaAssistant.java`**

```java
package com.nexusai.ai.orchestration;

import com.nexusai.ai.models.TicketAnalysis;
import com.nexusai.ai.models.SupportResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class OllamaAssistant implements AiAssistant {

    private final ChatClient chatClient;

    public OllamaAssistant(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public TicketAnalysis analyzeTicket(String ticketTitle, String ticketDescription) {
        // TODO: Implementa este método.
        //
        // Instrucciones:
        // 1. Usa chatClient.prompt() para iniciar la llamada.
        // 2. En .user(), construye un prompt que incluya el título y la descripción.
        //    Ejemplo: "Analyze this support ticket:\nTitle: " + ticketTitle + "\nDescription: " + ticketDescription
        // 3. Usa .call() para ejecutar la llamada.
        // 4. Usa .entity(TicketAnalysis.class) para obtener el resultado como structured output.
        // 5. Retorna el TicketAnalysis resultante.
        //
        // Pista — La cadena completa se ve así:
        // return chatClient.prompt().user(...).call().entity(TicketAnalysis.class);

        throw new UnsupportedOperationException("TODO: Implement analyzeTicket");
    }

    @Override
    public SupportResponse answerQuestion(String question) {
        // TODO: Implementa este método.
        //
        // Instrucciones:
        // 1. Construye un prompt claro pidiendo al LLM responder la pregunta de soporte.
        // 2. Usa .entity(SupportResponse.class) para structured output.
        // 3. Retorna el SupportResponse.
        //
        // Tip: El system prompt ya está configurado en AiConfig,
        //      así que solo necesitas el user prompt aquí.

        throw new UnsupportedOperationException("TODO: Implement answerQuestion");
    }

    @Override
    public Flux<String> streamResponse(String question) {
        // Implementaremos esto en la Fase 5 (Streaming).
        // Por ahora, deja esta implementación temporal:
        return Flux.just(chat(question));
    }

    @Override
    public String chat(String userMessage) {
        // TODO: Implementa este método.
        //
        // Este es el más simple — retorna texto plano.
        // Usa: chatClient.prompt().user(userMessage).call().content();

        throw new UnsupportedOperationException("TODO: Implement chat");
    }
}
```

---

### Actividad 2.2 — Crear tu Propio DTO de Structured Output

Demuestra que entiendes el concepto creando tu propio `record` para un caso de uso diferente.

```java
// TODO: Crea el archivo ai/models/EscalationDecision.java
//
// Este record debe representar la decisión de si un ticket necesita 
// ser escalado a un nivel superior de soporte.
//
// Campos sugeridos (puedes agregar más):
// - boolean shouldEscalate      → ¿Debe escalarse?
// - String reason               → Razón de la decisión
// - String suggestedTeam        → Equipo sugerido ("L2 Support", "Engineering", etc.)
// - int urgencyLevel            → Nivel de urgencia (1-5)
// - List<String> missingInfo    → Información faltante para resolver el ticket
```

---

### Actividad 2.3 — REST Controller de Prueba

Crea un controller REST para probar tus implementaciones:

**`api/rest/AiTestController.java`**

```java
package com.nexusai.api.rest;

import com.nexusai.ai.models.TicketAnalysis;
import com.nexusai.ai.models.SupportResponse;
import com.nexusai.ai.orchestration.AiAssistant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiTestController {

    private final AiAssistant aiAssistant;

    public AiTestController(AiAssistant aiAssistant) {
        this.aiAssistant = aiAssistant;
    }

    // ─── Test: Simple Chat ─────────────────────────
    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam String message) {
        String response = aiAssistant.chat(message);
        return ResponseEntity.ok(response);
    }

    // ─── Test: Ticket Analysis (Structured) ────────
    @PostMapping("/analyze-ticket")
    public ResponseEntity<TicketAnalysis> analyzeTicket(
            @RequestParam String title,
            @RequestParam String description) {
        TicketAnalysis analysis = aiAssistant.analyzeTicket(title, description);
        return ResponseEntity.ok(analysis);
    }

    // TODO: Agrega un endpoint para probar tu SupportResponse.
    //
    // Requisitos:
    // 1. Ruta: GET /api/ai/support
    // 2. Parámetro: @RequestParam String question
    // 3. Usa aiAssistant.answerQuestion(question)
    // 4. Retorna ResponseEntity<SupportResponse>

    // TODO: Agrega un endpoint para probar tu EscalationDecision.
    //
    // Requisitos:
    // 1. Ruta: POST /api/ai/escalation-check
    // 2. Parámetros: title y description
    // 3. Crea un nuevo método en AiAssistant para esto,
    //    o usa el chatClient directamente aquí como prueba rápida.
    // 4. Retorna ResponseEntity<EscalationDecision>
}
```

---

### Actividad 2.4 — Pruebas con cURL o Postman

Una vez implementados los TODOs, prueba tus endpoints:

```bash
# Test 1: Chat simple
curl "http://localhost:8080/api/ai/chat?message=What%20is%20a%20null%20pointer%20exception?"

# Test 2: Ticket Analysis (structured output)
curl -X POST "http://localhost:8080/api/ai/analyze-ticket" \
  -d "title=Server%20crashes%20every%20Monday" \
  -d "description=The%20production%20server%20crashes%20at%203am%20every%20Monday.%20Logs%20show%20OOM%20errors."

# Test 3: Tu endpoint de support
curl "http://localhost:8080/api/ai/support?question=How%20do%20I%20reset%20my%20password?"
```

**Verifica que:**
- El chat simple retorna texto plano.
- El análisis de ticket retorna un **JSON estructurado** con los campos del record.
- Las respuestas son consistentes y tipadas.

---

## 🧠 Concepto Avanzado: ¿Por qué `record` y no `class`?

Los Java `records` son ideales para structured outputs porque:

| Característica | `record` | `class` |
|---------------|----------|---------|
| Inmutabilidad | ✅ Sí, por defecto | ❌ No, requiere `final` |
| `equals()`/`hashCode()` | ✅ Auto-generados | ❌ Manual |
| Deserialización JSON | ✅ Constructor canónico | ⚠️ Requiere `@JsonCreator` |
| Boilerplate | ✅ Mínimo | ❌ Getters, setters, etc. |
| Semántica | ✅ "Carrier of data" | ⚠️ Ambiguo |

Los records comunican **intención**: "este tipo existe SOLO para transportar datos" — perfecto para respuestas de IA.

---

## ✅ Criterios de Evaluación — Fase 2

| Criterio | Cumple |
|----------|--------|
| `AiConfig` crea el `ChatClient` bean usando `ChatModel` (no implementación concreta) | ☐ |
| `OllamaAssistant` implementa `AiAssistant` y usa `chatClient.prompt()` | ☐ |
| `analyzeTicket()` retorna un `TicketAnalysis` usando `.entity()` | ☐ |
| `answerQuestion()` retorna un `SupportResponse` usando `.entity()` | ☐ |
| Se creó al menos un `record` DTO adicional (`EscalationDecision` u otro) | ☐ |
| Los endpoints REST del `AiTestController` responden correctamente | ☐ |
| Las respuestas JSON contienen los campos definidos en los records | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿Qué ventaja tiene usar `.entity(TicketAnalysis.class)` sobre `.content()` + parsing manual con Jackson?
2. Si el LLM retorna un JSON con un campo extra que no está en tu `record`, ¿Spring AI lanza una excepción o lo ignora? Experimenta para descubrirlo.
3. ¿Por qué el `ChatClient` bean se configura con un `defaultSystem` prompt? ¿Qué pasaría si cada llamada al LLM tuviera que especificar el system prompt manualmente?
4. ¿Cómo cambiarías la implementación para usar OpenAI en lugar de Ollama **sin modificar** ningún controlador ni servicio de negocio?

---

> ⬅️ **Anterior:** [Fase 1 — Proyecto y Arquitectura](Lab5-Fase1-Proyecto-Arquitectura.md)  
> ➡️ **Siguiente:** [Fase 3 — Prompt Engineering](Lab5-Fase3-Prompt-Engineering.md)
