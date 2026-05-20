# 🧪 FASE 7 — RAG con PostgreSQL + PGVector

**Módulo 6.1 · Semana 3 · Proyecto NexusAI 🤖**  
**Duración estimada:** 60 minutos  
**Prerrequisito:** Fase 6 completada  
**Objetivo:** Implementar Retrieval-Augmented Generation (RAG) usando PGVector como base de datos vectorial, permitiendo que la IA responda con conocimiento privado de la empresa.

---

## 📖 Concepto Clave: RAG (Retrieval-Augmented Generation)

Los LLMs solo conocen datos de su entrenamiento. **No saben nada** sobre tu empresa, tus productos, ni tus clientes. RAG resuelve esto:

```
Documentos privados → Chunking → Embeddings → Vector Store (PGVector)
                                                      ↓
Usuario pregunta → Busca chunks similares → Inyecta en prompt → LLM → Respuesta informada
```

### Pipeline RAG paso a paso:

1. **Ingesta**: Cargar documentos (PDFs, TXT, HTML, etc.)
2. **Chunking**: Dividir en fragmentos de ~500-1000 tokens
3. **Embedding**: Convertir cada chunk en un vector numérico (ej. 768 dimensiones)
4. **Store**: Guardar vectores en PGVector
5. **Retrieval**: Cuando el usuario pregunta, convertir la pregunta en vector y buscar chunks similares
6. **Augmented Generation**: Inyectar los chunks encontrados en el prompt y generar respuesta

### ¿Por qué PGVector?

| Ventaja | Detalle |
|---------|---------|
| Simple | Es solo una extensión de PostgreSQL |
| Barato | Sin infra adicional — ya tienes Postgres |
| Transaccional | ACID compliant, backups normales |
| Robusto | Años de madurez de PostgreSQL |

---

## 🛠️ Código Base Proporcionado

### 1. Configuración del VectorStore — `ai/rag/RagConfig.java`

La mayor parte de la configuración ya está en `application.yml` (Fase 1). Spring AI auto-configura el `VectorStore` bean. Solo necesitas una configuración adicional:

```java
package com.nexusai.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for ingesting documents into the vector store.
 * Documents are automatically chunked and embedded by Spring AI.
 */
@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Ingest a single text document into the vector store.
     * Spring AI handles: chunking → embedding → storage automatically.
     */
    public void ingestDocument(String content, String source, String category) {
        Document document = new Document(
            content,
            Map.of(
                "source", source,
                "category", category,
                "ingested_at", java.time.Instant.now().toString()
            )
        );
        vectorStore.add(List.of(document));
    }

    /**
     * Ingest multiple documents at once (batch).
     */
    public void ingestDocuments(List<Document> documents) {
        vectorStore.add(documents);
    }

    /**
     * Search for similar documents given a query.
     * Returns the top-k most relevant chunks.
     */
    public List<Document> searchSimilar(String query, int topK) {
        return vectorStore.similaritySearch(
            org.springframework.ai.vectorstore.SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build()
        );
    }
}
```

### 2. Datos de Knowledge Base — `resources/knowledge/`

Crea la carpeta `src/main/resources/knowledge/` con archivos de texto:

**`resources/knowledge/password-reset.txt`**
```
How to Reset Your Password - NexusTech Support Guide

If you've forgotten your password, follow these steps:
1. Go to https://portal.nexustech.com/login
2. Click "Forgot Password" below the login form
3. Enter your registered email address
4. Check your inbox for the reset link (expires in 1 hour)
5. Click the link and create a new password (minimum 12 characters, must include uppercase, lowercase, number, and special character)

If you don't receive the email:
- Check your spam/junk folder
- Verify you're using the correct email address
- Contact support at help@nexustech.com
- For urgent cases, call +1-555-NEXUS (63987)

Security Note: NexusTech will never ask for your password via email or phone. If someone asks, report it to security@nexustech.com immediately.
```

**`resources/knowledge/vpn-setup.txt`**
```
VPN Configuration Guide - NexusTech Remote Access

Requirements:
- NexusTech VPN Client v3.2 or later
- Valid employee credentials
- Company-issued security certificate

Setup Steps:
1. Download the VPN client from https://tools.nexustech.com/vpn
2. Install and launch the application
3. Server address: vpn.nexustech.com
4. Port: 443 (SSL)
5. Protocol: OpenVPN (UDP preferred, TCP fallback)
6. Import your security certificate (.p12 file from IT department)
7. Enter your employee username and password
8. Enable "Auto-reconnect" in settings

Troubleshooting:
- Connection timeout: Check firewall settings, port 443 must be open
- Certificate error: Request a new certificate from IT (certs expire every 90 days)
- Slow connection: Try switching from UDP to TCP protocol
- DNS issues: Set DNS to 10.0.0.1 (internal) and 8.8.8.8 (fallback)
```

---

## 📝 Actividades — Tu Turno

### Actividad 7.1 — Ingesta de Documentos al Iniciar

```java
// TODO: Crea ai/rag/KnowledgeBaseLoader.java
//
// Este componente carga los archivos de knowledge/ al iniciar la aplicación.
//
// Requisitos:
// 1. Anotado con @Component, implementa CommandLineRunner
// 2. Inyecta DocumentIngestionService
// 3. En run():
//    a. Lee cada archivo .txt de resources/knowledge/
//    b. Llama a ingestDocument() con el contenido, nombre del archivo y categoría
//    c. Loguea cuántos documentos fueron ingestados
//
// Pista: Usa ResourcePatternResolver o ClassPathResource para leer archivos.
//
// @Component
// public class KnowledgeBaseLoader implements CommandLineRunner {
//     
//     @Value("classpath:knowledge/*.txt")
//     private Resource[] knowledgeFiles;
//     
//     private final DocumentIngestionService ingestionService;
//     
//     // Constructor...
//     
//     @Override
//     public void run(String... args) throws Exception {
//         for (Resource file : knowledgeFiles) {
//             String content = new String(file.getInputStream().readAllBytes());
//             String filename = file.getFilename();
//             ingestionService.ingestDocument(content, filename, "knowledge-base");
//             // Log: "Ingested: " + filename
//         }
//     }
// }
```

