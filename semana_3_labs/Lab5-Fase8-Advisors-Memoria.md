# 🧪 FASE 8 — Advisors: Memoria Conversacional y Cadenas de Interceptores

**Módulo 6.1 · Semana 3 · Proyecto NexusAI 🤖**  
**Duración estimada:** 45 minutos  
**Prerrequisito:** Fase 7 completada  
**Objetivo:** Implementar memoria conversacional persistente usando Advisors de Spring AI, crear advisors personalizados y entender la cadena de interceptores.

---

## 📖 Concepto Clave: Advisors

Los Advisors son **interceptores** que modifican el request o response de una llamada al LLM. Funcionan como AOP (Aspect-Oriented Programming) para IA:

```
Request del usuario
    ↓
[Advisor 1: Memory]    → Inyecta historial de conversación en el prompt
    ↓
[Advisor 2: RAG]       → Busca documentos relevantes y los añade al contexto
    ↓
[Advisor 3: Security]  → Filtra PII y contenido sensible
    ↓
LLM procesa y responde
    ↓
[Advisor 3: Security]  → Valida la respuesta
    ↓
[Advisor 2: RAG]       → Post-procesamiento
    ↓
[Advisor 1: Memory]    → Guarda la interacción en la memoria
    ↓
Response al usuario
```

### Advisors Built-in de Spring AI

| Advisor | Propósito |
|---------|-----------|
| `MessageChatMemoryAdvisor` | Memoria conversacional — recuerda mensajes previos |
| `QuestionAnswerAdvisor` | RAG — busca contexto en vector store |

---

## 🛠️ Código Base Proporcionado

### 1. Configuración de Memoria — `ai/memory/MemoryConfig.java`

```java
package com.nexusai.ai.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryConfig {

    /**
     * ChatMemory stores conversation history.
     * 
     * InMemoryChatMemory is fine for development.
     * For production, implement a persistent ChatMemory
     * backed by Redis, MongoDB, or PostgreSQL.
     */
    @Bean
    ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }
}
```

### 2. Advisor Personalizado de Logging — `ai/advisors/LoggingAdvisor.java`

```java
package com.nexusai.ai.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.*;

import reactor.core.publisher.Flux;

/**
 * Custom advisor that logs all AI interactions for observability.
 * 
 * Advisors implement the chain pattern — each advisor processes
 * the request before passing it to the next, and processes the
 * response on the way back.
 */
public class LoggingAdvisor implements CallAroundAdvisor {

    private static final Logger log = LoggerFactory.getLogger(LoggingAdvisor.class);

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        // ── Before LLM call ──
        long startTime = System.currentTimeMillis();
        log.info("🤖 AI Request — User prompt length: {} chars",
                request.userText().length());

        // ── Execute the chain (call LLM) ──
        AdvisedResponse response = chain.nextAroundCall(request);

        // ── After LLM call ──
        long duration = System.currentTimeMillis() - startTime;
        String content = response.response().getResult().getOutput().getText();
        log.info("✅ AI Response — {} chars in {}ms", 
                content != null ? content.length() : 0, duration);

        return response;
    }

    @Override
    public String getName() {
        return "LoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return 0; // First in the chain
    }
}
```

---

## 📝 Actividades — Tu Turno

### Actividad 8.1 — Integrar Memoria en el Chat

```java
// TODO: Modifica OllamaAssistant para incluir MessageChatMemoryAdvisor
//
// import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
// import org.springframework.ai.chat.memory.ChatMemory;
//
// 1. Inyecta ChatMemory en el constructor
// 2. En el método chat(), agrega el advisor:
//
//    return chatClient.prompt()
//            .user(userMessage)
//            .advisors(
//                new MessageChatMemoryAdvisor(chatMemory),     // ← Memoria
//                new QuestionAnswerAdvisor(vectorStore),        // ← RAG (Fase 7)
//                new LoggingAdvisor()                           // ← Logging
//            )
//            .tools(ticketTool, systemStatusTool)
//            .call()
//            .content();
//
// 3. Para memoria por sesión, pasa el conversationId:
//
//    .advisors(advisorSpec -> advisorSpec
//        .param(ChatMemory.CONVERSATION_ID, sessionId))
//
// PRUEBA: En el chat, di "My name is Carlos" y luego pregunta 
// "What is my name?" — la IA debe recordar.
```

