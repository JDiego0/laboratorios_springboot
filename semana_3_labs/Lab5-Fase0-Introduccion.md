# 🧪 LABORATORIO DÍA 5 — NexusAI: Plataforma de Soporte Inteligente con Spring AI

**Módulo 6.1 · Semana 3 · Proyecto NexusAI 🤖**  
**Duración estimada:** 8–10 horas (10 fases progresivas)  
**Nivel:** Avanzado  
**Prerrequisitos:** Labs 1–4 completados, Docker instalado  

---

## 🎯 Visión General

Este laboratorio es una experiencia **inmersiva y progresiva** donde construirás **NexusAI**: una plataforma enterprise de soporte técnico potenciada por Inteligencia Artificial. No se trata de simplemente "llamar a un LLM" — vas a diseñar una **arquitectura AI seria** con las mejores prácticas de la industria.

### ¿Qué vas a construir?

Una aplicación web completa con:

- 💬 **Chat en tiempo real** con Thymeleaf + WebSockets (STOMP)
- 🤖 **Asistente IA** que analiza tickets, responde preguntas y ejecuta acciones
- 🧠 **Memoria conversacional** persistente
- 📚 **RAG** (Retrieval-Augmented Generation) con base de conocimiento privada
- 🔧 **Tools/Function Calling** — la IA invoca herramientas de tu sistema
- 📡 **Streaming** tipo ChatGPT (token por token)
- 🔌 **MCP** (Model Context Protocol) para interoperabilidad
- 📊 **Observabilidad** y **seguridad** enterprise

---

## 📖 Contexto de Negocio — NexusAI

**NexusTech Solutions** es una empresa de tecnología que necesita una plataforma de soporte técnico inteligente para sus clientes. El equipo de desarrollo ha sido encargado de construir **NexusAI**, un sistema que:

1. Permite a los agentes de soporte chatear en tiempo real con un asistente de IA.
2. La IA tiene acceso a la **base de conocimiento** de la empresa (manuales, FAQs, documentación).
3. La IA puede **ejecutar acciones** como consultar el estado de tickets, verificar el inventario de licencias, y escalar problemas.
4. Todo debe ser **observable**, **seguro** y **desacoplado** del proveedor de IA.

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────┐
│                    FRONTEND (Thymeleaf)                  │
│  ┌─────────────┐  ┌─────────────┐  ┌────────────────┐  │
│  │  Chat View   │  │  Dashboard  │  │  Knowledge     │  │
│  │  (WebSocket) │  │  (REST)     │  │  Base Upload   │  │
│  └──────┬───────┘  └──────┬──────┘  └───────┬────────┘  │
└─────────┼─────────────────┼─────────────────┼───────────┘
          │ STOMP/WS        │ HTTP            │ HTTP
┌─────────▼─────────────────▼─────────────────▼───────────┐
│                  SPRING BOOT 4 BACKEND                   │
│                                                          │
│  ┌─── API Layer ─────────────────────────────────────┐  │
│  │  WebSocket Controllers  │  REST Controllers       │  │
│  │  UI Controllers (Thymeleaf)                       │  │
│  └────────────────────────┬──────────────────────────┘  │
│                           │                              │
│  ┌─── Application Layer ──▼──────────────────────────┐  │
│  │  TicketService  │  KnowledgeBaseService            │  │
│  │  UserService    │  NotificationService             │  │
│  └────────────────────────┬──────────────────────────┘  │
│                           │                              │
│  ┌─── AI Orchestration ───▼──────────────────────────┐  │
│  │                                                    │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │  │
│  │  │ Prompts  │ │  Tools   │ │  Memory          │   │  │
│  │  │ (.st)    │ │ (@Tool)  │ │  (ChatMemory)    │   │  │
│  │  └──────────┘ └──────────┘ └──────────────────┘   │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │  │
│  │  │  RAG     │ │ Advisors │ │  MCP             │   │  │
│  │  │ (Vector) │ │ (Chain)  │ │  (Server/Client) │   │  │
│  │  └──────────┘ └──────────┘ └──────────────────┘   │  │
│  │                                                    │  │
│  │  interface AiAssistant ──▶ OllamaAssistant        │  │
│  │                        ──▶ OpenAiAssistant         │  │
│  └───────────────────────────────────────────────────┘  │
│                                                          │
│  ┌─── Infrastructure Layer ──────────────────────────┐  │
│  │  PostgreSQL + PGVector  │  MongoDB (optional)     │  │
│  │  Redis (sessions)       │  Ollama (local LLM)     │  │
│  └───────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

