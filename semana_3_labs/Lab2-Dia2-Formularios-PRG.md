# 🧪 LABORATORIO DÍA 2 — Gestión de Formularios y Patrón Post-Redirect-Get

**Módulo 6.1 · Semana 3 · Proyecto LibroTech 📚**  
**Duración estimada:** 2 horas  
**Nivel:** Intermedio  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Manejar la captura de datos desde formularios HTML utilizando Thymeleaf.
2. Utilizar la anotación `@ModelAttribute` para vincular datos del formulario a objetos Java.
3. Implementar el patrón de navegación **Post-Redirect-Get (PRG)** para evitar el reenvío accidental de formularios.
4. Generar enlaces dinámicos con la directiva `th:href`.

---

## 📖 Contexto de Negocio — LibroTech

El dashboard de lectura está listo, pero los bibliotecarios necesitan una forma de agregar nuevos libros directamente desde la interfaz web, en lugar de usar herramientas externas como Postman. Necesitamos construir un formulario de registro y procesar esa información de forma segura, garantizando que si el usuario recarga la página, no se registre el mismo libro dos veces.

---

## 🧠 Contexto Conceptual: Formularios y Patrón PRG

- **`th:object` y `th:field`:** Permiten vincular un formulario HTML directamente con un objeto Java (Data Binding). `th:field` autogenera los atributos `id`, `name` y `value` correspondientes.
- **`@ModelAttribute`:** En el controlador, esta anotación recibe los datos enviados por el formulario y los convierte automáticamente en un objeto de dominio (como `Libro`).
- **Patrón Post-Redirect-Get (PRG):** Es una buena práctica en desarrollo web. Cuando un usuario envía un formulario (`POST`), en lugar de devolverle directamente una vista HTML (lo que causaría que una recarga reenvíe el formulario), el servidor procesa los datos y responde con una **redirección** (HTTP 302). El navegador entonces hace una nueva petición `GET` a la nueva URL.

---

## 📝 Actividades

### Actividad 1 — Preparar el Formulario en el Controlador

Abra la clase `LibroUIController` y agregue un método para mostrar el formulario de creación. Debemos enviar un objeto `Libro` vacío a la vista para que Thymeleaf pueda hacer el "binding".

```java
    // ... código anterior ...

    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        // Pasamos una instancia vacía que el formulario llenará
        model.addAttribute("libro", new Libro());
        model.addAttribute("tituloPantalla", "Registrar Nuevo Libro");
        
        return "libros/formulario";
    }
```

### Actividad 2 — Crear la Vista del Formulario

Cree el archivo `formulario.html` en `src/main/resources/templates/libros/`.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${tituloPantalla}">Formulario</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; font-weight: bold; }
        input { padding: 8px; width: 300px; }
        button { padding: 10px 15px; background-color: #4CAF50; color: white; border: none; cursor: pointer; }
        .btn-cancel { background-color: #f44336; text-decoration: none; display: inline-block; padding: 10px 15px; color: white; }
    </style>
</head>
<body>

    <h1 th:text="${tituloPantalla}">Nuevo Libro</h1>

    <!-- El formulario se vincula al objeto 'libro' enviado desde el controlador -->
    <form th:action="@{/admin/libros/guardar}" th:object="${libro}" method="post">
        
        <div class="form-group">
            <label for="titulo">Título del Libro:</label>
            <!-- th:field se encarga del id, name y value -->
            <input type="text" th:field="*{titulo}" required />
        </div>

        <div class="form-group">
            <label for="autor">Autor:</label>
            <input type="text" th:field="*{autor}" required />
        </div>

        <div class="form-group">
            <label for="isbn">ISBN:</label>
            <input type="text" th:field="*{isbn}" required />
        </div>

        <div class="form-group">
            <label for="anioPublicacion">Año de Publicación:</label>
            <input type="number" th:field="*{anioPublicacion}" required />
        </div>

        <button type="submit">Guardar Libro</button>
        <!-- th:href genera rutas relativas al contexto de la app -->
        <a th:href="@{/admin/libros}" class="btn-cancel">Cancelar</a>
        
    </form>

</body>
</html>
```

### Actividad 3 — Procesar el Formulario (Patrón PRG)

Regrese a `LibroUIController` y agregue el método para recibir el `POST` del formulario. Aquí aplicaremos el patrón PRG.

```java
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.ModelAttribute;

    // ... código anterior ...

    @PostMapping("/guardar")
    public String guardarLibro(@ModelAttribute("libro") Libro libro) {
        // Guardamos el libro usando el servicio
        libroService.guardar(libro);
        
        // Patrón PRG: Redirigimos a la lista de libros (GET)
        // La palabra clave "redirect:" le dice a Spring que emita un HTTP 302
        return "redirect:/admin/libros";
    }
```

### Actividad 4 — Conectar la Lista con el Formulario

Abra su archivo `lista.html` (del Laboratorio 1) y agregue un botón/enlace en la parte superior para ir al formulario de creación:

```html
    <h1 th:text="${tituloPantalla}">Lista de Libros</h1>
    
    <!-- Agregue esta línea debajo del H1 -->
    <div style="margin-bottom: 20px;">
        <a th:href="@{/admin/libros/nuevo}" style="padding: 10px; background-color: #008CBA; color: white; text-decoration: none;">+ Agregar Nuevo Libro</a>
    </div>
```

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| Se utiliza `th:object` en el `<form>` y `th:field` en los `<input>` | ☐ |
| El controlador utiliza `@ModelAttribute` para capturar los datos del POST | ☐ |
| El método de guardado utiliza `return "redirect:..."` (Patrón PRG) | ☐ |
| Los enlaces se construyen dinámicamente usando `th:href="@{...}"` | ☐ |

---

## 🔍 Preguntas de Reflexión

1. Si no usáramos `redirect:/admin/libros` y en su lugar retornáramos `"libros/lista"`, ¿qué pasaría si el usuario presiona F5 (Actualizar) en su navegador después de guardar?
2. ¿Por qué usamos `*{campo}` en los inputs en lugar de `${libro.campo}`? ¿Qué ventaja ofrece `th:object`?
3. ¿Cómo maneja Spring internamente la conversión del formulario HTML (texto) al objeto `Libro` de Java?
