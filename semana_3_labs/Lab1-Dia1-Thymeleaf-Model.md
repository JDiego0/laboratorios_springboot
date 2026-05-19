# 🧪 LABORATORIO DÍA 1 — Introducción a Thymeleaf y Arquitectura Híbrida

**Módulo 6.1 · Semana 3 · Proyecto LibroTech 📚**  
**Duración estimada:** 2 horas  
**Nivel:** Intermedio  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Configurar y utilizar el motor de plantillas **Thymeleaf**.
2. Comprender la **Arquitectura Híbrida**, separando rutas de API REST (`/api/**`) de las rutas de interfaz de usuario (`/admin/**`).
3. Enviar datos desde el Controlador a la Vista utilizando el objeto `Model`.
4. Utilizar directivas esenciales de Thymeleaf como `th:text`, `th:each` y `th:if` para renderizar datos dinámicos.

---

## 📖 Contexto de Negocio — LibroTech

Hasta ahora, **LibroTech** cuenta con una API REST funcional que devuelve datos en formato JSON. Sin embargo, los bibliotecarios necesitan una interfaz web gráfica y amigable para gestionar el inventario sin depender de herramientas técnicas como Postman. En esta fase, construiremos un panel de administración web (Dashboard) integrado directamente en nuestra aplicación Spring Boot.

---

## 🧠 Contexto Conceptual: Thymeleaf y MVC Tradicional

En semanas anteriores construimos una API REST (JSON). Ahora volveremos al **MVC Tradicional** donde el servidor genera el HTML final que verá el usuario.

- **Thymeleaf:** Es un motor de plantillas moderno para Java. Se basa en **Natural Templates**, lo que significa que los archivos HTML de Thymeleaf pueden abrirse en un navegador y verse correctamente como HTML estático, incluso sin el servidor encendido.
- **Model:** Es una interfaz de Spring que permite pasar datos (atributos) desde el Controlador hacia la Vista (HTML).
- **Arquitectura Híbrida:** Una misma aplicación Spring Boot puede servir JSON (con `@RestController` en `/api`) y HTML (con `@Controller` regular en `/admin`).

### Directivas Clave de Thymeleaf:
- `th:text`: Reemplaza el texto interno de una etiqueta HTML con un valor dinámico.
- `th:each`: Itera sobre una colección (lista) para repetir un bloque HTML.
- `th:if`: Evalúa una condición booleana para mostrar u ocultar un elemento HTML.

---

## 📝 Actividades

### Actividad 1 — Configurar Thymeleaf y la Capa Web

Asegúrese de tener la dependencia de Thymeleaf en su `pom.xml`. Si no la tiene, agréguela (y actualice Maven):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

### Actividad 2 — Crear el Controlador de Interfaz de Usuario (UI)

Cree el paquete `com.librotech.controller.ui` (para separarlo de los REST controllers) y agregue la clase `LibroUIController`. Note que usamos `@Controller`, no `@RestController`.

```java
package com.librotech.controller.ui;

import com.librotech.model.Libro;
import com.librotech.service.LibroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/libros")
public class LibroUIController {

    @Autowired
    private LibroService libroService;

    @GetMapping
    public String listarLibrosUI(Model model) {
        List<Libro> libros = libroService.obtenerTodos();
        
        // Pasamos la lista de libros a la vista con el nombre "libros"
        model.addAttribute("libros", libros);
        model.addAttribute("tituloPantalla", "Catálogo de Libros - Dashboard");
        
        // Retornamos el nombre del archivo HTML (sin la extensión .html)
        return "libros/lista"; 
    }
}
```

---

### Actividad 3 — Crear la Vista Dinámica con Thymeleaf

Cree la estructura de carpetas `src/main/resources/templates/libros/`.
Dentro de la carpeta `libros`, cree el archivo `lista.html`.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${tituloPantalla}">Catálogo Estático</title>
    <!-- Estilos básicos embebidos para el laboratorio -->
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .empty-state { color: #888; font-style: italic; }
    </style>
</head>
<body>

    <h1 th:text="${tituloPantalla}">Lista de Libros</h1>
    
    <!-- th:if evalúa si la lista está vacía -->
    <div th:if="${#lists.isEmpty(libros)}" class="empty-state">
        <p>No hay libros registrados en el sistema actualmente.</p>
    </div>

    <!-- th:unless es lo opuesto a th:if (si NO está vacía) -->
    <table th:unless="${#lists.isEmpty(libros)}">
        <thead>
            <tr>
                <th>ID</th>
                <th>Título</th>
                <th>Autor</th>
                <th>ISBN</th>
                <th>Año</th>
            </tr>
        </thead>
        <tbody>
            <!-- th:each itera sobre la colección ${libros} -->
            <tr th:each="libro : ${libros}">
                <td th:text="${libro.id}">1</td>
                <td th:text="${libro.titulo}">El Quijote</td>
                <td th:text="${libro.autor}">Cervantes</td>
                <td th:text="${libro.isbn}">123-456</td>
                <td th:text="${libro.anioPublicacion}">1605</td>
            </tr>
        </tbody>
    </table>

</body>
</html>
```

---

### Actividad 4 — Pruebas del Dashboard

1. Inicie su aplicación Spring Boot.
2. Abra su navegador web y visite: `http://localhost:8080/admin/libros`
3. Si la base de datos está vacía, verá el mensaje de estado vacío (`th:if`).
4. Utilice Postman para registrar 2 o 3 libros mediante su API REST (`POST /api/libros`).
5. Recargue la página en su navegador. ¡Ahora debería ver la tabla renderizada dinámicamente (`th:each`)!

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| El controlador usa `@Controller` en lugar de `@RestController` | ☐ |
| Se inyecta correctamente el objeto `Model` para pasar atributos | ☐ |
| La plantilla HTML incluye el namespace `xmlns:th` de Thymeleaf | ☐ |
| Se renderiza la lista de libros usando `th:each` y `th:text` | ☐ |

---

## 🔍 Preguntas de Reflexión

1. Si abre el archivo `lista.html` haciendo doble clic directamente en su explorador de archivos (sin Spring Boot), ¿qué datos observa en la tabla? ¿Por qué es útil esta característica de "Natural Templates"?
2. ¿Cuál es la diferencia fundamental entre el `return` de un `@RestController` y el `return` de un `@Controller`?
3. ¿Por qué separamos las rutas bajo `/admin/**` y dejamos las de la API bajo `/api/**`?