---

## 🛠️ Stack Tecnológico

| Componente | Tecnología | Propósito |
|------------|-----------|-----------|
| **Runtime** | Java 21 | Lenguaje base |
| **Framework** | Spring Boot 4.0.x | Framework principal |
| **AI** | Spring AI 1.0+ | Orquestación de IA |
| **LLM Principal** | Ollama (Llama 3) | Modelo local gratuito |
| **LLM Secundario** | OpenAI (GPT-4.1) | Opción cloud (opcional) |
| **Frontend** | Thymeleaf 4 | Vistas server-side |
| **Tiempo Real** | WebSockets (STOMP) | Chat bidireccional |
| **Streaming** | SSE (Server-Sent Events) | Respuestas token-por-token |
| **Vector DB** | PostgreSQL + PGVector | RAG / búsqueda semántica |
| **Document DB** | MongoDB | Chat history (opcional) |
| **Containers** | Docker Compose | Infraestructura local |

---

## 📁 Estructura de Archivos del Laboratorio

Este laboratorio está dividido en **10 fases progresivas**. Cada fase se construye sobre la anterior:

| Fase | Archivo | Tema | Duración |
|------|---------|------|----------|
| 0 | `Lab5-Fase0-Introduccion.md` | Este documento | — |
| 1 | `Lab5-Fase1-Proyecto-Arquitectura.md` | Setup, Docker, estructura | 45 min |
| 2 | `Lab5-Fase2-ChatClient-StructuredOutputs.md` | ChatClient, records, `.entity()` | 45 min |
| 3 | `Lab5-Fase3-Prompt-Engineering.md` | Templates `.st`, versionables | 30 min |
| 4 | `Lab5-Fase4-Chat-Thymeleaf-WebSockets.md` | Chat web, STOMP, frontend pro | 90 min |
| 5 | `Lab5-Fase5-Streaming-SSE.md` | Streaming tipo ChatGPT | 45 min |
| 6 | `Lab5-Fase6-Tools-FunctionCalling.md` | `@Tool`, `@ToolParam`, acciones | 60 min |
| 7 | `Lab5-Fase7-RAG-PGVector.md` | VectorStore, embeddings, RAG | 60 min |
| 8 | `Lab5-Fase8-Advisors-Memoria.md` | ChatMemory, advisors custom | 45 min |
| 9 | `Lab5-Fase9-MCP.md` | MCP server/client | 45 min |
| 10 | `Lab5-Fase10-Observabilidad-Seguridad.md` | Monitoring, testing, security | 45 min |

---

## 📏 Principio Fundamental: DESACOPLAMIENTO

> **La regla #1 de este laboratorio:**  
> **NUNCA** acoples tu aplicación directamente al modelo de IA.

❌ **JAMÁS hagas esto:**
```java
// ANTI-PATRÓN: Acoplamiento directo al proveedor
@Autowired
OpenAiChatModel openAiModel; // ← NO

public String ask(String question) {
    return openAiModel.call(question); // ← TERRIBLE
}
```

✅ **SIEMPRE haz esto:**
```java
// PATRÓN CORRECTO: Abstracción con interface
public interface AiAssistant {
    SupportResponse analyzeTicket(TicketRequest request);
    Flux<String> streamResponse(String question);
}

@Service
@Profile("ollama")
class OllamaAssistant implements AiAssistant {
    // Implementación con Ollama
}

@Service
@Profile("openai")
class OpenAiAssistant implements AiAssistant {
    // Implementación con OpenAI
}
```

