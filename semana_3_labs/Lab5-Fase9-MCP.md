# 🧪 FASE 9 — MCP (Model Context Protocol)

**Módulo 6.1 · Semana 3 · Proyecto NexusAI 🤖**  
**Duración estimada:** 45 minutos  
**Prerrequisito:** Fase 8 completada  
**Objetivo:** Exponer las herramientas de NexusAI como un servidor MCP estandarizado y consumir servidores MCP externos, habilitando interoperabilidad entre sistemas de IA.

---

## 📖 Concepto Clave: Model Context Protocol (MCP)

MCP es un **estándar abierto** (creado por Anthropic, adoptado por la industria) que define cómo los modelos de IA se conectan con herramientas externas. Es el "USB-C de la IA" — un protocolo universal.

### Sin MCP (antes):
```
App A → Tool propio A → LLM A
App B → Tool propio B → LLM B
(Cada app reinventa la rueda, herramientas no son reutilizables)
```

### Con MCP (ahora):
```
App A ──┐
App B ──┤──→ MCP Server (herramientas estandarizadas) ──→ Cualquier LLM
App C ──┘
```

### Spring AI + MCP

Spring AI es un **contribuidor principal** al SDK oficial de MCP para Java. Ofrece:
- `@McpTool` — Exponer herramientas como servidor MCP
- Auto-configuración con Boot Starters
- Transportes: STDIO, HTTP Streamable, SSE
- Integración automática con `ChatClient`

---

## 🛠️ Código Base Proporcionado

### 1. Configuración MCP Server — `application.yml` (agregar)

Agrega esta sección a tu `application.yml`:

```yaml
  # ─── MCP Server Configuration ────────────────────
  ai:
    mcp:
      server:
        name: nexusai-mcp-server
        version: 1.0.0
        type: SYNC  # SYNC para WebMVC, ASYNC para WebFlux
```

### 2. MCP Tool de ejemplo — `ai/mcp/NexusMcpTools.java`

```java
package com.nexusai.ai.mcp;

import com.nexusai.domain.model.Ticket;
import com.nexusai.infrastructure.repository.TicketRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP-exposed tools for NexusAI.
 * 
 * These tools are automatically discoverable by any MCP client.
 * External AI agents (Claude Desktop, Cursor, other Spring AI apps)
 * can connect to this server and use these tools.
 * 
 * IMPORTANT: In Spring AI 1.0+, @Tool works for both regular tool calling
 * AND MCP. The MCP server starter automatically exposes @Tool methods
 * as MCP tools when spring-ai-starter-mcp-server-webmvc is on the classpath.
 */
@Service
public class NexusMcpTools {

    private final TicketRepository ticketRepository;

    public NexusMcpTools(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Tool(description = "List all open support tickets from NexusTech's ticketing system. " +
                         "Returns ticket ID, title, priority, and creation date.")
    public List<Map<String, String>> listOpenTickets() {
        return ticketRepository.findByStatus(Ticket.TicketStatus.OPEN)
                .stream()
                .map(t -> Map.of(
                    "id", String.valueOf(t.getId()),
                    "title", t.getTitle(),
                    "priority", t.getPriority().name(),
                    "createdAt", t.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }

    @Tool(description = "Get detailed information about a specific ticket by its ID.")
    public Map<String, String> getTicketDetails(
            @ToolParam(description = "The numeric ticket ID") Long ticketId) {
        return ticketRepository.findById(ticketId)
                .map(t -> Map.of(
                    "id", String.valueOf(t.getId()),
                    "title", t.getTitle(),
                    "description", t.getDescription(),
                    "status", t.getStatus().name(),
                    "priority", t.getPriority().name(),
                    "assignedTo", t.getAssignedTo() != null ? t.getAssignedTo() : "Unassigned",
                    "createdBy", t.getCreatedBy(),
                    "createdAt", t.getCreatedAt().toString()
                ))
                .orElse(Map.of("error", "Ticket not found"));
    }
}
```

