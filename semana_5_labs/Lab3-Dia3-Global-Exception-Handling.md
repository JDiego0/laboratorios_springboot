# 🧪 LABORATORIO DÍA 3 — Manejo Global de Excepciones y Validación en Thymeleaf

**Módulo 6.1 · Semana 5 · Proyecto LibroTech 📚**  
**Duración estimada:** 3 horas  
**Nivel:** Avanzado  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Centralizar el manejo de errores de la API usando `@RestControllerAdvice`.
2. Estandarizar la respuesta de error utilizando la especificación **RFC 7807 (ProblemDetail)** de Spring Boot 3.
3. Extraer y formatear elegantemente los mensajes de error de Jakarta Validation (`MethodArgumentNotValidException`).
4. Conectar la validación del Backend con el Frontend utilizando `BindingResult` y `th:errors` en Thymeleaf.

---

## 📖 Contexto de Negocio — LibroTech

Tenemos un problema de experiencia de usuario y de integrabilidad:
1. **API:** Cuando ocurre un error (ej. buscando un libro que no existe), Spring Boot devuelve un HTML feo de Tomcat o un JSON genérico gigante con un stacktrace. Esto es inaceptable para los clientes móviles de LibroTech, que necesitan un formato estándar (`ProblemDetail`).
2. **Frontend:** En nuestro panel de administración (Thymeleaf), si el bibliotecario intenta crear un libro y el formulario falla en el backend (ej. título vacío), la aplicación explota mostrando la página blanca de error genérica ("Whitelabel Error Page"). El bibliotecario pierde todos los datos que escribió. Necesitamos que la página se recargue mostrando en rojo exactamente qué campos fallaron, conservando lo que ya había escrito.

---

## 📝 Actividades

### Actividad 1 — Centralizar los Errores de la API REST

Vamos a crear un manejador global de excepciones que intercepte los errores y los envuelva en el formato estándar RFC 7807.

**1.1 Crear la clase `GlobalExceptionHandler`:**

Cree un paquete `exception` y añada esta clase. La anotación `@RestControllerAdvice` convierte a esta clase en un interceptor global. Cada vez que cualquier controlador arroje una excepción, Spring buscará aquí un método `@ExceptionHandler` compatible. Utilizamos `ProblemDetail` (introducido en Spring Boot 3) para generar respuestas estandarizadas según el RFC 7807, lo cual es ideal para APIs profesionales.
```java
package com.librotech.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Manejo de Errores Genéricos (ej. libro no encontrado)
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Error de Negocio");
        problemDetail.setType(URI.create("https://librotech.com/docs/errores/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // 2. Manejo de Errores de Validación (Jakarta Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos");
        problemDetail.setTitle("Error de Validación");
        problemDetail.setType(URI.create("https://librotech.com/docs/errores/validacion"));

        // Extraer los errores de campo específicos
        Map<String, String> errores = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // Añadir el mapa de errores como una propiedad extra en el JSON
        problemDetail.setProperty("erroresDetallados", errores);
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}
```

> 💡 **Pruebe en Postman:** Envíe un JSON inválido a `POST /api/libros`. Verá que la respuesta ahora es un JSON elegante tipo `application/problem+json` que incluye un nodo `erroresDetallados` con mensajes claros.

---

### Actividad 2 — Preparar el Controlador de Vistas (Thymeleaf)

Para que Thymeleaf pueda mostrar los errores, necesita acceso al objeto `BindingResult` que captura el fallo de la validación, y debe devolver la misma vista en lugar de redirigir.

**2.1 Modificar `LibroUIController`:**

(Asuma que tiene un controlador para las vistas HTML en un paquete distinto a la API). Para que un formulario en Thymeleaf pueda mostrar errores y mantener los datos que el usuario ya digitó, necesitamos dos cosas: 1) El formulario debe enviarse por POST hacia un método que reciba el DTO con `@Valid`. 2) Inmediatamente después del DTO, debemos inyectar el objeto `BindingResult`. Si hay errores, en lugar de hacer un `redirect` (lo cual borraría todo), retornamos la misma vista del formulario para que Thymeleaf la vuelva a renderizar con los mensajes de error.
```java
@Controller
@RequestMapping("/admin/libros")
public class LibroUIController {

    private final LibroService libroService;

    // ... Constructor ...

    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {
        // Debemos inyectar un objeto vacío en el modelo para que el formulario se enlace
        model.addAttribute("libroCreateDTO", new LibroCreateDTO(null, null, null, null, null, null, null));
        return "libros/formulario";
    }

    @PostMapping("/guardar")
    public String guardarLibro(
            @Valid @ModelAttribute("libroCreateDTO") LibroCreateDTO dto,
            BindingResult bindingResult, // DEBE IR JUSTO DESPUÉS DEL OBJETO VALIDADO
            Model model) {

        // Si la validación falla, recargamos la MISMA VISTA
        if (bindingResult.hasErrors()) {
            // Thymeleaf usará el BindingResult automáticamente
            return "libros/formulario";
        }

        libroService.crearLibro(dto);
        return "redirect:/admin/libros"; // Patrón PRG (Post-Redirect-Get) si tiene éxito
    }
}
```

