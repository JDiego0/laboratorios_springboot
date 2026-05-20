# 🧪 FASE 1 — Proyecto, Infraestructura y Arquitectura

**Módulo 6.1 · Semana 3 · Proyecto NexusAI 🤖**  
**Duración estimada:** 45 minutos  
**Objetivo:** Configurar el proyecto Spring Boot 4 con Spring AI, levantar la infraestructura Docker y establecer la arquitectura de paquetes enterprise.

---

## 📖 Concepto Clave: Infraestructura como Código

En desarrollo moderno con IA, la infraestructura no es un detalle menor. Necesitas:
- Un **LLM local** (Ollama) para no depender de APIs de pago durante el desarrollo.
- Una **base de datos vectorial** (PGVector) para RAG.
- Una **base de datos relacional** (PostgreSQL) para datos transaccionales.
- Una **base de datos documental** (MongoDB) como alternativa para historial de chat.

Docker Compose nos permite levantar todo con un solo comando.

---

## 🛠️ Código Base Proporcionado

### 1. Docker Compose — `docker-compose.yml`

Este archivo levanta toda la infraestructura necesaria. Crea este archivo en la raíz de tu proyecto:

```yaml
# docker-compose.yml — NexusAI Infrastructure
version: '3.9'

services:

  # ═══════════════════════════════════════════
  # PostgreSQL + PGVector (RAG + datos)
  # ═══════════════════════════════════════════
  postgres:
    image: pgvector/pgvector:pg16
    container_name: nexusai-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: nexusai
      POSTGRES_USER: nexusai
      POSTGRES_PASSWORD: nexusai_secret
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U nexusai"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ═══════════════════════════════════════════
  # MongoDB (Chat history — opcional)
  # ═══════════════════════════════════════════
  mongodb:
    image: mongo:7
    container_name: nexusai-mongo
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_DATABASE: nexusai_chat
    volumes:
      - mongodata:/data/db

  # ═══════════════════════════════════════════
  # Ollama (LLM Local — GRATUITO)
  # ═══════════════════════════════════════════
  ollama:
    image: ollama/ollama:latest
    container_name: nexusai-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_models:/root/.ollama
    # Para GPU NVIDIA, descomenta:
    # deploy:
    #   resources:
    #     reservations:
    #       devices:
    #         - driver: nvidia
    #           count: all
    #           capabilities: [gpu]

volumes:
  pgdata:
  mongodata:
  ollama_models:
```

### 2. Levantar la Infraestructura

Ejecuta estos comandos en la raíz del proyecto:

```bash
# 1. Levantar todos los servicios
docker compose up -d

# 2. Verificar que están corriendo
docker compose ps

# 3. Descargar el modelo Llama 3 en Ollama (primera vez ~4GB)
docker exec -it nexusai-ollama ollama pull llama3

# 4. (Opcional) Descargar modelo de embeddings para RAG
docker exec -it nexusai-ollama ollama pull nomic-embed-text

# 5. Verificar que Ollama responde
curl http://localhost:11434/api/tags
```

> 💡 **Nota:** La primera descarga de Llama 3 puede tardar varios minutos dependiendo de tu conexión. Solo se descarga una vez gracias al volumen `ollama_models`.

---

### 3. Dependencias del Proyecto — `pom.xml`

