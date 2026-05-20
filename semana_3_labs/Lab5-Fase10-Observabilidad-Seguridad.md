# 🧪 FASE 10 — Observabilidad, Seguridad y Testing

**Módulo 6.1 · Semana 3 · Proyecto NexusAI 🤖**  
**Duración estimada:** 45 minutos  
**Prerrequisito:** Fase 9 completada  
**Objetivo:** Implementar observabilidad para monitorear prompts, latencia y tokens; agregar seguridad con masking y guardrails; y escribir tests determinísticos para sistemas de IA.

---

## 📖 Concepto Clave: ¿Por qué Observabilidad en IA?

Sin observabilidad, tu app de IA es una **caja negra**. Necesitas saber:

| Métrica | Por qué importa |
|---------|-----------------|
| **Latencia** | ¿El LLM tarda 2s o 20s? |
| **Tokens** | ¿Cuánto cuesta cada llamada? |
| **Prompts** | ¿Qué se le envió al modelo? |
| **Tool calls** | ¿Qué herramientas invocó y con qué parámetros? |
| **Errores** | ¿Cuántas llamadas fallan? |
| **Alucinaciones** | ¿La respuesta es coherente con el contexto? |

### Seguridad: Lo que NUNCA debes enviar al LLM

- 🔐 API keys, tokens, passwords
- 👤 PII (nombres reales, emails, SSN, tarjetas de crédito)
- 💰 Datos financieros completos
- 🏥 Datos de salud (HIPAA)

Debes implementar **masking** antes de enviar al LLM y **guardrails** para validar las respuestas.

---

## 🛠️ Código Base Proporcionado

### 1. Métricas con Actuator — `application.yml` (agregar)

```yaml
# ─── Actuator + Observability ─────────────────────
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  metrics:
    tags:
      application: nexus-ai
```

### 2. Interceptor de Métricas AI — `ai/advisors/MetricsAdvisor.java`

```java
package com.nexusai.ai.advisors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.stereotype.Component;

/**
 * Advisor that records metrics for every AI interaction.
 * Integrates with Micrometer for Prometheus/Grafana dashboards.
 */
@Component
public class MetricsAdvisor implements CallAroundAdvisor {

    private final Timer aiLatencyTimer;
    private final Counter aiCallCounter;
    private final Counter aiErrorCounter;

    public MetricsAdvisor(MeterRegistry meterRegistry) {
        this.aiLatencyTimer = Timer.builder("ai.call.duration")
                .description("Time taken for AI calls")
                .register(meterRegistry);
        this.aiCallCounter = Counter.builder("ai.call.total")
                .description("Total number of AI calls")
                .register(meterRegistry);
        this.aiErrorCounter = Counter.builder("ai.call.errors")
                .description("Total number of AI call errors")
                .register(meterRegistry);
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        aiCallCounter.increment();
        return aiLatencyTimer.record(() -> {
            try {
                return chain.nextAroundCall(request);
            } catch (Exception e) {
                aiErrorCounter.increment();
                throw e;
            }
        });
    }

    @Override
    public String getName() { return "MetricsAdvisor"; }

    @Override
    public int getOrder() { return -10; } // Very first
}
```

### 3. Utilidad de Masking — `ai/advisors/PiiMaskingUtil.java`

```java
package com.nexusai.ai.advisors;

import java.util.regex.Pattern;

/**
 * Utility to mask PII (Personally Identifiable Information)
 * before sending data to external LLM providers.
 */
public class PiiMaskingUtil {

    // Email pattern: user@domain.com → u***@d***.com
    private static final Pattern EMAIL = 
        Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    // Credit card: 1234-5678-9012-3456 → ****-****-****-3456
    private static final Pattern CREDIT_CARD = 
        Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b");

    // SSN: 123-45-6789 → ***-**-6789
    private static final Pattern SSN = 
        Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    // Phone: various formats
    private static final Pattern PHONE = 
        Pattern.compile("\\b\\+?\\d{1,3}[- ]?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}\\b");

    public static String maskPii(String text) {
        if (text == null) return null;
        String masked = text;
        masked = EMAIL.matcher(masked).replaceAll("[EMAIL_REDACTED]");
        masked = CREDIT_CARD.matcher(masked).replaceAll("[CARD_REDACTED]");
        masked = SSN.matcher(masked).replaceAll("[SSN_REDACTED]");
        masked = PHONE.matcher(masked).replaceAll("[PHONE_REDACTED]");
        return masked;
    }
}
```

---

## 📝 Actividades — Tu Turno

### Actividad 10.1 — Integrar Métricas en la Cadena de Advisors

