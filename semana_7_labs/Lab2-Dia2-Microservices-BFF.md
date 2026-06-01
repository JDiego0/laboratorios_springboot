# 🧪 LABORATORIO DÍA 2 — Microservicios UI y Backend-For-Frontend (BFF)

**Módulo 6.1 · Semana 7 · Proyecto LibroTech 📚**  
**Duración estimada:** 3 horas  
**Nivel:** Experto  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Iniciar la transición de monolito a microservicios separando la Capa de Presentación de la Capa de Negocio.
2. Construir un microservicio independiente llamado `ui-service` que actuará como **BFF (Backend-For-Frontend)**.
3. Garantizar que el `ui-service` no posea base de datos y obtenga la información invocando APIs externas vía `RestClient` (o `WebClient`).
4. Reenviar tokens de seguridad (JWT) desde el frontend web hacia los microservicios de backend.

---

## 📖 Contexto de Negocio — LibroTech

El monolito de LibroTech tiene una fuerte dependencia entre las plantillas HTML (Thymeleaf) y los servicios de base de datos.
El equipo de UI quiere escalar el panel de administración web, ya que experimenta picos de tráfico en fechas de matriculación, pero el equipo de Backend se queja de que esos despliegues afectan el núcleo de base de datos.

La solución: Extraer **todo lo visual** a su propio microservicio. 

A partir de hoy, nuestro viejo "monolito" será exclusivamente una API REST (Hexagonal) y lo llamaremos `libro-service`. Por otro lado, crearemos un nuevo microservicio llamado `ui-service` cuyo único propósito será devolver las páginas HTML y hablar por la red (HTTP) con `libro-service` para traer los datos.

---

## 📝 Actividades

### Actividad 1 — Crear el Microservicio UI (Cero Base de Datos)

