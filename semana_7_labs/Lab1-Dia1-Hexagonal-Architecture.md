# 🧪 LABORATORIO DÍA 1 — Arquitectura Hexagonal (Ports & Adapters)

**Módulo 6.1 · Semana 7 · Proyecto LibroTech 📚**  
**Duración estimada:** 3 horas  
**Nivel:** Experto  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Comprender la Inversión de Dependencias y su importancia para aislar las reglas de negocio.
2. Reestructurar el código de LibroTech aplicando el patrón **Ports & Adapters (Arquitectura Hexagonal)**.
3. Crear un paquete de **Dominio puro** libre de anotaciones de Spring y JPA (`@Entity`, `@Table`).
4. Implementar **Puertos (Interfaces)** de entrada y salida, conectándolos mediante **Adaptadores**.

---

## 📖 Contexto de Negocio — LibroTech

Hasta ahora, la entidad `Libro` en LibroTech era una clase "mezclada": contenía datos del negocio (ej. ISBN), pero también dependía íntimamente de Hibernate (`@Entity`, `@SQLRestriction`, `@ManyToOne`). 

¿Qué pasa si LibroTech decide migrar su base de datos a MongoDB (NoSQL) el próximo año? Tendríamos que modificar y posiblemente romper nuestras reglas de negocio porque la clase `Libro` está acoplada a JPA.

La directiva ha ordenado que la lógica core del sistema quede "blindada". Debemos implementar Arquitectura Hexagonal. El **Dominio** dictará las reglas, y la infraestructura (Spring Web, Base de Datos MySQL) solo serán **"detalles de implementación"** que se conectarán al dominio mediante enchufes (Puertos y Adaptadores).

---

## 📝 Actividades

### Actividad 1 — Crear la Estructura de Paquetes Hexagonal

Dentro de `src/main/java/com/librotech/`, reorganice (o cree) los siguientes paquetes estrictos. La clave de la Arquitectura Hexagonal es agrupar el código no por capas técnicas (Controladores, Servicios, Repositorios) sino por su relación con el negocio. El `domain` está en el centro, aislado. La `application` orquesta las tareas usando abstracciones (`ports`). Finalmente, la `infrastructure` implementa las tecnologías concretas que empujan datos hacia adentro (Web) o sacan datos hacia afuera (Persistence):
```text
com.librotech
├── domain            # ¡PROHIBIDO IMPORTAR SPRING O JPA AQUÍ!
│   └── model         # Modelos puros del negocio (POJOs)
├── application
│   ├── ports
│   │   ├── in        # Casos de uso (Lo que la app puede hacer)
│   │   └── out       # Contratos de salida (Lo que la app necesita)
│   └── service       # Implementación de los casos de uso
└── infrastructure
    └── adapters
        ├── in
        │   └── web   # Controladores REST (Spring Web)
        └── out
            └── persistence # Spring Data JPA, Entidades JPA
```

---

### Actividad 2 — Construir el Hexágono Interno (Domain y Ports)

**2.1 Modelo de Dominio Puro (`domain/model/Libro.java`)**

Cree una clase de Java estándar. Cero anotaciones de base de datos. En esta clase radica el mayor valor del software. Al remover dependencias tecnológicas como `@Entity` o `@Table`, este modelo se vuelve portable, fácil de testear (sin bases de datos) e inmune a las actualizaciones del framework. Aquí solo hay reglas de negocio puras escritas en Java.
```java
package com.librotech.domain.model;

import java.time.LocalDate;

// ⚠️ NINGÚN IMPORT DE JAKARTA O SPRING
public class Libro {
    private Long id;
    private String titulo;
    private String autor;
    private String isbn;
    private LocalDate fechaPublicacion;
    private Double precio;

    // Lógica pura de negocio:
    public boolean esLibroAntiguo() {
        return fechaPublicacion.isBefore(LocalDate.now().minusYears(20));
    }
    
    // Constructores, Getters y Setters...
}
```

**2.2 Puerto de Entrada (`application/ports/in/CrearLibroUseCase.java`)**