**¿Por qué?** Porque así puedes:
- 🔄 Cambiar de GPT → Claude → Llama sin tocar tu código
- 💰 Balancear costos entre modelos
- 🏠 Usar Ollama local en desarrollo y OpenAI en producción
- 🛡️ Hacer fallback si un proveedor falla
- ✅ Testear con mocks sin llamar a la IA real

---

## 🚦 Antes de Empezar — Checklist

Verifica que tienes todo listo antes de comenzar la Fase 1:

- [ ] **Java 21** instalado (`java --version`)
- [ ] **Maven 3.9+** instalado (`mvn --version`)
- [ ] **Docker** y **Docker Compose** instalados (`docker --version`)
- [ ] **IDE** configurado (IntelliJ IDEA recomendado)
- [ ] **Git** para versionar tu progreso
- [ ] Los **Labs 1–4** de la Semana 3 completados
- [ ] (Opcional) API Key de OpenAI si deseas usar el proveedor cloud

---

## 📐 Convención del Código

A diferencia de los laboratorios anteriores (que usaron nombres en español), este laboratorio utiliza **inglés** en todo el código para seguir las convenciones enterprise y estándares profesionales de la industria:

| Antes (Labs 1-4) | Ahora (Lab 5) |
|-------------------|---------------|
| `MensajeService` | `MessageService` |
| `obtenerHistorial()` | `getHistory()` |
| `guardarMensaje()` | `saveMessage()` |
| `Mensaje.java` | `ChatMessage.java` |

> 💡 **Nota pedagógica:** En la industria real, el código Java se escribe en inglés. Los comentarios, la documentación y las instrucciones de este laboratorio siguen en español para facilitar el aprendizaje.

---

## 🎓 Metodología del Laboratorio

Cada fase sigue este patrón:

1. **📖 Explicación Conceptual** — Entiendes el "por qué" antes del "cómo"
2. **🛠️ Código Base** — Código funcional que ya está listo para ti
3. **✍️ Secciones `// TODO:`** — Partes que TÚ debes completar demostrando tu conocimiento
4. **🧪 Verificación** — Cómo probar que tu código funciona
5. **✅ Criterios de Evaluación** — Lo que se va a evaluar en cada fase
6. **🔍 Preguntas de Reflexión** — Para profundizar tu comprensión

> ⚠️ **Importante:** Las secciones marcadas con `// TODO:` son las que demuestran tu competencia como desarrollador. El código base te da el contexto, pero TÚ construyes la solución.

---

## 🏆 Criterios de Evaluación Global

| Criterio | Peso |
|----------|------|
| Arquitectura desacoplada (interface AiAssistant) | 15% |
| Structured outputs con records y `.entity()` | 10% |
| Prompt templates externalizados y versionables | 5% |
| Chat funcional con Thymeleaf + WebSockets | 15% |
| Streaming implementado correctamente | 10% |
| Al menos 2 tools con `@Tool` funcionales | 10% |
| RAG con PGVector operativo | 10% |
| Advisors configurados (memoria + RAG) | 10% |
| MCP server expuesto correctamente | 5% |
| Testing y observabilidad básica | 5% |
| Código limpio, documentado y profesional | 5% |

---

## 📚 Recursos de Referencia

- [Spring AI Reference Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
- [Ollama — Run LLMs Locally](https://ollama.ai/)
- [PGVector Documentation](https://github.com/pgvector/pgvector)
- [Spring Initializr](https://start.spring.io/)
- [STOMP Over WebSocket](https://stomp-js.github.io/stomp-websocket/)

---

> 🚀 **¡Comienza con la Fase 1!** → [`Lab5-Fase1-Proyecto-Arquitectura.md`](Lab5-Fase1-Proyecto-Arquitectura.md)
