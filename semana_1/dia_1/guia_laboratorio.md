# 🚀 Laboratorio Día 1: Inmersión en el Ecosistema Spring Boot

Este laboratorio está diseñado para que los estudiantes comprendan los conceptos fundamentales de Spring Boot: **Beans**, **Inyección de Dependencias**, **Estereotipos** y la estructura base de una aplicación web.

## 🎯 Objetivos de Aprendizaje
1.  Inicializar un proyecto profesional con **Spring Initializr**.
2.  Comprender el ciclo de vida de los **Beans** gestionados por Spring.
3.  Implementar la comunicación entre capas usando **Inyección por Constructor**.
4.  Exponer el primer **Endpoint REST** funcional.

---

## 🛠️ Parte 1: Creación del Proyecto
1.  Ir a [start.spring.io](https://start.spring.io).
2.  Configurar:
    *   **Project:** Maven
    *   **Language:** Java (Versión 17 o 21)
    *   **Spring Boot:** 3.x.x
    *   **Dependencies:** `Spring Web`, `Lombok`, `Spring Boot DevTools`.
3.  Descargar, descomprimir y abrir en el IDE (IntelliJ/VS Code).

---

## 🏗️ Parte 2: El primer "Bean" (Capa de Servicio)
Vamos a crear un componente que se encargue de la lógica de "Saludos".

1.  Crear el paquete `com.riwi.intro.services`.
2.  Crear la clase `GreetingService.java`:

```java
package com.riwi.intro.services;

import org.springframework.stereotype.Service;

@Service // Estereotipo que indica que es un componente de lógica de negocio
public class GreetingService {
    public String getPersonalizedGreeting(String name) {
        return "¡Hola, " + name + "! Bienvenido al entrenamiento de Spring Boot.";
    }
}
```

---

## 🎮 Parte 3: Inyección de Dependencias (Capa de Controlador)
Ahora usaremos ese servicio en un controlador para exponerlo a la web.

1.  Crear el paquete `com.riwi.intro.controllers`.
2.  Crear la clase `GreetingController.java`:

```java
package com.riwi.intro.controllers;

import com.riwi.intro.services.GreetingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private final GreetingService greetingService;

    // INYECCIÓN POR CONSTRUCTOR: La forma recomendada y profesional
    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/greet")
    public String greet(@RequestParam(defaultValue = "Coder") String name) {
        return greetingService.getPersonalizedGreeting(name);
    }
}
```

---

## ⚙️ Parte 4: Definición Manual de Beans (@Configuration)
A veces necesitamos registrar clases que no son nuestras (ej. librerías externas) como Beans.

1.  Crear el paquete `com.riwi.intro.config`.
2.  Crear la clase `AppConfig.java`:

```java
package com.riwi.intro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDateTime;

@Configuration
public class AppConfig {

    @Bean // Definición manual de un Bean que no usa estereotipos
    public LocalDateTime serverStartTime() {
        return LocalDateTime.now();
    }
}
```

---

## 🧪 Parte 5: Desafío para el Estudiante
**Reto "TaskCount":**
1.  Crea un nuevo servicio llamado `TaskService` anotado con `@Service`.
2.  Dentro, define una lista de tareas (`List<String>`).
3.  Crea un método que retorne el tamaño de esa lista.
4.  Inyecta este servicio en un nuevo controlador y expón una ruta `GET /tasks/count`.

---

## 🏁 Cierre y Reflexión
*   **¿Por qué usamos `@Service`?** Para que Spring sepa que debe crear esa clase por nosotros y tenerla lista para ser inyectada.
*   **¿Qué pasa si quitamos el constructor en el controlador?** Spring no sabrá cómo obtener una instancia de `GreetingService` y la aplicación fallará al arrancar (NullPointerException o Error de Contexto).