El contrato de lo que el exterior puede pedirle a nuestra aplicación. Los puertos de entrada definen los 'Casos de Uso'. Representan las intenciones del usuario, como 'crear un libro' o 'comprar un libro'. Son interfaces que dictan qué puede hacer nuestro hexágono, independientemente de si la petición llega por una API REST, una consola, o una cola de mensajes.
```java
package com.librotech.application.ports.in;

import com.librotech.domain.model.Libro;

public interface CrearLibroUseCase {
    Libro crearNuevoLibro(Libro libro);
}
```

**2.3 Puerto de Salida (`application/ports/out/LibroRepositoryPort.java`)**

Lo que la aplicación necesita del exterior (generalmente bases de datos o APIs). Los puertos de salida definen qué necesita el hexágono para funcionar (por ejemplo, guardar datos o enviar correos), pero no define *cómo* se hace. Esto es el Principio de Inversión de Dependencias en acción: el dominio dicta el contrato (Note que usa el `Libro` del dominio, no una entidad JPA), y la infraestructura debe adaptarse a él, no al revés.
```java
package com.librotech.application.ports.out;

import com.librotech.domain.model.Libro;
import java.util.Optional;

public interface LibroRepositoryPort {
    Libro guardar(Libro libro);
    Optional<Libro> buscarPorId(Long id);
    boolean existePorIsbn(String isbn);
}
```

---

### Actividad 3 — Implementar la Lógica de Aplicación (Service)

### Actividad 3 — Implementar la Lógica de Aplicación (Service)

El caso de uso vive en la capa `application/service`. Implementa el puerto de entrada e invoca al puerto de salida. El servicio orquesta el flujo. Al inyectar el `LibroRepositoryPort` (la interfaz) en lugar del repositorio de Spring Data real, garantizamos que el caso de uso es agnóstico a la tecnología. Si mañana cambiamos MySQL por MongoDB, este código no sufrirá ninguna alteración.
```java
package com.librotech.application.service;

import com.librotech.application.ports.in.CrearLibroUseCase;
import com.librotech.application.ports.out.LibroRepositoryPort;
import com.librotech.domain.model.Libro;

// En arquitectura limpia pura no usaríamos @Service aquí, 
// pero por pragmatismo en Spring lo permitimos para la inyección.
import org.springframework.stereotype.Service; 

@Service
public class LibroUseCaseImpl implements CrearLibroUseCase {

    // Dependemos de la abstracción (Puerto), NO de la implementación JPA
    private final LibroRepositoryPort libroRepositoryPort;

    public LibroUseCaseImpl(LibroRepositoryPort libroRepositoryPort) {
        this.libroRepositoryPort = libroRepositoryPort;
    }

    @Override
    public Libro crearNuevoLibro(Libro libro) {
        if (libroRepositoryPort.existePorIsbn(libro.getIsbn())) {
            throw new RuntimeException("Regla de negocio: El ISBN ya existe");
        }
        return libroRepositoryPort.guardar(libro);
    }
}
```

---

### Actividad 4 — Construir los Adaptadores (Infrastructure)

**4.1 Adaptador de Persistencia (`infrastructure/adapters/out/persistence`)**

Aquí vive el código "sucio" (acoplado a JPA). Renombre su antigua entidad `Libro` a `LibroJpaEntity`. Este es el modelo exclusivo para la base de datos, altamente acoplado a Hibernate. Lo separamos deliberadamente del `Libro` de dominio para que cualquier peculiaridad de la base de datos (claves foráneas, joins, lazy loading) se quede contenida en este paquete de infraestructura.
```java
package com.librotech.infrastructure.adapters.out.persistence;

import jakarta.persistence.*;
// ... imports JPA ...

@Entity
@Table(name = "libros")
public class LibroJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    // ... campos ...
}
```

**4.2 Repositorio Spring Data**

El repositorio de Spring Data ahora solo interactúa con la `LibroJpaEntity`. Sigue funcionando exactamente igual, pero su alcance está restringido a la capa de infraestructura, lejos del núcleo de negocio.
```java
public interface SpringDataLibroRepository extends JpaRepository<LibroJpaEntity, Long> {
    boolean existsByIsbn(String isbn);
}
```