Si usas Spring Initializr (https://start.spring.io), selecciona:
- Spring Boot **4.0.x**
- Java **21**
- Dependencies: Web, Thymeleaf, WebSocket, Spring Data JPA, PostgreSQL Driver, Spring Data MongoDB, Validation

Luego agrega manualmente las dependencias de Spring AI. Tu `pom.xml` debe incluir:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.6</version>
        <relativeTo/>
    </parent>

    <groupId>com.nexusai</groupId>
    <artifactId>nexus-ai</artifactId>
    <version>1.0.0</version>
    <name>NexusAI</name>
    <description>AI-Powered Support Platform with Spring AI</description>

    <properties>
        <java.version>21</java.version>
        <spring-ai.version>1.0.0</spring-ai.version>
    </properties>

    <dependencies>
        <!-- ═══════════════════════════════════ -->
        <!-- Core Spring Boot                    -->
        <!-- ═══════════════════════════════════ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- ═══════════════════════════════════ -->
        <!-- Persistence                         -->
        <!-- ═══════════════════════════════════ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>

        <!-- ═══════════════════════════════════ -->
        <!-- Spring AI — Ollama (Principal)      -->
        <!-- ═══════════════════════════════════ -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
        </dependency>

        <!-- ═══════════════════════════════════ -->
        <!-- Spring AI — OpenAI (Secundario)     -->
        <!-- Descomenta si tienes API Key         -->
        <!-- ═══════════════════════════════════ -->
        <!--
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        </dependency>
        -->

        <!-- ═══════════════════════════════════ -->
        <!-- Spring AI — PGVector (RAG)          -->
        <!-- ═══════════════════════════════════ -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
        </dependency>

        <!-- ═══════════════════════════════════ -->
        <!-- Spring AI — MCP (Fase 9)            -->
        <!-- ═══════════════════════════════════ -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
        </dependency>

        <!-- ═══════════════════════════════════ -->
        <!-- Observability (Fase 10)             -->
        <!-- ═══════════════════════════════════ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-otel</artifactId>
        </dependency>

        <!-- ═══════════════════════════════════ -->
        <!-- Testing                             -->
        <!-- ═══════════════════════════════════ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- ═══════════════════════════════════════ -->
    <!-- Spring AI BOM (Bill of Materials)       -->
    <!-- Gestiona las versiones automáticamente  -->
    <!-- ═══════════════════════════════════════ -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

---

### 4. Configuración — `application.yml`

Crea el archivo `src/main/resources/application.yml`:

```yaml
# ═══════════════════════════════════════════════════════
# NexusAI — Application Configuration
# ═══════════════════════════════════════════════════════

spring:
  application:
    name: nexus-ai

  # ─── PostgreSQL (datos transaccionales + PGVector) ───
  datasource:
    url: jdbc:postgresql://localhost:5432/nexusai
    username: nexusai
    password: nexusai_secret
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  # ─── MongoDB (historial de chat — opcional) ──────────
  data:
    mongodb:
      uri: mongodb://localhost:27017/nexusai_chat

  # ─── Thymeleaf ───────────────────────────────────────
  thymeleaf:
    cache: false  # Desactivar caché en desarrollo
    prefix: classpath:/templates/
    suffix: .html

  # ═══════════════════════════════════════════════════════
  # Spring AI Configuration
  # ═══════════════════════════════════════════════════════

  ai:
    # ─── Ollama (Proveedor Principal — Local/Gratis) ───
    ollama:
      base-url: http://localhost:11434
      chat:
        model: llama3
        options:
          temperature: 0.7
          num-predict: 2048
      embedding:
        model: nomic-embed-text

    # ─── OpenAI (Proveedor Secundario — Opcional) ──────
    # Descomenta y configura si tienes API Key:
    # openai:
    #   api-key: ${OPENAI_API_KEY}
    #   chat:
    #     options:
    #       model: gpt-4.1-mini
    #       temperature: 0.7
    #   embedding:
    #     options:
    #       model: text-embedding-3-small

    # ─── PGVector (Vector Store para RAG) ──────────────
    vectorstore:
      pgvector:
        initialize-schema: true
        index-type: hnsw
        distance-type: cosine_distance
        dimensions: 768  # nomic-embed-text = 768 dims

# ─── Server ─────────────────────────────────────────────
server:
  port: 8080

# ─── Logging ─────────────────────────────────────────────
logging:
  level:
    com.nexusai: DEBUG
    org.springframework.ai: DEBUG
```

---

### 5. Estructura de Paquetes

Crea la siguiente estructura de paquetes en `src/main/java/com/nexusai/`:

```
src/main/java/com/nexusai/
├── NexusAiApplication.java           ← Main class
│
├── ai/                                ← TODO lo relacionado con IA
│   ├── config/                        ← Beans de configuración AI
│   │   └── AiConfig.java
│   ├── prompts/                       ← Gestión de prompt templates
│   ├── rag/                           ← Ingesta y retrieval de docs
│   ├── tools/                         ← Function calling (@Tool)
│   ├── memory/                        ← Configuración de memoria
│   ├── advisors/                      ← Advisors personalizados
│   ├── models/                        ← DTOs structured output (records)
│   ├── mcp/                           ← MCP server/client
│   └── orchestration/                 ← Interface AiAssistant + impls
│       ├── AiAssistant.java           ← Interface de abstracción
│       └── OllamaAssistant.java       ← Implementación Ollama
│
├── application/                       ← Lógica de negocio
│   └── service/
│       ├── TicketService.java
│       └── KnowledgeBaseService.java
│
├── domain/                            ← Entidades de dominio
│   └── model/
│       ├── Ticket.java                ← Entidad JPA (PostgreSQL)
│       ├── ChatMessage.java           ← Documento MongoDB
│       └── KnowledgeArticle.java      ← Entidad JPA
│
├── infrastructure/                    ← Acceso a datos
│   └── repository/
│       ├── TicketRepository.java      ← Spring Data JPA
│       ├── ChatMessageRepository.java ← Spring Data MongoDB
│       └── KnowledgeArticleRepository.java
│
└── api/                               ← Capa de presentación
    ├── rest/                          ← REST Controllers (@RestController)
    │   └── TicketRestController.java
    ├── ws/                            ← WebSocket Controllers
    │   └── ChatSocketController.java
    └── ui/                            ← Thymeleaf Controllers (@Controller)
        ├── DashboardController.java
        └── ChatUIController.java
```

---

## 📝 Actividades — Tu Turno

### Actividad 1.1 — Levantar la Infraestructura Docker

1. Crea el archivo `docker-compose.yml` en la raíz de tu proyecto con el contenido proporcionado.
2. Ejecuta `docker compose up -d` y verifica que los 3 servicios están corriendo.
3. Descarga el modelo Llama 3 con `docker exec -it nexusai-ollama ollama pull llama3`.
4. Verifica la conexión a PostgreSQL con tu IDE o con:
   ```bash
   docker exec -it nexusai-postgres psql -U nexusai -d nexusai -c "SELECT 1;"
   ```

### Actividad 1.2 — Crear el Proyecto Spring Boot

1. Genera el proyecto en [Spring Initializr](https://start.spring.io/) o crea manualmente el `pom.xml`.
2. Copia el `application.yml` proporcionado.
3. Crea **toda** la estructura de paquetes mostrada arriba (los paquetes vacíos se llenarán en fases posteriores).

### Actividad 1.3 — Clase Principal y Entidades Base

La clase principal ya viene generada:

```java
package com.nexusai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NexusAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusAiApplication.class, args);
    }
}
```

Ahora crea las entidades de dominio:

**`domain/model/Ticket.java`** — Entidad JPA para tickets de soporte:

```java
package com.nexusai.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    private TicketPriority priority = TicketPriority.MEDIUM;

    private String assignedTo;
    private String createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ─── Enums ─────────────────────────────────────
    public enum TicketStatus { OPEN, IN_PROGRESS, RESOLVED, CLOSED }
    public enum TicketPriority { LOW, MEDIUM, HIGH, CRITICAL }

    // ─── Constructors ──────────────────────────────
    public Ticket() {}

    public Ticket(String title, String description, String createdBy) {
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
    }

    // ─── Getters & Setters ─────────────────────────
    // TODO: Genera todos los getters y setters
    //       Usa tu IDE (Alt+Insert en IntelliJ) o escríbelos manualmente.
    //       Incluye: getId, setId, getTitle, setTitle, getDescription,
    //       setDescription, getStatus, setStatus, getPriority, setPriority,
    //       getAssignedTo, setAssignedTo, getCreatedBy, setCreatedBy,
    //       getCreatedAt, setCreatedAt, getUpdatedAt, setUpdatedAt
}
```

**`domain/model/ChatMessage.java`** — Documento MongoDB para historial de chat:

```java
package com.nexusai.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;
    private String sessionId;
    private String sender;       // "user", "assistant", "system"
    private String content;
    private String role;         // Para Spring AI: USER, ASSISTANT, SYSTEM
    private LocalDateTime timestamp = LocalDateTime.now();

    // ─── Constructors ──────────────────────────────
    public ChatMessage() {}

    public ChatMessage(String sessionId, String sender, String content, String role) {
        this.sessionId = sessionId;
        this.sender = sender;
        this.content = content;
        this.role = role;
    }

    // ─── Getters & Setters ─────────────────────────
    // TODO: Genera todos los getters y setters para cada campo.
}
```

### Actividad 1.4 — Repositorios Base

Crea los repositorios en `infrastructure/repository/`:

```java
// TODO: Crea TicketRepository.java
//
// Requisitos:
// 1. Debe extender JpaRepository<Ticket, Long>
// 2. Debe incluir estos query methods:
//    - List<Ticket> findByStatus(Ticket.TicketStatus status);
//    - List<Ticket> findByPriority(Ticket.TicketPriority priority);
//    - List<Ticket> findByCreatedByOrderByCreatedAtDesc(String createdBy);
//    - long countByStatus(Ticket.TicketStatus status);
```

```java
// TODO: Crea ChatMessageRepository.java
//
// Requisitos:
// 1. Debe extender MongoRepository<ChatMessage, String>
// 2. Debe incluir estos query methods:
//    - List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
//    - List<ChatMessage> findTop20BySessionIdOrderByTimestampDesc(String sessionId);
//    - void deleteBySessionId(String sessionId);
```

### Actividad 1.5 — Verificación Inicial

1. Ejecuta la aplicación: `mvn spring-boot:run`
2. Verifica que arranca sin errores.
3. Verifica en los logs que:
   - Se conecta a PostgreSQL ✅
   - Se conecta a MongoDB ✅
   - Las tablas JPA se crean automáticamente ✅

> 💡 **Tip:** Si Ollama no está corriendo, la app puede fallar al intentar conectar. Asegúrate de que `docker compose ps` muestra todos los servicios como `running`.

---

## ✅ Criterios de Evaluación — Fase 1

| Criterio | Cumple |
|----------|--------|
| Docker Compose levanta PostgreSQL, MongoDB y Ollama correctamente | ☐ |
| El modelo Llama 3 está descargado en Ollama | ☐ |
| El `pom.xml` incluye todas las dependencias de Spring AI | ☐ |
| La estructura de paquetes sigue la arquitectura propuesta | ☐ |
| Las entidades `Ticket` y `ChatMessage` están completas con getters/setters | ☐ |
| Los repositorios `TicketRepository` y `ChatMessageRepository` están creados | ☐ |
| La aplicación arranca sin errores y se conecta a las bases de datos | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿Por qué usamos PostgreSQL con PGVector para RAG en lugar de una base de datos vectorial dedicada como Pinecone o Weaviate? ¿Cuáles son las ventajas y desventajas?
2. ¿Cuál es la diferencia entre usar `ddl-auto: update` y herramientas de migración como Flyway o Liquibase? ¿Cuál usarías en producción?
3. Observa la estructura de paquetes: ¿por qué separamos `ai/` de `application/` y `domain/`? ¿Qué principio de diseño estamos aplicando?

---

> ➡️ **Siguiente:** [Fase 2 — ChatClient y Structured Outputs](Lab5-Fase2-ChatClient-StructuredOutputs.md)