---

## 📝 Actividades — Tu Turno

### Actividad 9.1 — Agregar tus Propias Herramientas MCP

```java
// TODO: Agrega al menos 2 métodos @Tool adicionales a NexusMcpTools.java
//
// Ideas:
// 1. @Tool("Update the status of a ticket")
//    Map<String, String> updateTicketStatus(Long ticketId, String newStatus)
//    → Cambia el status del ticket en la base de datos
//
// 2. @Tool("Search tickets by keyword in title or description") 
//    List<Map<String, String>> searchTickets(String keyword)
//    → Busca tickets que contengan el keyword
//
// 3. @Tool("Get ticket statistics for a dashboard")
//    Map<String, Object> getTicketStats()
//    → Retorna: total, por status, por prioridad, ticket más antiguo
```

### Actividad 9.2 — Verificar el MCP Server

Una vez la aplicación está corriendo, el servidor MCP expone sus herramientas. Verifica que funciona:

```bash
# El MCP server expone metadata en su endpoint
# Si usas HTTP transport, puedes verificar con:
curl http://localhost:8080/mcp/tools

# Deberías ver la lista de herramientas disponibles con sus schemas
```

### Actividad 9.3 — Crear un MCP Client (Consumir MCP Externo)

```java
// TODO: Crea ai/mcp/McpClientConfig.java
//
// Configura un MCP Client que pueda consumir herramientas de servidores MCP externos.
//
// Ejemplo conceptual de configuración en application.yml:
//
// spring:
//   ai:
//     mcp:
//       client:
//         servers:
//           - name: weather-server
//             url: http://localhost:8081/mcp
//           - name: calendar-server  
//             url: http://localhost:8082/mcp
//
// Spring AI descubrirá automáticamente las herramientas de esos servidores
// y las hará disponibles para el ChatClient.
//
// NOTA: Para este lab, documentar la configuración es suficiente.
// En un ambiente real, conectarías con servidores MCP de terceros.
```

### Actividad 9.4 — Dashboard de Herramientas MCP (Thymeleaf)

```html
<!-- TODO: Crea templates/mcp/dashboard.html -->
<!--                                          -->
<!-- Una vista Thymeleaf que muestre:          -->
<!-- 1. Lista de herramientas MCP expuestas    -->
<!-- 2. Descripción de cada herramienta        -->
<!-- 3. Schema de parámetros de entrada        -->
<!-- 4. Botón para probar cada herramienta     -->
<!--                                          -->
<!-- Crea también api/ui/McpDashboardController.java -->
<!-- que alimente esta vista con los datos     -->
```

---

## ✅ Criterios de Evaluación — Fase 9

| Criterio | Cumple |
|----------|--------|
| MCP Server configurado en `application.yml` | ☐ |
| Al menos 4 herramientas `@Tool` expuestas via MCP | ☐ |
| Las herramientas MCP funcionan y retornan datos reales | ☐ |
| Documentación de cómo conectar un MCP Client externo | ☐ |
| Vista Thymeleaf mostrando herramientas MCP disponibles | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿Cuál es la diferencia entre un `@Tool` usado directamente con `ChatClient.tools()` y un `@Tool` expuesto via MCP? ¿Cuándo usarías cada uno?
2. ¿Qué ventaja tiene MCP sobre simplemente exponer una API REST para que otros sistemas consuman tus herramientas?
3. Si Claude Desktop se conecta a tu MCP Server, ¿quién decide cuándo ejecutar una herramienta: Claude o tu servidor? Investiga el flujo de tool invocation en MCP.

---

> ⬅️ **Anterior:** [Fase 8 — Advisors y Memoria](Lab5-Fase8-Advisors-Memoria.md)  
> ➡️ **Siguiente:** [Fase 10 — Observabilidad y Seguridad](Lab5-Fase10-Observabilidad-Seguridad.md)
