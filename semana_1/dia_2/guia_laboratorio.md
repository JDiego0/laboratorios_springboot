# 🌐 Laboratorio Día 2: Controladores y Comunicación REST

En este segundo día, profundizaremos en cómo Spring MVC maneja las peticiones web y cómo podemos recibir datos del cliente de diferentes formas.

## 🎯 Objetivos de Aprendizaje
1.  Dominar las anotaciones `@GetMapping` y `@PostMapping`.
2.  Diferenciar entre `@RequestParam`, `@PathVariable` y `@RequestBody`.
3.  Implementar un CRUD básico en memoria para entender el flujo de datos.

---

## 🛠️ Parte 1: El Modelo de Datos
Crearemos una clase simple para representar a un "Coder".

1.  Crear paquete `com.riwi.intro.models`.
2.  Clase `Coder.java`:

```java
package com.riwi.intro.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Coder {
    private Long id;
    private String name;
    private String clan;
}
```

---

## 🏗️ Parte 2: El Controlador de Coders
Implementaremos diferentes formas de recibir datos.

```java
package com.riwi.intro.controllers;

import com.riwi.intro.models.Coder;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/coders") // Prefijo para todos los endpoints de este controlador
public class CoderController {

    private List<Coder> coders = new ArrayList<>();

    // 1. Path Variable: Para recursos específicos por ID
    @GetMapping("/{id}")
    public Coder getById(@PathVariable Long id) {
        return coders.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // 2. Request Param: Para filtros o búsquedas
    @GetMapping("/search")
    public List<Coder> searchByClan(@RequestParam String clan) {
        return coders.stream()
                .filter(c -> c.getClan().equalsIgnoreCase(clan))
                .toList();
    }

    // 3. Request Body: Para recibir objetos complejos (JSON)
    @PostMapping
    public Coder create(@RequestBody Coder coder) {
        coders.add(coder);
        return coder;
    }
}
```

---

## 🧪 Parte 3: Desafío del Día
**Reto "Update & Delete":**
1.  Implementa un método `@DeleteMapping("/{id}")` que elimine un coder de la lista.
2.  Implementa un método `@PutMapping("/{id}")` que actualice los datos de un coder existente.

---

## 🔍 Temas de Investigación y Aprendizaje Continuo
Para la siguiente sesión, investiga los siguientes temas que están marcando tendencia en el ecosistema Java:

1.  **Niveles de Madurez de Richardson:** ¿Qué diferencia a una API "buena" de una API "RESTful" profesional?
2.  **HTTP Status Codes:** Investiga cuándo es correcto usar `201 Created` vs `200 OK` y por qué nunca deberías devolver un `200` si hubo un error.
3.  **JSON Serialization:** ¿Cómo hace Spring para convertir un objeto Java en un JSON? (Pista: Busca sobre la librería **Jackson**).

---
> [!NOTE]
> Recuerda que en este punto los datos se pierden al reiniciar la aplicación. ¡Eso lo solucionaremos en la Semana 2!
