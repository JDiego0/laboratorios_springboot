# 🧪 Laboratorio Spring AI — Plataforma NexusAI

## Objetivo

Crear un **laboratorio exhaustivo y progresivo** (Lab5-Dia5) para la Semana 3 que guíe a los estudiantes en la construcción de **NexusAI**: una plataforma de soporte técnico inteligente con chat en tiempo real, usando Spring Boot 4, Spring AI, Thymeleaf, WebSockets y arquitectura enterprise.

## Contexto

- Los labs anteriores (1-4) cubrieron Thymeleaf básico, formularios, fragmentos, MongoDB y WebSockets con un ChatBot simple.
- Este Lab5 eleva el nivel drásticamente: arquitectura desacoplada, structured outputs, RAG, tools, MCP, streaming, y observabilidad.
- Dominio: **NexusAI** — plataforma de soporte técnico con IA para una empresa de tecnología (distinto de LibroTech/ChatTech).

---

## User Review Required

> [!IMPORTANT]
> **Extensión del laboratorio**: Este laboratorio es sustancialmente más largo que los anteriores (~3x). Propongo dividirlo en **10 fases progresivas** donde cada fase se construye sobre la anterior. ¿Prefiere un solo archivo .md extenso o dividirlo en múltiples archivos (ej. Lab5-Fase1.md, Lab5-Fase2.md)?

> [!WARNING]
> **Proveedores de LLM**: El lab usará **Ollama (Llama 3)** como proveedor principal para desarrollo local (gratis, sin API key). Se incluirán instrucciones opcionales para OpenAI. ¿Está bien este enfoque o prefiere que sea OpenAI el principal?

> [!IMPORTANT]
> **Base de datos**: Los labs anteriores usan MongoDB. Este lab introduce **PostgreSQL + PGVector** para RAG. ¿Desea que también se mantenga compatibilidad con MongoDB o solo PostgreSQL?

## Open Questions

1. **Nombre del archivo**: ¿Mantener la convención `Lab5-Dia5-Spring-AI-Enterprise.md` o usar otro nombre dado que es un lab especial/avanzado?
2. **Idioma del código**: Los labs anteriores usan español en nombres de clases (ej. `MensajeService`). ¿Continuar con español o usar inglés en este lab dado que es más enterprise/profesional?
3. **Docker Compose**: ¿Los estudiantes tienen Docker instalado y accesible? Es necesario para PostgreSQL+PGVector y Ollama.

---

## Proposed Changes

### Estructura del Laboratorio (10 Fases Progresivas)

El laboratorio se construye como un documento único extenso con 10 fases. Cada fase tiene:
- 📖 Explicación conceptual
- 🛠️ Código base proporcionado
- ✍️ Secciones `// TODO:` para que el estudiante complete
- ✅ Criterios de evaluación por fase

---

### Fase 1 — Fundamentos: Proyecto y Arquitectura

#### [NEW] Estructura del proyecto + docker-compose.yml

```
nexus-ai/
├── docker-compose.yml          (PostgreSQL + PGVector + Ollama)
├── pom.xml                     (Spring Boot 4, Spring AI, WebSocket, Thymeleaf, PGVector)
└── src/main/java/com/nexusai/
     ├── ai/
     │    ├── config/            (ChatClient, VectorStore beans)
     │    ├── prompts/           (Prompt templates .st)
     │    ├── rag/               (Document ingestion, retrieval)
     │    ├── tools/             (Function calling con @Tool)
     │    ├── memory/            (Chat memory configuration)
     │    ├── advisors/          (Custom advisors)
     │    ├── models/            (DTOs para structured output)
     │    ├── mcp/               (MCP server/client)
     │    └── orchestration/     (AiAssistant interface + impl)
     ├── application/
     │    └── service/           (Business services)
     ├── domain/
     │    └── model/             (Entidades JPA)
     ├── infrastructure/
     │    └── repository/        (Spring Data repos)
     └── api/
          ├── rest/              (REST controllers)
          ├── ws/                (WebSocket controllers)
          └── ui/                (Thymeleaf controllers)
```

**Contenido**: Explicación de la arquitectura hexagonal, por qué separar capas AI de negocio, `pom.xml` completo, `docker-compose.yml`, `application.yml`.

---

### Fase 2 — ChatClient y Structured Outputs

**Concepto**: Interfaz `AiAssistant` desacoplada del modelo, DTOs como records para structured output.

**Código base**:
- Interface `AiAssistant`
- Record DTOs: `TicketAnalysis`, `SupportResponse`
- Configuración `AiConfig` con `ChatClient` bean

**TODO estudiante**:
- Implementar `OllamaAssistant implements AiAssistant`
- Crear sus propios DTOs con `.entity()`
- Crear endpoint REST básico de prueba

---

### Fase 3 — Prompt Engineering Profesional