```java
// TODO: Modifica OllamaAssistant para incluir MetricsAdvisor en la cadena:
//
// 1. Inyecta MetricsAdvisor en el constructor
// 2. Agrégalo a la cadena de advisors (debe ser el primero — getOrder = -10):
//
//    .advisors(
//        metricsAdvisor,                               // Métricas (primero)
//        new ContentModerationAdvisor(),                // Seguridad (segundo)
//        new LoggingAdvisor(),                          // Logging
//        new MessageChatMemoryAdvisor(chatMemory),      // Memoria
//        new QuestionAnswerAdvisor(vectorStore)          // RAG
//    )
//
// 3. Verifica las métricas en: http://localhost:8080/actuator/metrics/ai.call.duration
```

### Actividad 10.2 — Implementar Masking en el Advisor de Moderación

```java
// TODO: Modifica ContentModerationAdvisor (Fase 8) para usar PiiMaskingUtil:
//
// En aroundCall(), ANTES de pasar el request al LLM:
//   String userText = request.userText();
//   String maskedText = PiiMaskingUtil.maskPii(userText);
//   // Si se detectó PII (maskedText != userText), loguea una advertencia
//   // Crea un nuevo request con el texto enmascarado
//
// Esto asegura que nunca se envíen datos sensibles a APIs externas.
```

### Actividad 10.3 — Tests Determinísticos para IA

Los tests de IA **NO deben probar texto exacto**. Prueba **estructura, esquema e intención**:

```java
// TODO: Crea test/java/com/nexusai/ai/AiAssistantTest.java
//
// @SpringBootTest
// class AiAssistantTest {
//
//     @Autowired AiAssistant aiAssistant;
//
//     @Test
//     void analyzeTicket_shouldReturnValidStructure() {
//         TicketAnalysis result = aiAssistant.analyzeTicket(
//             "Server is down",
//             "Production web server returning 503 errors since 3am"
//         );
//
//         // ✅ DO: Test structure, not exact text
//         assertNotNull(result);
//         assertNotNull(result.priority());
//         assertTrue(List.of("LOW","MEDIUM","HIGH","CRITICAL").contains(result.priority()));
//         assertNotNull(result.category());
//         assertFalse(result.suggestedActions().isEmpty());
//         assertTrue(result.confidenceScore() >= 0.0 && result.confidenceScore() <= 1.0);
//
//         // ❌ DON'T: Test exact text (non-deterministic)
//         // assertEquals("CRITICAL", result.priority()); // Might vary
//         // assertEquals("The server...", result.summary()); // NEVER do this
//     }
//
//     @Test
//     void analyzeTicket_criticalIssue_shouldHaveHighPriorityOrCritical() {
//         TicketAnalysis result = aiAssistant.analyzeTicket(
//             "DATA BREACH - Customer records exposed",
//             "SQL injection attack detected. Customer PII may be compromised."
//         );
//
//         // Test INTENTION — a security breach should be HIGH or CRITICAL
//         assertTrue(
//             result.priority().equals("HIGH") || result.priority().equals("CRITICAL"),
//             "Security breach should be HIGH or CRITICAL priority"
//         );
//         assertTrue(result.needsEscalation() || result.confidenceScore() > 0.7);
//     }
// }
```

```java
// TODO: Crea test para PiiMaskingUtil:
//
// class PiiMaskingUtilTest {
//
//     @Test
//     void shouldMaskEmails() {
//         String input = "Contact john@example.com for help";
//         String result = PiiMaskingUtil.maskPii(input);
//         assertFalse(result.contains("john@example.com"));
//         assertTrue(result.contains("[EMAIL_REDACTED]"));
//     }
//
//     @Test
//     void shouldMaskCreditCards() {
//         String input = "Card number is 4532-1234-5678-9012";
//         String result = PiiMaskingUtil.maskPii(input);
//         assertFalse(result.contains("4532"));
//         assertTrue(result.contains("[CARD_REDACTED]"));
//     }
//
//     // TODO: Agrega tests para SSN, teléfono, y texto sin PII
// }
```

### Actividad 10.4 — Dashboard de Observabilidad (Thymeleaf)

```html
<!-- TODO: Crea templates/admin/observability.html -->
<!--                                               -->
<!-- Dashboard que muestre:                         -->
<!-- 1. Métricas de AI:                            -->
<!--    - Total de llamadas (ai.call.total)        -->
<!--    - Latencia promedio (ai.call.duration)     -->
<!--    - Errores (ai.call.errors)                 -->
<!-- 2. Últimas 10 interacciones del LoggingAdvisor -->
<!-- 3. Estado de los servicios (Ollama, PG, Mongo) -->
<!--                                               -->
<!-- Crea api/ui/ObservabilityController.java       -->
<!-- que consulte los datos de Actuator/Micrometer  -->
<!-- y los pase al Model de Thymeleaf               -->
```

### Actividad 10.5 — Verificación Final del Sistema Completo

