# 🧪 FASE 3 — Prompt Engineering Profesional

**Módulo 6.1 · Semana 3 · Proyecto NexusAI 🤖**  
**Duración estimada:** 30 minutos  
**Prerrequisito:** Fase 2 completada  
**Objetivo:** Externalizar prompts en archivos `.st` (StringTemplate), parametrizarlos con variables dinámicas, y entender por qué los prompts inline son un anti-patrón en sistemas enterprise.

---

## 📖 Concepto Clave: Prompts como Código

En la Fase 2 escribiste prompts directamente en el código Java:

```java
// ❌ Anti-patrón: Prompt inline, no versionable, no testeable
return chatClient.prompt()
    .user("Analyze this support ticket: Title: " + title + " Description: " + desc)
    .call()
    .entity(TicketAnalysis.class);
```

**Problemas con este enfoque:**
- Los prompts **no se pueden versionar** independientemente del código.
- No puedes hacer **A/B testing** de prompts sin recompilar.
- Los prompts se mezclan con la lógica Java (violación de SRP).
- El equipo de producto no puede **ajustar prompts** sin un desarrollador.
- No puedes **reutilizar** prompts entre diferentes servicios.

### La solución: Prompt Templates

Spring AI usa **StringTemplate** (archivos `.st`) como motor de plantillas para prompts. Funcionan como las vistas de Thymeleaf, pero para instrucciones de IA:

```
resources/prompts/
├── ticket-analysis.st
├── support-response.st
├── escalation-check.st
└── knowledge-search.st
```

Cada archivo es un prompt parametrizable que puedes modificar, versionar y testear sin tocar Java.

---

## 🛠️ Código Base Proporcionado

### 1. Prompt Template de Ejemplo — `resources/prompts/ticket-analysis.st`

Crea la carpeta `src/main/resources/prompts/` y el archivo:

```
You are an expert technical support analyst at NexusTech Solutions.

Analyze the following support ticket and provide a structured assessment.

## Ticket Information
- **Title:** {title}
- **Description:** {description}
- **Submitted by:** {createdBy}

## Analysis Instructions
1. Determine the priority level: LOW, MEDIUM, HIGH, or CRITICAL.
2. Categorize the issue (Infrastructure, Software, Network, Security, User Error, etc.).
3. Provide a concise one-line summary.
4. Suggest 2-4 concrete actions to resolve the issue.
5. Estimate resolution time.
6. Rate your confidence from 0.0 to 1.0.

Consider these priority guidelines:
- CRITICAL: System is down, data loss, security breach
- HIGH: Major functionality affected, many users impacted
- MEDIUM: Functionality degraded, workaround exists
- LOW: Minor issue, cosmetic, documentation

Respond ONLY with the requested structured format.
```

> 💡 **Observa las variables:** `{title}`, `{description}`, `{createdBy}` son placeholders que Spring AI reemplaza con valores reales en runtime.

---

### 2. Servicio de Prompts — `ai/prompts/PromptService.java`

```java
package com.nexusai.ai.prompts;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Centralized service for loading and rendering prompt templates.
 * 
 * All prompts live in resources/prompts/*.st files.
 * This service loads them and renders with runtime variables.
 */
@Service
public class PromptService {

    // Spring inyecta el archivo como Resource gracias a @Value
    @Value("classpath:prompts/ticket-analysis.st")
    private Resource ticketAnalysisPrompt;

    /**
     * Renders the ticket analysis prompt with the given variables.
     *
     * @param title       The ticket title
     * @param description The ticket description  
     * @param createdBy   Who created the ticket
     * @return A rendered Prompt ready to send to the LLM
     */
    public Prompt buildTicketAnalysisPrompt(String title, String description, String createdBy) {
        PromptTemplate template = new PromptTemplate(ticketAnalysisPrompt);
        return template.create(Map.of(
            "title", title,
            "description", description,
            "createdBy", createdBy
        ));
    }
}
```

---

## 📝 Actividades — Tu Turno

### Actividad 3.1 — Crear el Prompt Template de Soporte

Crea el archivo `src/main/resources/prompts/support-response.st`:

```
// TODO: Diseña un prompt template para respuestas de soporte técnico.
//
// Requisitos:
// 1. Debe tener al menos estas variables: {question}, {userRole}, {productVersion}
// 2. Debe instruir al LLM a responder de forma profesional.
// 3. Debe pedir que la respuesta incluya:
//    - Una respuesta principal clara
//    - El tono apropiado (técnico para devs, amigable para usuarios finales)
//    - Temas relacionados que podrían ser útiles
//    - Si necesita escalación humana
// 4. Incluye "guardrails" — instrucciones al LLM para que NO invente información
//    y admita cuando no sabe algo.
//
// Tip: Un buen prompt tiene:
//   - Rol claro ("You are...")
//   - Contexto específico
//   - Instrucciones paso a paso
//   - Formato de salida deseado
//   - Restricciones y guardrails
```

### Actividad 3.2 — Crear el Prompt Template de Escalación

Crea el archivo `src/main/resources/prompts/escalation-check.st`:

