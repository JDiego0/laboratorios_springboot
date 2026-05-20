# 🧪 FASE 6 — Tools / Function Calling con @Tool

**Módulo 6.1 · Semana 3 · Proyecto NexusAI 🤖**  
**Duración estimada:** 60 minutos  
**Prerrequisito:** Fase 5 completada  
**Objetivo:** Implementar herramientas (tools) que el modelo de IA puede invocar automáticamente para consultar datos reales, ejecutar acciones y conectarse con sistemas externos.

---

## 📖 Concepto Clave: Function Calling

Los LLMs por sí solos solo generan texto. **No pueden** consultar bases de datos, llamar APIs o ejecutar código. **Function Calling** (o Tool Use) resuelve esto:

1. Tú registras funciones/herramientas con una descripción.
2. El LLM analiza la pregunta del usuario.
3. Si necesita datos externos, el LLM **solicita** ejecutar una herramienta.
4. Spring AI ejecuta la herramienta automáticamente.
5. El resultado se inyecta de vuelta al LLM para generar la respuesta final.

```
Usuario: "¿Cuántos tickets críticos hay abiertos?"
    ↓
LLM piensa: "Necesito consultar la base de datos de tickets"
    ↓
LLM solicita: callTool("getOpenTicketsByPriority", {priority: "CRITICAL"})
    ↓
Spring AI ejecuta: ticketService.getOpenTicketsByPriority("CRITICAL") → 7
    ↓
LLM responde: "Actualmente hay 7 tickets críticos abiertos."
```

### `@Tool` vs `FunctionCallback` (deprecated)

Spring AI 1.0 introduce `@Tool` como la forma declarativa y moderna:

```java
// ✅ Moderno — Declarativo con @Tool
@Tool(description = "Get tickets by status")
public List<Ticket> getTicketsByStatus(@ToolParam(description = "OPEN, IN_PROGRESS, etc.") String status) {
    return ticketRepository.findByStatus(Ticket.TicketStatus.valueOf(status));
}

// ❌ Deprecated — Imperativo con FunctionCallback  
@Bean Function<StatusRequest, List<Ticket>> getTickets() { ... }
```

---

## 🛠️ Código Base Proporcionado

### 1. Tool de Tickets — `ai/tools/TicketTool.java`

```java
package com.nexusai.ai.tools;

import com.nexusai.domain.model.Ticket;
import com.nexusai.infrastructure.repository.TicketRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Tools for ticket management.
 * The AI model can invoke these methods when it needs real data.
 * 
 * IMPORTANT: The 'description' in @Tool is what the LLM reads
 * to decide WHEN to call this function. Make it clear and precise.
 */
@Service
public class TicketTool {

    private final TicketRepository ticketRepository;

    public TicketTool(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Tool(description = "Search for support tickets by their current status. " +
                         "Valid statuses: OPEN, IN_PROGRESS, RESOLVED, CLOSED.")
    public List<Ticket> getTicketsByStatus(
            @ToolParam(description = "The ticket status to filter by") String status) {
        return ticketRepository.findByStatus(
                Ticket.TicketStatus.valueOf(status.toUpperCase()));
    }

    @Tool(description = "Get the total count of tickets grouped by status. " +
                         "Useful for dashboard summaries and reports.")
    public String getTicketSummary() {
        long open = ticketRepository.countByStatus(Ticket.TicketStatus.OPEN);
        long inProgress = ticketRepository.countByStatus(Ticket.TicketStatus.IN_PROGRESS);
        long resolved = ticketRepository.countByStatus(Ticket.TicketStatus.RESOLVED);
        long closed = ticketRepository.countByStatus(Ticket.TicketStatus.CLOSED);
        return String.format(
            "Ticket Summary — Open: %d, In Progress: %d, Resolved: %d, Closed: %d, Total: %d",
            open, inProgress, resolved, closed, open + inProgress + resolved + closed);
    }

    @Tool(description = "Create a new support ticket in the system. " +
                         "Returns the created ticket with its assigned ID.")
    public Ticket createTicket(
            @ToolParam(description = "Short title describing the issue") String title,
            @ToolParam(description = "Detailed description of the problem") String description,
            @ToolParam(description = "Name of the person creating the ticket") String createdBy) {
        Ticket ticket = new Ticket(title, description, createdBy);
        return ticketRepository.save(ticket);
    }
}
```

### 2. Registrar Tools en el ChatClient

Modifica `OllamaAssistant.java` para registrar las herramientas:

```java
// En OllamaAssistant — modifica el método chat() para incluir tools:

@Override
public String chat(String userMessage) {
    return chatClient.prompt()
            .user(userMessage)
            .tools(ticketTool)         // ← Registra el tool
            .call()
            .content();
}
```

---

## 📝 Actividades — Tu Turno

### Actividad 6.1 — Crear tu Propio Tool: System Status

