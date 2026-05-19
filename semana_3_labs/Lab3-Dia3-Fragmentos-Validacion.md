# 🧪 LABORATORIO DÍA 3 — Componentización con Fragmentos y Validación Visual

**Módulo 6.1 · Semana 3 · Proyecto LibroTech 📚**  
**Duración estimada:** 2 horas  
**Nivel:** Intermedio  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Modularizar plantillas HTML utilizando la directiva `th:fragment` para evitar código duplicado (DRY).
2. Crear componentes reutilizables como encabezados (Header) y pies de página (Footer).
3. Implementar validación de datos básica en el controlador y mostrar mensajes de error visuales en la vista.
4. Integrar estilos estructurados mínimos para dar coherencia visual al sistema.

---

## 📖 Contexto de Negocio — LibroTech

La aplicación de **LibroTech** está creciendo. A medida que agregamos más páginas (ej. gestión de usuarios, categorías, préstamos), notamos que estamos copiando y pegando la barra de navegación y el pie de página en cada archivo HTML. Además, los bibliotecarios a veces ingresan datos erróneos (ej. un año de publicación en el futuro), y necesitamos mostrarles mensajes de error amigables sin que la aplicación colapse.

---

## 🧠 Contexto Conceptual: Fragmentos y Validaciones

- **`th:fragment`:** Permite definir un bloque de código HTML al que se le asigna un nombre. 
- **`th:replace` o `th:insert`:** Permiten inyectar el fragmento previamente definido dentro de otra plantilla. Esto es el equivalente a los "componentes" en frameworks de frontend moderno.
- **Validación Básica con Model:** Antes de usar librerías complejas de validación (como Jakarta Validation), podemos interceptar datos en el controlador, verificar las reglas de negocio, y si fallan, devolver la misma vista inyectando un mensaje de error en el `Model`.

---

## 📝 Actividades

### Actividad 1 — Crear el Archivo de Componentes Compartidos (Fragmentos)

Cree una nueva carpeta llamada `layout` dentro de `src/main/resources/templates/`.
Dentro de `layout`, cree el archivo `componentes.html`.

```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<!-- Este archivo no se renderiza por sí solo, solo almacena fragmentos -->
<body>

    <!-- Fragmento: Navbar -->
    <header th:fragment="navbar" style="background-color: #333; color: white; padding: 15px;">
        <h2 style="margin: 0; display: inline-block;">📚 LibroTech Admin</h2>
        <nav style="display: inline-block; margin-left: 20px;">
            <a th:href="@{/admin/libros}" style="color: white; margin-right: 15px;">Catálogo</a>
            <a th:href="@{/api/libros}" target="_blank" style="color: #aaa;">Ver API JSON</a>
        </nav>
    </header>

    <!-- Fragmento: Footer -->
    <footer th:fragment="footer" style="text-align: center; padding: 20px; color: #777; margin-top: 40px; border-top: 1px solid #ddd;">
        <p>&copy; <span th:text="${#dates.format(#dates.createNow(), 'yyyy')}">2024</span> LibroTech - Sistema de Gestión de Bibliotecas</p>
    </footer>

</body>
</html>
```

---

### Actividad 2 — Refactorizar las Vistas para usar Fragmentos

Modifique los archivos `lista.html` y `formulario.html` (de los laboratorios previos) para inyectar la barra de navegación y el pie de página.

**En `lista.html` y `formulario.html`:**
Agregue el header justo debajo de la etiqueta `<body>` y el footer justo antes de cerrar `</body>`:

```html
<body>

    <!-- th:replace="ruta/archivo :: nombreFragmento" -->
    <div th:replace="~{layout/componentes :: navbar}"></div>

    <div style="margin: 20px;">
        <!-- ... AQUÍ VA EL CONTENIDO PRINCIPAL (El <h1>, la tabla, el formulario, etc.) ... -->
    </div>

    <div th:replace="~{layout/componentes :: footer}"></div>

</body>
```
*Pruebe la aplicación y navegue entre la lista y el formulario. Verá que la barra de navegación y el footer son consistentes.*

---

### Actividad 3 — Implementar Validación Manual en el Controlador

Modifique el método `guardarLibro` en `LibroUIController` para validar que el año de publicación no sea en el futuro. Si lo es, recargamos el formulario con un mensaje de error.

```java
    // ... código anterior ...
    import java.time.LocalDate;

    @PostMapping("/guardar")
    public String guardarLibro(@ModelAttribute("libro") Libro libro, Model model) {
        
        int anioActual = LocalDate.now().getYear();
        
        // Validación de negocio manual
        if (libro.getAnioPublicacion() > anioActual) {
            // Inyectamos el mensaje de error en el modelo
            model.addAttribute("errorAnio", "El año de publicación no puede ser mayor al año actual (" + anioActual + ").");
            model.addAttribute("tituloPantalla", "Registrar Nuevo Libro (Corrección)");
            
            // Retornamos la vista del formulario (NO usamos redirect, para mantener los datos tipeados)
            return "libros/formulario";
        }

        libroService.guardar(libro);
        return "redirect:/admin/libros";
    }
```

---

### Actividad 4 — Mostrar Mensajes de Error Visuales (th:if)

Modifique `formulario.html` para mostrar el mensaje de error si existe en el `Model`. Utilizaremos `th:if` para condicionar su aparición.

```html
    <!-- Contenido del formulario en formulario.html -->
    <h1 th:text="${tituloPantalla}">Nuevo Libro</h1>

    <!-- Bloque de error visual -->
    <div th:if="${errorAnio}" style="background-color: #f8d7da; color: #721c24; padding: 10px; border-radius: 5px; margin-bottom: 15px;">
        <strong th:text="${errorAnio}">Mensaje de error genérico</strong>
    </div>

    <form th:action="@{/admin/libros/guardar}" th:object="${libro}" method="post">
    <!-- ... campos del formulario ... -->
```

*Pruebe creando un libro con el año `2050`. Debería ver el recuadro rojo con el mensaje de error y el formulario no se procesará.*

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| Se creó un archivo para alojar fragmentos de código (`th:fragment`) | ☐ |
| Las vistas integran los componentes usando `th:replace` o `th:insert` | ☐ |
| El controlador valida la información antes de guardar en base de datos | ☐ |
| La vista muestra alertas de error condicionadas mediante `th:if` | ☐ |

---

## 🔍 Preguntas de Reflexión

1. ¿Cuál es la principal ventaja arquitectónica de usar fragmentos (`th:fragment`) en aplicaciones web con muchas pantallas?
2. Cuando el año de publicación es inválido, el controlador hace `return "libros/formulario"` en lugar de `return "redirect:..."`. ¿Por qué hacemos esto y qué pasaría si usáramos redirección al fallar la validación?
3. En el footer usamos una expresión especial: `${#dates.format(#dates.createNow(), 'yyyy')}`. Investigue brevemente qué son los "Expression Utility Objects" (Objetos de Utilidad de Expresiones) en Thymeleaf como `#dates` o `#lists` (que usamos en el Lab 1).