```
// TODO: Diseña un prompt que evalúe si un ticket debe escalarse.
//
// Requisitos:
// 1. Variables: {title}, {description}, {currentPriority}, {timeOpen}
// 2. Debe tener criterios claros de escalación:
//    - Tickets CRITICAL sin resolver por más de 1 hora
//    - Tickets que mencionan seguridad o pérdida de datos
//    - Tickets con más de 3 interacciones sin resolución
// 3. Debe pedir que el LLM justifique su decisión.
// 4. El output debe mapear al record EscalationDecision de la Fase 2.
```

### Actividad 3.3 — Registrar tus Templates en `PromptService`

Modifica `PromptService.java` para cargar tus nuevos templates:

```java
// TODO: Agrega a PromptService los siguientes métodos:
//
// 1. Campo:
//    @Value("classpath:prompts/support-response.st")
//    private Resource supportResponsePrompt;
//
// 2. Método buildSupportResponsePrompt(String question, String userRole, String productVersion)
//    que renderice el template con esas variables.
//
// 3. Campo y método similar para escalation-check.st
//
// Tip: Sigue el mismo patrón de buildTicketAnalysisPrompt()
```

### Actividad 3.4 — Refactorizar `OllamaAssistant` para usar Templates

Ahora que tienes templates externalizados, refactoriza tu implementación:

```java
// TODO: Modifica OllamaAssistant para usar PromptService en vez de prompts inline.
//
// Antes (Fase 2):
//   chatClient.prompt().user("Analyze: " + title + " - " + desc)...
//
// Después (Fase 3):
//   Prompt prompt = promptService.buildTicketAnalysisPrompt(title, desc, "user");
//   chatClient.prompt(prompt).call().entity(TicketAnalysis.class);
//
// Pasos:
// 1. Inyecta PromptService en OllamaAssistant (constructor injection)
// 2. En analyzeTicket(), usa promptService.buildTicketAnalysisPrompt()
// 3. En answerQuestion(), usa promptService.buildSupportResponsePrompt()
// 4. Verifica que los endpoints REST siguen funcionando correctamente
```

### Actividad 3.5 — Verificación

Prueba que tus prompts externalizados funcionan correctamente:

```bash
# Debe retornar la misma calidad de respuesta (o mejor) que la Fase 2
curl -X POST "http://localhost:8080/api/ai/analyze-ticket" \
  -d "title=Database%20connection%20pool%20exhausted" \
  -d "description=Production%20PostgreSQL%20connections%20maxed%20out%20during%20peak%20hours.%20Users%20get%20timeout%20errors."
```

**Bonus:** Modifica el texto del `.st` SIN recompilar Java. Reinicia la app y verifica que el nuevo prompt se usa. Esto demuestra la flexibilidad de externalizar prompts.

---

## 🧠 Tip Profesional: Prompt Engineering Patterns

### Pattern 1: Chain of Thought
```
Think step by step before giving your final answer:
1. First, identify the core problem...
2. Then, consider possible causes...
3. Finally, recommend actions...
```

### Pattern 2: Few-Shot Examples
```
Here are examples of good ticket analyses:

Example 1:
Ticket: "Can't login"
Analysis: { priority: "HIGH", category: "Authentication", ... }

Example 2:
Ticket: "Button color is wrong"  
Analysis: { priority: "LOW", category: "UI", ... }

Now analyze this ticket: {description}
```

### Pattern 3: Role + Context + Task + Format
```
ROLE: You are a senior support engineer with 10 years of experience.
CONTEXT: Our product is a cloud-based CRM for enterprise clients.
TASK: Analyze the support ticket below.
FORMAT: Respond using this exact JSON structure: ...
```

---

## ✅ Criterios de Evaluación — Fase 3

| Criterio | Cumple |
|----------|--------|
| Existe al menos 3 archivos `.st` en `resources/prompts/` | ☐ |
| Los templates usan variables con la sintaxis `{variable}` | ☐ |
| `PromptService` carga y renderiza todos los templates | ☐ |
| `OllamaAssistant` fue refactorizado para usar `PromptService` | ☐ |
| Los prompts incluyen guardrails (instrucciones de seguridad) | ☐ |
| Los endpoints REST siguen funcionando con los templates externalizados | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿Qué ventaja tiene que los archivos `.st` estén en `resources/prompts/` en lugar de hardcoded en Java? Piensa en: versionado git, colaboración con el equipo de producto, A/B testing.
2. ¿Cómo implementarías un sistema de **versionamiento de prompts** para poder comparar la calidad de v1 vs v2 de un prompt en producción?
3. ¿Por qué es importante incluir "guardrails" en los prompts (instrucciones para que el LLM no invente datos)? Investiga qué son las **alucinaciones** de los LLMs.

---

> ⬅️ **Anterior:** [Fase 2 — ChatClient y Structured Outputs](Lab5-Fase2-ChatClient-StructuredOutputs.md)  
> ➡️ **Siguiente:** [Fase 4 — Chat Web con Thymeleaf y WebSockets](Lab5-Fase4-Chat-Thymeleaf-WebSockets.md)