```java
// TODO: Crea ai/tools/SystemStatusTool.java
//
// Este tool permite a la IA consultar el estado de los sistemas de NexusTech.
//
// Requisitos:
// 1. Anotado con @Service
// 2. Al menos 3 métodos con @Tool:
//
//    a. @Tool("Check the health status of a specific service")
//       String checkServiceHealth(@ToolParam("Service name: 'database', 'api', 'email', 'storage'") String serviceName)
//       → Simula el estado (retorna "UP", "DOWN", "DEGRADED")
//       → Tip: Usa un Map<String, String> con datos hardcoded para simular
//
//    b. @Tool("Get system performance metrics")
//       String getPerformanceMetrics()
//       → Retorna métricas simuladas: CPU, memory, response time
//
//    c. @Tool("Get the current server time and uptime")
//       String getServerInfo()
//       → Retorna la hora actual y un uptime simulado
//
// La IA podrá decir cosas como:
// "El servicio de email está DOWN. La CPU está al 45%. El servidor lleva 72 horas activo."
```

### Actividad 6.2 — Crear Tool de Knowledge Base

```java
// TODO: Crea ai/tools/KnowledgeBaseTool.java
//
// Simula una búsqueda en la base de conocimiento de NexusTech.
//
// Requisitos:
// 1. Método con @Tool que busque artículos por keyword
// 2. Usa un List<Map<String, String>> con artículos hardcoded, ej:
//    - "password reset" → "To reset your password, go to Settings > Security..."
//    - "VPN setup"      → "Configure the VPN client with server: vpn.nexustech..."
//    - "license renewal" → "Contact your account manager or visit portal.nexustech..."
// 3. Retorna los artículos que coincidan con el keyword
//
// NOTA: En la Fase 7 (RAG) reemplazaremos esto con búsqueda vectorial real.
```

### Actividad 6.3 — Registrar todos los Tools

```java
// TODO: Modifica OllamaAssistant para registrar TODOS los tools:
//
// 1. Inyecta TicketTool, SystemStatusTool, KnowledgeBaseTool en el constructor
// 2. En el método chat(), pasa todos los tools:
//
//    return chatClient.prompt()
//            .user(userMessage)
//            .tools(ticketTool, systemStatusTool, knowledgeBaseTool)
//            .call()
//            .content();
```

### Actividad 6.4 — Insertar Datos de Prueba

Crea datos de prueba para que los tools tengan datos reales que consultar:

```java
// TODO: Crea config/DataSeeder.java
//
// Usa @Component + CommandLineRunner para insertar tickets de ejemplo al iniciar:
//
// @Component
// public class DataSeeder implements CommandLineRunner {
//     @Autowired TicketRepository ticketRepository;
//
//     @Override
//     public void run(String... args) {
//         if (ticketRepository.count() == 0) {
//             ticketRepository.save(new Ticket("Server crashes at 3am", 
//                 "Production server OOM every Monday", "carlos@nexus.com"));
//             // Agrega al menos 5 tickets más con diferentes status y prioridades
//         }
//     }
// }
```

### Actividad 6.5 — Verificación en el Chat

Abre el chat y prueba estas preguntas. La IA debe invocar los tools automáticamente:

1. "¿Cuántos tickets abiertos hay?" → Debe invocar `getTicketSummary()`
2. "¿Cuál es el estado del servicio de email?" → Debe invocar `checkServiceHealth("email")`
3. "Crea un ticket: el WiFi de la oficina no funciona" → Debe invocar `createTicket()`
4. "¿Cómo reseteo mi contraseña?" → Debe invocar el KB tool

---

## ✅ Criterios de Evaluación — Fase 6

| Criterio | Cumple |
|----------|--------|
| `TicketTool` funciona y la IA lo invoca correctamente | ☐ |
| Se creó `SystemStatusTool` con al menos 3 métodos `@Tool` | ☐ |
| Se creó `KnowledgeBaseTool` con búsqueda por keyword | ☐ |
| Todos los tools están registrados en `OllamaAssistant` con `.tools()` | ☐ |
| Datos de prueba insertados con `DataSeeder` | ☐ |
| La IA invoca los tools correctos según el contexto de la pregunta | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿Cómo decide el LLM cuándo invocar un tool y cuándo simplemente responder con texto? ¿Qué rol juega la `description` del `@Tool`?
2. ¿Qué riesgos de seguridad existen al permitir que un LLM ejecute funciones en tu sistema? ¿Qué pasaría si el tool pudiera borrar datos?
3. ¿Por qué es mejor tener un `KnowledgeBaseTool` separado en vez de meter la lógica de búsqueda directamente en el prompt?

---

> ⬅️ **Anterior:** [Fase 5 — Streaming SSE](Lab5-Fase5-Streaming-SSE.md)  
> ➡️ **Siguiente:** [Fase 7 — RAG con PGVector](Lab5-Fase7-RAG-PGVector.md)