---

### Actividad 3 — Interfaz Reactiva en Thymeleaf

Vamos a modificar nuestra plantilla `formulario.html` para que escuche esos errores e informe al bibliotecario.

**3.1 Modificar el formulario Thymeleaf:**

Thymeleaf se integra perfectamente con `BindingResult`. Usando el atributo `th:field`, Thymeleaf enlaza el input con el campo del DTO, preservando su valor en caso de error. Además, `th:errorclass` añade dinámicamente clases CSS (como `is-invalid` de Bootstrap) si el campo falló, y `th:errors` extrae e imprime el mensaje exacto que definimos en las anotaciones del DTO (ej. 'El precio no puede ser negativo').
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Nuevo Libro</title>
    <!-- Incluir Bootstrap para facilitar los estilos de error -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<div class="container mt-5">
    <h2>Registrar Nuevo Libro</h2>

    <form th:action="@{/admin/libros/guardar}" th:object="${libroCreateDTO}" method="post">
        
        <!-- Campo: Título -->
        <div class="mb-3">
            <label class="form-label">Título</label>
            <!-- th:errorclass agrega la clase 'is-invalid' de Bootstrap si hay error -->
            <input type="text" th:field="*{titulo}" 
                   class="form-control" th:errorclass="is-invalid">
            
            <!-- Aquí se imprime el mensaje del DTO (@NotBlank) -->
            <div class="invalid-feedback" th:if="${#fields.hasErrors('titulo')}" th:errors="*{titulo}">
                Error de título
            </div>
        </div>

        <!-- Campo: Autor -->
        <div class="mb-3">
            <label class="form-label">Autor</label>
            <input type="text" th:field="*{autor}" 
                   class="form-control" th:errorclass="is-invalid">
            <div class="invalid-feedback" th:if="${#fields.hasErrors('autor')}" th:errors="*{autor}"></div>
        </div>

        <!-- Campo: Precio -->
        <div class="mb-3">
            <label class="form-label">Precio</label>
            <input type="number" step="0.01" th:field="*{precio}" 
                   class="form-control" th:errorclass="is-invalid">
            <div class="invalid-feedback" th:if="${#fields.hasErrors('precio')}" th:errors="*{precio}"></div>
        </div>

        <!-- Omitimos el resto de campos por brevedad, aplique el mismo patrón -->

        <button type="submit" class="btn btn-primary">Guardar Libro</button>
    </form>
</div>
</body>
</html>
```

> 💡 **Pruebe la aplicación:** Navegue a `/admin/libros/nuevo`. Deje el título en blanco y ponga un precio de `-5`. Al dar clic en "Guardar", la página no explotará. Se recargará, los inputs defectuosos se pondrán con borde rojo y verá los mensajes exactos de sus DTOs ("El título es obligatorio", "El precio no puede ser negativo").

---

## 🏋️ Retos para el Coder

### Reto 1 — Excepciones Personalizadas (Custom Exceptions)
Reemplace los `RuntimeException` genéricos en el servicio por una excepción personalizada `ResourceNotFoundException`. En el `@RestControllerAdvice`, intercepte específicamente esta clase y devuelva un `ProblemDetail` con HTTP `404 Not Found` en lugar de `400 Bad Request`.

### Reto 2 — Mensajes Globales (Flash Attributes)
En el Controlador, si el libro se guarda exitosamente, utilice un `RedirectAttributes` para añadir un mensaje flash (ej. "Libro guardado con éxito"). Muéstrelo en la vista del listado general usando un `<div class="alert alert-success">`.

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| Existe una clase `@RestControllerAdvice` que maneja `MethodArgumentNotValidException` | ☐ |
| Las peticiones REST inválidas retornan el formato `ProblemDetail` (RFC 7807) | ☐ |
| El controlador de vista recibe y evalúa el `BindingResult` | ☐ |
| Thymeleaf utiliza `th:errorclass` y `th:errors` para mostrar los problemas en pantalla | ☐ |
| Los datos ingresados previamente por el usuario no se pierden tras un fallo de validación | ☐ |