### Actividad 7.2 — Crear al menos 3 Documentos Adicionales

```
// TODO: Crea al menos 3 archivos .txt más en resources/knowledge/
//
// Ideas:
// - license-management.txt  → Cómo gestionar licencias de software
// - incident-response.txt   → Procedimiento ante incidentes de seguridad  
// - onboarding-guide.txt    → Guía para nuevos empleados
//
// Cada archivo debe tener al menos 200 palabras con información realista.
// Recuerda: la calidad del RAG depende de la calidad de los documentos.
```

### Actividad 7.3 — Endpoint REST para Búsqueda Semántica

```java
// TODO: Crea api/rest/KnowledgeBaseController.java
//
// @RestController
// @RequestMapping("/api/knowledge")
// public class KnowledgeBaseController {
//
//     // Inyecta DocumentIngestionService
//
//     @GetMapping("/search")
//     public ResponseEntity<List<Map<String, Object>>> search(
//             @RequestParam String query,
//             @RequestParam(defaultValue = "3") int topK) {
//         // 1. Llama a documentIngestionService.searchSimilar(query, topK)
//         // 2. Mapea los Document a un formato JSON legible:
//         //    - content (texto del chunk)
//         //    - metadata (source, category, etc.)
//         //    - Similarity score si está disponible
//         // 3. Retorna la lista
//     }
//
//     @PostMapping("/ingest")
//     public ResponseEntity<String> ingest(
//             @RequestParam String content,
//             @RequestParam String source) {
//         // Permite ingestar nuevos documentos via API
//     }
// }
```

### Actividad 7.4 — Integrar RAG en el Chat con `QuestionAnswerAdvisor`

Esta es la parte más poderosa. Spring AI tiene un advisor que automatiza todo el flujo RAG:

```java
// TODO: Modifica OllamaAssistant para incluir RAG
//
// Opción A — Con QuestionAnswerAdvisor (recomendado):
//
// import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
// import org.springframework.ai.vectorstore.VectorStore;
//
// // En el constructor, inyecta VectorStore
// private final VectorStore vectorStore;
//
// // En el método chat():
// return chatClient.prompt()
//         .user(userMessage)
//         .advisors(new QuestionAnswerAdvisor(vectorStore))  // ← RAG automático
//         .tools(ticketTool, systemStatusTool)
//         .call()
//         .content();
//
// ¿Qué hace QuestionAnswerAdvisor internamente?
// 1. Toma la pregunta del usuario
// 2. Busca chunks similares en PGVector
// 3. Inyecta los chunks encontrados en el prompt (como contexto)
// 4. El LLM responde usando ese contexto
//
// Opción B — Manual (para entender el proceso):
//
// List<Document> relevantDocs = documentIngestionService.searchSimilar(userMessage, 3);
// String context = relevantDocs.stream()
//     .map(Document::getContent)
//     .collect(Collectors.joining("\n\n"));
// 
// String enhancedPrompt = "Use this context to answer:\n" + context + "\n\nQuestion: " + userMessage;
// return chatClient.prompt().user(enhancedPrompt).call().content();
```

### Actividad 7.5 — Verificación

```bash
# 1. Verificar que los documentos fueron ingestados
curl "http://localhost:8080/api/knowledge/search?query=how%20to%20reset%20password&topK=2"

# 2. Probar en el chat: pregunta algo que está en los documentos
# Abre http://localhost:8080/chat y pregunta:
# "¿Cómo configuro la VPN de NexusTech?"
# → La respuesta debe contener datos específicos de vpn-setup.txt

# 3. Pregunta algo que NO está en los documentos
# "¿Cuál es el horario de la oficina?"
# → La IA debe indicar que no tiene esa información
```

---

## ✅ Criterios de Evaluación — Fase 7

| Criterio | Cumple |
|----------|--------|
| Al menos 5 documentos `.txt` en `resources/knowledge/` | ☐ |
| `KnowledgeBaseLoader` ingesta los documentos al iniciar | ☐ |
| `DocumentIngestionService` puede buscar por similitud semántica | ☐ |
| Endpoint REST `/api/knowledge/search` retorna resultados relevantes | ☐ |
| RAG integrado en el chat (via `QuestionAnswerAdvisor` o manual) | ☐ |
| La IA responde con datos de los documentos cuando son relevantes | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿Qué son los "embeddings" y por qué permiten buscar por **significado** en vez de por palabras exactas? (Ej: buscar "forgot my password" encuentra el documento sobre "password reset")
2. ¿Qué dimensiones (`768`) configuramos en PGVector? ¿Qué pasa si cambias el modelo de embeddings a uno con dimensiones diferentes?
3. ¿Cuál es la diferencia entre el `KnowledgeBaseTool` (Fase 6) que busca por keyword hardcoded y el RAG (Fase 7) que busca por similitud semántica? ¿Cuál es mejor y por qué?

---

> ⬅️ **Anterior:** [Fase 6 — Tools Function Calling](Lab5-Fase6-Tools-FunctionCalling.md)  
> ➡️ **Siguiente:** [Fase 8 — Advisors y Memoria](Lab5-Fase8-Advisors-Memoria.md)