**4.3 Implementación del Adaptador (La Magia)**

El Adaptador implementa nuestro Puerto de Salida (`LibroRepositoryPort`). Es el traductor oficial: recibe el `Libro` limpio del dominio, lo mapea a `LibroJpaEntity`, invoca a Spring Data, y mapea la respuesta de vuelta al formato del dominio.
```java
package com.librotech.infrastructure.adapters.out.persistence;

import com.librotech.application.ports.out.LibroRepositoryPort;
import com.librotech.domain.model.Libro;
import org.springframework.stereotype.Component;

@Component
public class LibroPersistenceAdapter implements LibroRepositoryPort {

    private final SpringDataLibroRepository repository;
    private final LibroPersistenceMapper mapper; // Usaremos MapStruct

    public LibroPersistenceAdapter(SpringDataLibroRepository repository, LibroPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Libro guardar(Libro libro) {
        LibroJpaEntity entity = mapper.toJpaEntity(libro);
        LibroJpaEntity saved = repository.save(entity);
        return mapper.toDomainModel(saved);
    }

    @Override
    public boolean existePorIsbn(String isbn) {
        return repository.existsByIsbn(isbn);
    }
    
    // ... implementación buscarPorId ...
}
```

**4.4 Adaptador Web (Controlador REST)**

El controlador es un adaptador web ("Driving Adapter"). Transforma una petición HTTP (el DTO JSON) en un modelo de dominio puro, invoca el puerto de entrada (`CrearLibroUseCase`), y luego traduce la respuesta del dominio de vuelta a HTTP.
```java
package com.librotech.infrastructure.adapters.in.web;

import com.librotech.application.ports.in.CrearLibroUseCase;
// ... imports ...

@RestController
@RequestMapping("/api/libros")
public class LibroRestController {

    // Depende del puerto de entrada, no del servicio directamente
    private final CrearLibroUseCase crearLibroUseCase;
    private final LibroWebMapper webMapper; // Mapea DTO <-> Domain

    public LibroRestController(CrearLibroUseCase crearLibroUseCase, LibroWebMapper webMapper) {
        this.crearLibroUseCase = crearLibroUseCase;
        this.webMapper = webMapper;
    }

    @PostMapping
    public ResponseEntity<LibroResponseDTO> crear(@RequestBody LibroCreateDTO dto) {
        Libro dominio = webMapper.toDomain(dto);
        Libro guardado = crearLibroUseCase.crearNuevoLibro(dominio);
        return ResponseEntity.ok(webMapper.toResponseDto(guardado));
    }
}
```

---

## 🏋️ Retos para el Coder

### Reto 1 — Mappers Hexagonales con MapStruct
En este laboratorio omitimos el código de los mappers por brevedad. Su misión es crear las interfaces de MapStruct:
- `LibroPersistenceMapper` (traduce `Libro` de dominio a `LibroJpaEntity` y viceversa).
- `LibroWebMapper` (traduce `LibroCreateDTO` a `Libro` de dominio, y `Libro` a `LibroResponseDTO`).

### Reto 2 — Añadir un nuevo "Puerto de Salida"
Suponga que cada vez que se crea un libro, LibroTech debe enviar un email de notificación. Cree un puerto `EmailNotificationPort` en `application/ports/out`. Inyéctelo en su Caso de Uso (`LibroUseCaseImpl`). Finalmente, cree un adaptador "Dummy" (`infrastructure/adapters/out/email/DummyEmailAdapter.java`) que simplemente imprima un `System.out.println("Enviando email...")` e implemente la interfaz. ¡Observe cómo se conectan sin acoplarse!

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| El paquete `domain/model` no tiene ninguna importación a librerías de infraestructura | ☐ |
| Las clases JPA (`LibroJpaEntity`) residen en `infrastructure/adapters/out/persistence` | ☐ |
| Los casos de uso dependen exclusivamente de interfaces (`Ports`) | ☐ |
| Se implementaron los Adaptadores que conectan la Web y JPA a los puertos | ☐ |
| La compilación no arroja errores por ciclos de dependencia | ☐ |
