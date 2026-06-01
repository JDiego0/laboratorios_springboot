# 🧪 LABORATORIO DÍA 2 — Jakarta Bean Validation y Validadores Personalizados

**Módulo 6.1 · Semana 5 · Proyecto LibroTech 📚**  
**Duración estimada:** 3 horas  
**Nivel:** Avanzado  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Proteger la integridad de la base de datos validando los DTOs en la capa de entrada.
2. Utilizar las anotaciones estándar de **Jakarta Bean Validation** (`@NotBlank`, `@Min`, `@PastOrPresent`).
3. Crear **Anotaciones y Validadores Personalizados** para reglas de negocio complejas (ej. validación del formato ISBN).
4. Capturar los errores de validación a nivel del Controlador usando la anotación `@Valid`.

---

## 📖 Contexto de Negocio — LibroTech

Ahora que LibroTech utiliza DTOs, hemos resuelto el problema de exposición de datos. Sin embargo, los clientes de la API pueden enviarnos datos inválidos: títulos vacíos, precios negativos, fechas de publicación en el futuro, o ISBNs que no cumplen con el formato estándar.

Actualmente, estos datos llegan hasta la base de datos y causan excepciones de SQL (violaciones de constraints) o, peor aún, se guardan datos sucios. Necesitamos una barrera en nuestra API que rechace estas peticiones **antes** de que lleguen a la lógica de negocio (Servicio).

---

## 📝 Actividades

### Actividad 1 — Configurar Dependencia y Validaciones Estándar

**1.1 Agregue el starter de validación en su `pom.xml`:**

Spring Boot ofrece un "starter" que agrupa todas las librerías necesarias de Jakarta Bean Validation (la especificación) y Hibernate Validator (la implementación por defecto). Esto nos permite usar anotaciones estándar sin descargar dependencias sueltas.
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**1.2 Añada restricciones a `LibroCreateDTO`:**

Abra el record `LibroCreateDTO` e incluya las anotaciones de Jakarta Validation. Añadir estas anotaciones directamente en el Record instruye al validador sobre las reglas de negocio básicas. Usaremos `@NotBlank` para strings, `@PastOrPresent` para evitar fechas futuras, y `@PositiveOrZero` para evitar precios negativos.
```java
package com.librotech.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Set;

public record LibroCreateDTO(
    
    @NotBlank(message = "El título es obligatorio")
    @Size(min = 2, max = 255, message = "El título debe tener entre 2 y 255 caracteres")
    String titulo,

    @NotBlank(message = "El autor es obligatorio")
    String autor,

    // Temporalmente usaremos @NotBlank, en la siguiente actividad haremos nuestro propio validador
    @NotBlank(message = "El ISBN no puede estar vacío")
    String isbn,

    @NotNull(message = "La fecha de publicación es obligatoria")
    @PastOrPresent(message = "La fecha de publicación no puede estar en el futuro")
    LocalDate fechaPublicacion,

    @NotNull(message = "El precio es obligatorio")
    @PositiveOrZero(message = "El precio no puede ser negativo")
    Double precio,

    @NotNull(message = "Debe especificar el ID de la editorial")
    @Positive(message = "ID de editorial inválido")
    Long editorialId,

    @NotEmpty(message = "El libro debe pertenecer al menos a un género")
    Set<Long> generoIds
) {}
```

**1.3 Activar la validación en el Controlador:**

Las anotaciones en el DTO por sí solas no hacen nada a menos que le digamos a Spring que las procese. La anotación `@Valid` actúa como un interruptor. Al recibir un request, Spring interceptará el JSON, lo convertirá al DTO y ejecutará todas las reglas definidas. Si alguna falla, la ejecución se detiene antes de entrar al cuerpo del método.
```java
// En LibroController.java
@PostMapping
public ResponseEntity<LibroResponseDTO> crearLibro(@Valid @RequestBody LibroCreateDTO dto) {
    // Si los datos son inválidos, Spring lanzará MethodArgumentNotValidException
    // y este código NO se ejecutará.
    LibroResponseDTO nuevoLibro = libroService.crearLibro(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(nuevoLibro);
}
```

---

### Actividad 2 — Crear un Validador Personalizado

A veces las reglas estándar no son suficientes. Queremos validar que el ISBN tenga el formato estándar (Ej. "ISBN-XXXX" o "978-X-XX-XXXXXX-X").