**1.1 Crear un nuevo proyecto Spring Boot (`ui-service`):**
Vaya a [Spring Initializr](https://start.spring.io/) y cree un nuevo proyecto.
- **Dependencies:** Spring Web, Thymeleaf, Lombok.
- **NO incluya:** Spring Data JPA, ni drivers de BD (MySQL, PostgreSQL, H2).

**1.2 Migrar las Vistas:**
Corte y pegue toda su carpeta `src/main/resources/templates` y `src/main/resources/static` desde su monolito original hacia el nuevo `ui-service`.

**1.3 Configurar el Puerto:**

Dado que el backend original (ahora `libro-service`) y el nuevo `ui-service` correrán simultáneamente en la misma máquina durante el desarrollo, no pueden usar el mismo puerto. Asignaremos el clásico 8080 a la UI (lo que ven los usuarios) y el 8081 al backend (la API oculta). También definimos la URL del backend como una variable paramétrica en `application.properties`:
```properties
server.port=8080
spring.application.name=ui-service

# Variable de entorno apuntando al backend (útil para Docker después)
backend.libro-service.url=http://localhost:8081
```

*(En su monolito original, que ahora será `libro-service`, cambie el puerto a `8081`).*

---

### Actividad 2 — Consumir APIs Externas con RestClient

Dado que el `ui-service` no tiene repositorios de JPA, para mostrar la tabla de libros en la vista HTML, debemos hacer una llamada HTTP GET al backend.

**2.1 Configurar RestClient:**

En Spring Boot 3.2+, `RestClient` es la forma moderna (y bloqueante) recomendada para consumir APIs, reemplazando a `RestTemplate`. Creamos un `@Bean` de configuración centralizado. Esto nos permite instanciar el cliente HTTP una sola vez con una URL base por defecto, haciendo que su uso posterior sea más limpio y permitiéndonos en el futuro inyectar interceptores globales:
```java
package com.librotech.ui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${backend.libro-service.url}")
    private String libroServiceUrl;

    @Bean
    public RestClient libroServiceClient() {
        return RestClient.builder()
                .baseUrl(libroServiceUrl)
                // Aquí podríamos añadir un interceptor para enviar el JWT siempre
                .build();
    }
}
```

**2.2 Crear el UI Service Proxy:**

Este servicio "finge" ser nuestro antiguo servicio de base de datos. El patrón Proxy oculta la complejidad de la red. Para el controlador web, invocar este método parece una llamada local. Sin embargo, internamente el proxy construye una petición GET, le inyecta el token de seguridad en el header, cruza la red hacia el puerto 8081, y deserializa el JSON devuelto en una lista de DTOs.
```java
package com.librotech.ui.service;

import com.librotech.ui.dto.LibroResponseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class LibroUiProxyService {

    private final RestClient restClient;

    public LibroUiProxyService(RestClient libroServiceClient) {
        this.restClient = libroServiceClient;
    }

    public List<LibroResponseDTO> obtenerTodosLosLibros(String jwtToken) {
        return restClient.get()
                .uri("/api/libros")
                .header("Authorization", "Bearer " + jwtToken) // Pasamos el JWT al backend
                .retrieve()
                .body(new ParameterizedTypeReference<List<LibroResponseDTO>>() {});
    }
}
```

---

### Actividad 3 — Reconstruir el Controlador Thymeleaf

Ahora el `@Controller` de Thymeleaf usará el Proxy Service. Este controlador ahora es agnóstico de la base de datos. Extrae la cookie con el JWT provisto por el usuario, delega el trabajo pesado al proxy que hace la llamada HTTP al backend, y si recibe respuesta, renderiza la vista Thymeleaf. Si algo falla (ej. el backend está apagado), atrapa el error elegantemente.
```java
package com.librotech.ui.controller;

import com.librotech.ui.service.LibroUiProxyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/libros")
public class LibroWebController {

    private final LibroUiProxyService proxyService;

    public LibroWebController(LibroUiProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping
    public String listarLibros(
            // Extraemos la cookie JWT que se generó en el login
            @CookieValue(value = "jwt", defaultValue = "") String jwtToken, 
            Model model) {
        
        if (jwtToken.isEmpty()) {
            return "redirect:/login"; // Si no hay token, a loguearse
        }

        try {
            // Llamada de red hacia el backend 8081
            var libros = proxyService.obtenerTodosLosLibros(jwtToken);
            model.addAttribute("libros", libros);
            return "libros/listado";
            
        } catch (Exception e) {
            // Manejar error de conexión
            model.addAttribute("error", "Servicio de libros no disponible temporalmente");
            return "error";
        }
    }
}
```

---

### Actividad 4 — Migrar la Gestión de Sesión (Login)

Recuerde que en la Semana 6, nuestro Backend generaba una Cookie `HttpOnly` con el JWT.
Dado que la URL que visita el navegador ahora es la del `ui-service` (puerto 8080), **este microservicio BFF debe ser quien se encargue del proceso de Login**.

**¿Cómo funciona el flujo ahora?**
1. El usuario navega a `http://localhost:8080/login` (`ui-service`).
2. Envía email/password al Controlador de Login del `ui-service`.
3. El `ui-service` hace un `POST /api/auth/login` hacia el backend (puerto 8081).
4. El backend genera el String del JWT y lo devuelve al `ui-service`.
5. El `ui-service` toma el JWT, lo mete en una cookie `HttpOnly` y se lo envía al navegador web del usuario.

¡Intente implementar este flujo usted mismo utilizando `RestClient`!

---

## 🏋️ Retos para el Coder

### Reto 1 — Mapeo de Excepciones HTTP
Si el `libro-service` (Backend) rechaza una petición (por ejemplo con `HTTP 400 Bad Request` porque falló una validación de un formulario al crear un libro), el `RestClient` del `ui-service` lanzará una `RestClientResponseException`. Atrápela, lea el `ProblemDetail` que mandó el backend, y páselo al `BindingResult` para que Thymeleaf pinte el formulario de rojo como lo hacíamos en el monolito.

### Reto 2 — Interceptor de JWT Automático
En lugar de pasar `@CookieValue String jwt` manualmente a cada método del controlador, cree un filtro en el `ui-service` que extraiga la cookie, la guarde en un `ThreadLocal` (o en un objeto RequestScoped), y modifique la definición del `@Bean RestClient` añadiendo un interceptor (`.requestInterceptor(...)`) que automáticamente inyecte el header `Authorization: Bearer <token>` en TODAS las peticiones hacia el backend.

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| Existe un proyecto `ui-service` independiente ejecutándose en un puerto distinto | ☐ |
| El archivo `pom.xml` del `ui-service` no contiene dependencias de base de datos JPA | ☐ |
| El controlador de UI utiliza `RestClient` u otro cliente HTTP para obtener datos | ☐ |
| El JWT se transfiere desde la Cookie del navegador hacia el Header para el backend | ☐ |
| La interfaz Thymeleaf renderiza los datos exitosamente provenientes del backend remoto | ☐ |