### Actividad 8.2 — Crear Advisor de Moderación/Seguridad

```java
// TODO: Crea ai/advisors/ContentModerationAdvisor.java
//
// Este advisor filtra contenido peligroso ANTES de enviarlo al LLM
// y valida la respuesta DESPUÉS.
//
// Requisitos:
// 1. Implementa CallAroundAdvisor
// 2. En aroundCall():
//    a. ANTES del LLM:
//       - Verifica que el prompt no contiene palabras bloqueadas
//         (ej: "DROP TABLE", "rm -rf", "password is", etc.)
//       - Si detecta contenido peligroso, retorna una respuesta fija
//         sin llamar al LLM: "I cannot process this request for security reasons."
//    b. DESPUÉS del LLM:
//       - Verifica que la respuesta no contiene PII simulada
//         (ej: patrones de email, SSN, números de tarjeta de crédito)
//       - Si detecta PII, enmascárala con asteriscos
// 3. getOrder() retorna -1 (antes del LoggingAdvisor)
//
// Ejemplo de filtro:
// private static final List<String> BLOCKED_PATTERNS = 
//     List.of("DROP TABLE", "DELETE FROM", "rm -rf", "password is", "credit card");
//
// private boolean containsBlockedContent(String text) {
//     return BLOCKED_PATTERNS.stream()
//         .anyMatch(pattern -> text.toUpperCase().contains(pattern.toUpperCase()));
// }
```

### Actividad 8.3 — Memoria Persistente con MongoDB

```java
// TODO: Implementa una versión persistente de ChatMemory usando MongoDB
//
// Crea ai/memory/MongoChatMemory.java que implementa ChatMemory:
//
// public class MongoChatMemory implements ChatMemory {
//     
//     private final ChatMessageRepository repository;
//
//     @Override
//     public void add(String conversationId, List<Message> messages) {
//         // Convierte Message de Spring AI a ChatMessage (tu entidad MongoDB)
//         // y guarda en MongoDB
//     }
//
//     @Override  
//     public List<Message> get(String conversationId, int lastN) {
//         // Lee de MongoDB y convierte a Message de Spring AI
//     }
//
//     @Override
//     public void clear(String conversationId) {
//         // Borra los mensajes de esa conversación en MongoDB
//     }
// }
//
// Luego actualiza MemoryConfig para usar MongoChatMemory en vez de InMemoryChatMemory.
// Esto hace que la memoria sobreviva reinicios del servidor.
```

### Actividad 8.4 — Verificación

1. **Memoria**: Envía "Me llamo Carlos" → luego "¿Cómo me llamo?" → debe recordar
2. **RAG + Memoria**: Pregunta sobre VPN → luego pregunta "explica más sobre el paso 3" → debe recordar el contexto
3. **Moderación**: Envía "DROP TABLE tickets" → debe ser bloqueado
4. **Logging**: Verifica en la consola que cada interacción se loguea con duración

---

## ✅ Criterios de Evaluación — Fase 8

| Criterio | Cumple |
|----------|--------|
| `MessageChatMemoryAdvisor` integrado — la IA recuerda conversaciones | ☐ |
| `ContentModerationAdvisor` creado con filtrado de entrada y salida | ☐ |
| `LoggingAdvisor` loguea tiempo y tamaño de cada interacción | ☐ |
| Múltiples advisors encadenados en orden correcto | ☐ |
| Memoria persistente con MongoDB implementada (MongoChatMemory) | ☐ |
| La IA mantiene contexto conversacional a lo largo de múltiples mensajes | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿En qué orden se ejecutan los advisors y por qué importa? ¿Qué pasaría si el `ContentModerationAdvisor` se ejecutara DESPUÉS del `LoggingAdvisor`?
2. Si la memoria contiene los últimos 50 mensajes, ¿cuántos tokens consume eso? ¿Cómo limitarías la memoria para no exceder el context window del modelo?
3. ¿Cuál es la diferencia entre guardar la memoria en MongoDB (persistente) vs InMemory? ¿Cuándo usarías cada uno?

---

> ⬅️ **Anterior:** [Fase 7 — RAG PGVector](Lab5-Fase7-RAG-PGVector.md)  
> ➡️ **Siguiente:** [Fase 9 — MCP](Lab5-Fase9-MCP.md)