**2.1 Crear la Anotación `@ValidISBN`:**

Cree un paquete `validation` y agregue una nueva @interface de Java. Una anotación en Java es simplemente una "etiqueta" (metadata). Para crear una anotación de validación, usamos `@interface` y la decoramos con `@Constraint`, pasándole como argumento la clase que contendrá la verdadera lógica de validación (`IsbnValidator.class`).
```java
package com.librotech.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = IsbnValidator.class) // Apunta a la clase que hará la validación real
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidISBN {

    // Mensaje de error por defecto
    String message() default "Formato de ISBN inválido. Debe comenzar con 'ISBN-' o '978-'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

**2.2 Crear el Validador `IsbnValidator`:**

Cree la clase que implementa la lógica en el mismo paquete. Esta clase implementa `ConstraintValidator`, indicando que validará la anotación `@ValidISBN` aplicada sobre un campo de tipo `String`. Dentro del método `isValid` escribimos nuestra regla de negocio personalizada en Java puro, retornando `true` si el dato es válido o `false` si debe rechazarse.
```java
package com.librotech.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IsbnValidator implements ConstraintValidator<ValidISBN, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // La validación de @NotNull ya se encarga de los nulos
        if (value == null || value.isBlank()) {
            return true; 
        }

        // Regla de negocio LibroTech:
        // Debe comenzar con ISBN- o con 978-
        boolean empiezaCorrecto = value.startsWith("ISBN-") || value.startsWith("978-");
        
        // Longitud entre 9 y 17 caracteres
        boolean longitudCorrecta = value.length() >= 9 && value.length() <= 17;

        return empiezaCorrecto && longitudCorrecta;
    }
}
```

**2.3 Aplicar la nueva anotación en el DTO:**

Vuelva a `LibroCreateDTO` y reemplace el `@NotBlank` del ISBN con su propia anotación. Con la anotación y su lógica ya construidas, podemos usar `@ValidISBN` exactamente igual que usaríamos una anotación estándar de Jakarta. Esto mantiene nuestro DTO limpio y altamente declarativo.
```java
    @NotBlank(message = "El ISBN no puede estar vacío")
    @ValidISBN
    String isbn,
```

---

### Actividad 3 — Prueba desde Postman/Insomnia

Para verificar que nuestra barrera de validación funciona, intentaremos insertar datos que violan las reglas (precios negativos, fechas futuras, etc.). El servidor debe rechazar la solicitud inmediatamente protegiendo la base de datos.

Levante su aplicación Spring Boot y envíe una petición `POST` hacia `/api/libros` con un JSON inválido:
```json
{
  "titulo": "A",
  "autor": "",
  "isbn": "12345",
  "fechaPublicacion": "2050-01-01",
  "precio": -10.5,
  "editorialId": 1,
  "generoIds": []
}
```

**¿Qué sucede?**
Spring Boot intercepta el error, rechaza la petición con un código HTTP `400 Bad Request` y devuelve un JSON por defecto de Spring detallando cada campo que falló.

---

## 🏋️ Retos para el Coder

### Reto 1 — Validación de Colecciones
¿Qué pasa si el cliente envía `generoIds: [1, -5, null]`? Modifique `LibroCreateDTO` para asegurar que cada elemento individual dentro del `Set` no sea nulo ni negativo. (Pista: puede usar las anotaciones dentro de los diamantes `Set<@NotNull @Positive Long> generoIds`).

### Reto 2 — Validación a Nivel de Clase (Avanzado)
A veces la validación depende de varios campos. Crea un validador a nivel de clase `@LibroCoherente` para el `LibroCreateDTO` que verifique la siguiente regla de negocio: *"Si la editorialId es 1 (Editorial Exclusiva LibroTech), el precio del libro no puede ser menor a $20.000"*.

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| El controlador utiliza `@Valid` para disparar la validación | ☐ |
| Se usan anotaciones estándar en el DTO (`@NotBlank`, `@Positive`, etc) | ☐ |
| Se implementó correctamente la interfaz `ConstraintValidator` | ☐ |
| La anotación `@ValidISBN` fue creada y aplicada al campo correspondiente | ☐ |
| Las peticiones con datos inválidos son rechazadas con HTTP 400 | ☐ |