Ejecuta una verificación end-to-end de todas las fases:

```bash
# 1. Verificar infraestructura
docker compose ps  # Todos los servicios UP

# 2. Verificar métricas
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics/ai.call.total

# 3. Verificar API REST
curl "http://localhost:8080/api/ai/chat?message=Hello"
curl -X POST "http://localhost:8080/api/ai/analyze-ticket" -d "title=Test&description=Test"

# 4. Verificar Knowledge Base
curl "http://localhost:8080/api/knowledge/search?query=password+reset"

# 5. Verificar SSE Streaming
curl -N "http://localhost:8080/api/ai/stream?prompt=Hello"

# 6. Verificar chat web
# Abrir http://localhost:8080/chat y probar conversación completa

# 7. Ejecutar tests
mvn test
```

---

## ✅ Criterios de Evaluación — Fase 10 (Final)

| Criterio | Cumple |
|----------|--------|
| `MetricsAdvisor` registra latencia, conteo y errores en Micrometer | ☐ |
| `PiiMaskingUtil` enmascara emails, tarjetas, SSN y teléfonos | ☐ |
| Masking integrado en la cadena de advisors | ☐ |
| Al menos 3 tests determinísticos para AiAssistant (estructura, no texto) | ☐ |
| Tests para PiiMaskingUtil con cobertura de todos los patrones | ☐ |
| Dashboard de observabilidad en Thymeleaf funcional | ☐ |
| `mvn test` ejecuta exitosamente | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿Por qué es un error probar `assertEquals("CRITICAL", result.priority())` en tests de IA? ¿Qué hace que las respuestas de un LLM sean no-determinísticas?
2. ¿Qué regulaciones (GDPR, HIPAA, CCPA) aplican cuando envías datos de usuarios a un proveedor de LLM como OpenAI? ¿Cómo mitiga el masking este riesgo?
3. Si tu app hace 1000 llamadas al LLM por hora, ¿qué métricas te ayudarían a decidir si necesitas cambiar de modelo o optimizar los prompts?

---

## 🏆 Entrega Final — Checklist Completo

Antes de entregar, verifica que tu proyecto cumple con **todos** estos criterios:

### Arquitectura
- [ ] Interface `AiAssistant` desacoplada del modelo
- [ ] Código en inglés, documentación en español
- [ ] Estructura de paquetes organizada (ai/, application/, domain/, etc.)

### Core AI
- [ ] `ChatClient` configurado con `ChatModel` (no implementación concreta)
- [ ] Structured outputs con records y `.entity()`
- [ ] Prompt templates en archivos `.st` externalizados
- [ ] Al menos 3 prompts diferentes

### Frontend
- [ ] Chat con Thymeleaf + WebSockets funcional
- [ ] Streaming tipo ChatGPT con SSE
- [ ] Toggle entre modo normal y streaming
- [ ] Indicador de "IA escribiendo..."
- [ ] Diseño profesional con dark mode

### Enterprise Features
- [ ] Al menos 4 tools con `@Tool` funcionales
- [ ] RAG con PGVector operativo (al menos 5 documentos)
- [ ] Memoria conversacional con `MessageChatMemoryAdvisor`
- [ ] MCP Server expuesto con herramientas
- [ ] Cadena de advisors: Metrics → Moderation → Logging → Memory → RAG

### Calidad
- [ ] Tests determinísticos (estructura, no texto exacto)
- [ ] PII masking implementado
- [ ] Dashboard de observabilidad
- [ ] `docker compose up -d` levanta toda la infra
- [ ] `mvn test` pasa exitosamente
- [ ] `mvn spring-boot:run` inicia sin errores

---

## 📚 Recursos Adicionales

| Recurso | URL |
|---------|-----|
| Spring AI Reference | https://docs.spring.io/spring-ai/reference/ |
| Spring AI GitHub | https://github.com/spring-projects/spring-ai |
| Ollama Models | https://ollama.ai/library |
| PGVector Docs | https://github.com/pgvector/pgvector |
| MCP Specification | https://modelcontextprotocol.io |
| OpenTelemetry Java | https://opentelemetry.io/docs/languages/java/ |
| Micrometer Docs | https://micrometer.io/docs |

---

> ⬅️ **Anterior:** [Fase 9 — MCP](Lab5-Fase9-MCP.md)  
> 🏠 **Inicio:** [Introducción al Laboratorio](Lab5-Fase0-Introduccion.md)

---

> 🎉 **¡Felicitaciones!** Has completado el laboratorio NexusAI.  
> Has construido una aplicación AI enterprise con arquitectura desacoplada,  
> streaming, RAG, tools, memoria, MCP y observabilidad.  
> Esto no es una demo — es la base de un **sistema AI serio**.