**Concepto**: Prompt templates en `resources/prompts/*.st`, versionables y testeables.

**Código base**:
- Archivo `ticket-analysis.st` de ejemplo
- `PromptService` que carga templates

**TODO estudiante**:
- Crear sus propios templates (support-response.st, escalation-check.st)
- Parametrizar con variables dinámicas
- Testear con diferentes inputs

---

### Fase 4 — Chat Web con Thymeleaf + WebSockets (STOMP)

**Concepto**: Vista Thymeleaf profesional con WebSockets para chat en tiempo real.

**Código base**:
- `WebSocketConfig` completo
- Template Thymeleaf `chat/sala.html` con diseño profesional (CSS moderno, dark mode)
- Fragmentos reutilizables (navbar, sidebar)
- JavaScript con SockJS + STOMP

**TODO estudiante**:
- Completar `ChatSocketController` con lógica de orquestación
- Implementar indicador de "IA escribiendo..."
- Manejar errores en el frontend
- Agregar historial visual

---

### Fase 5 — Streaming (SSE + WebSocket)

**Concepto**: Respuestas tipo ChatGPT token-por-token.

**Código base**:
- Endpoint SSE con `Flux<ServerSentEvent>`
- JavaScript con `EventSource` API

**TODO estudiante**:
- Integrar streaming con WebSocket (enviar tokens progresivos)
- Implementar indicador visual de streaming
- Manejar cancelación de stream

---

### Fase 6 — Tools / Function Calling con @Tool

**Concepto**: El modelo decide cuándo llamar herramientas externas.

**Código base**:
- `TicketService` con `@Tool` para consultar tickets
- `SystemStatusService` con `@Tool` para estado del sistema

**TODO estudiante**:
- Crear su propia herramienta (ej. búsqueda en KB, crear ticket)
- Registrar tools en `ChatClient`
- Verificar que el modelo las invoca correctamente

---

### Fase 7 — RAG con PostgreSQL + PGVector

**Concepto**: Conocimiento privado → chunking → embeddings → retrieval → prompt → LLM.

**Código base**:
- Configuración `VectorStore` con PGVector
- Servicio de ingesta de documentos
- `QuestionAnswerAdvisor`

**TODO estudiante**:
- Cargar sus propios documentos (knowledge base)
- Implementar búsqueda semántica
- Integrar RAG en el flujo del chat

---

### Fase 8 — Advisors: Memoria y Contexto

**Concepto**: `MessageChatMemoryAdvisor`, `QuestionAnswerAdvisor`, custom advisors.

**Código base**:
- Configuración de memoria con `ChatMemory`
- Ejemplo de advisor personalizado (logging/audit)

**TODO estudiante**:
- Implementar memoria conversacional persistente
- Crear advisor de moderación/seguridad
- Encadenar múltiples advisors

---

### Fase 9 — MCP (Model Context Protocol)

**Concepto**: Exponer herramientas como servidor MCP y consumir MCPs externos.

**Código base**:
- `@McpTool` anotaciones de ejemplo
- Configuración MCP server

**TODO estudiante**:
- Crear un MCP server con sus propias herramientas
- Configurar un MCP client que las consuma
- Integrar en el flujo del chat

---

### Fase 10 — Observabilidad y Seguridad

**Concepto**: Monitoreo, seguridad, testing, y buenas prácticas.

**Código base**:
- Configuración OpenTelemetry básica
- Filtro de seguridad (masking PII)
- Test de ejemplo

**TODO estudiante**:
- Implementar logging de prompts y tokens
- Crear guardrails de seguridad
- Escribir tests determinísticos (estructura, no texto exacto)
- Documentar su arquitectura final

---

### Archivos a crear

#### [NEW] [Lab5-Dia5-Spring-AI-Enterprise.md](file:///home/angela-monsalve/Documentos/RA%20JAVA/OneDrive_2026-05-03/laboratorios_springboot/semana_3_labs/Lab5-Dia5-Spring-AI-Enterprise.md)

Archivo único con las 10 fases completas. Incluye:
- Encabezado con metadata (duración ~6-8 horas, nivel avanzado)
- Contexto de negocio NexusAI
- Las 10 fases con código base + TODOs
- Criterios de evaluación globales
- Preguntas de reflexión avanzadas
- Rúbrica completa
- Recursos y referencias

---

## Verification Plan

### Manual Verification
- Revisión del formato y consistencia con labs anteriores (1-4)
- Verificación de que todos los bloques de código Java son sintácticamente correctos
- Validación de que las dependencias `pom.xml` son reales y compatibles con Spring Boot 4 + Spring AI 1.0+
- Confirmación de que el `docker-compose.yml` funciona (pgvector + ollama)
- Revisión de la progresión pedagógica (cada fase construye sobre la anterior)
