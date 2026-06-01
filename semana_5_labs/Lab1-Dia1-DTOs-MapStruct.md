# 🧪 LABORATORIO DÍA 1 — Arquitectura DTO y Mapeo Automático con MapStruct

**Módulo 6.1 · Semana 5 · Proyecto LibroTech 📚**  
**Duración estimada:** 3 horas  
**Nivel:** Avanzado  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Comprender la necesidad del patrón **Data Transfer Object (DTO)** para proteger el dominio.
2. Crear DTOs de entrada (`CreateDTO`) y salida (`ResponseDTO`) usando `Records` de Java.
3. Integrar **MapStruct** para automatizar la conversión bidireccional entre Entidades JPA y DTOs.
4. Refactorizar la capa de Servicio y Controlador para que solo expongan e ingieran DTOs, ocultando completamente la entidad `Libro`.

---

## 📖 Contexto de Negocio — LibroTech

En la Semana 4 logramos un gran avance: implementamos `LibroResumenDTO` para optimizar las consultas JPQL masivas del catálogo. Sin embargo, para operaciones individuales (como consultar el detalle de un libro con `findById` o crear un libro nuevo con un `POST`), seguimos exponiendo y recibiendo directamente la entidad JPA `Libro`.

Cuando el cliente envía un JSON para crear un libro, este se intenta mapear directamente a la entidad. Esto ha causado dos problemas graves:
1. **Acoplamiento de Seguridad:** Estamos exponiendo accidentalmente información interna al cliente y permitiendo inyección masiva de propiedades.
2. **Ciclos Infinitos:** Al intentar serializar un `Libro` que tiene una `Editorial` que a su vez tiene una lista de `Libros`, el sistema colapsa con `StackOverflowError` a menos que usemos `@JsonIgnore`, ensuciando el modelo de base de datos.

La solución es implementar el **Patrón DTO**.

---

## 📝 Actividades

### Actividad 1 — Configurar Dependencias de MapStruct

MapStruct es un generador de código en tiempo de compilación. Necesitamos agregar la librería y configurar el plugin del compilador de Maven (o Gradle).

**1.1 Edite su `pom.xml`:**

Para gestionar las versiones de forma centralizada y evitar incompatibilidades, definimos las versiones de MapStruct y el binding de Lombok en la sección `<properties>` de Maven. Esto asegura que si necesitamos actualizar en el futuro, solo cambiamos un número.

Añada las propiedades de versión (verifique usar las más recientes compatibles):
```xml
<properties>
    <org.mapstruct.version>1.5.5.Final</org.mapstruct.version>
    <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
</properties>
```

Añada la dependencia de MapStruct. Esta dependencia descarga la librería central, que contiene las anotaciones necesarias (como `@Mapper` o `@Mapping`) que utilizaremos en nuestras interfaces para definir las reglas de conversión:
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${org.mapstruct.version}</version>
</dependency>
```

**1.2 Configure el plugin `maven-compiler-plugin`:**
Como MapStruct genera código en tiempo de compilación y Lombok también modifica el código, pueden entrar en conflicto. El `lombok-mapstruct-binding` actúa como un puente para que MapStruct entienda los getters y setters generados por Lombok. Configuramos el `maven-compiler-plugin` en la sección `<build><plugins>` para ejecutar estos procesadores en el orden correcto:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>17</source>
        <target>17</target>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${org.mapstruct.version}</version>
            </path>
            <!-- Si usa Lombok, añada estos dos también -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>${lombok-mapstruct-binding.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

### Actividad 2 — Crear los DTOs usando Records

Los `Records` (introducidos en Java 14) son perfectos para los DTOs porque son inmutables y generan los constructores y getters automáticamente.

Cree un paquete `dto` y agregue:

**2.1 `LibroResponseDTO.java` (Lo que enviamos al cliente):**

Este DTO está diseñado específicamente para las respuestas. Note que no incluimos la entidad `Editorial` completa, sino que la "aplanamos" extrayendo únicamente el `nombreEditorial`. Esto reduce el peso del JSON y evita exponer información confidencial. Usar `record` nos garantiza inmutabilidad.
```java
package com.librotech.dto;

import java.time.LocalDate;
import java.util.Set;

public record LibroResponseDTO(
    Long id,
    String titulo,
    String autor,
    String isbn,
    LocalDate fechaPublicacion,
    Double precio,
    String nombreEditorial, // Aplanamos la relación: solo enviamos el nombre
    Set<String> generos // Solo los nombres de los géneros
) {}
```

**2.2 `LibroCreateDTO.java` (Lo que recibimos del cliente):**

Cuando el cliente desea crear un libro, no envía el objeto `Editorial` completo ni los objetos `Genero`. Solo necesitamos que nos envíe las llaves foráneas (`editorialId` y `generoIds`). Esto simplifica enormemente el payload y previene que el cliente intente modificar datos de otras tablas accidentalmente.
```java
package com.librotech.dto;

import java.time.LocalDate;
import java.util.Set;

public record LibroCreateDTO(
    String titulo,
    String autor,
    String isbn,
    LocalDate fechaPublicacion,
    Double precio,
    Long editorialId, // El cliente solo envía el ID de la editorial
    Set<Long> generoIds // El cliente envía los IDs de los géneros
) {}
```

---

### Actividad 3 — Implementar el Mapeador con MapStruct

Cree un paquete `mapper` y defina la interfaz `LibroMapper`. MapStruct generará la implementación por nosotros al compilar.

**3.1 Interfaz `LibroMapper.java`:**

MapStruct analiza esta interfaz y genera una clase que la implementa automáticamente. Usamos `componentModel = "spring"` para que la clase generada sea un `@Component` inyectable. Las anotaciones `@Mapping` nos permiten resolver discrepancias de nombres entre la Entidad y el DTO (ej. `editorial.nombre` hacia `nombreEditorial`), y `@Named` permite definir métodos auxiliares (como extraer nombres de una lista).
```java
package com.librotech.mapper;

import com.librotech.dto.LibroCreateDTO;
import com.librotech.dto.LibroResponseDTO;
import com.librotech.model.Libro;
import com.librotech.model.Genero;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring") // Indica a Spring que inyecte este mapper como un @Component
public interface LibroMapper {

    // === DE ENTIDAD A RESPONSE DTO ===
    @Mapping(source = "editorial.nombre", target = "nombreEditorial")
    @Mapping(source = "generos", target = "generos", qualifiedByName = "mapGenerosToNombres")
    LibroResponseDTO toResponseDTO(Libro libro);

    // Método auxiliar para extraer solo los nombres de los géneros
    @Named("mapGenerosToNombres")
    default Set<String> mapGenerosToNombres(Set<Genero> generos) {
        if (generos == null) return null;
        return generos.stream().map(Genero::getNombre).collect(Collectors.toSet());
    }

    // === DE CREATE DTO A ENTIDAD ===
    // Ignoramos el ID, disponibilidad y relaciones complejas (las manejaremos en el Service)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "disponible", constant = "true")
    @Mapping(target = "editorial", ignore = true)
    @Mapping(target = "generos", ignore = true)
    Libro toEntity(LibroCreateDTO dto);
}
```

> 💡 **Compile el proyecto (`mvn clean compile`)**. Revise la carpeta `target/generated-sources/annotations`. ¡MapStruct ha creado la clase `LibroMapperImpl`!

---

### Actividad 4 — Refactorizar Service y Controller

**4.1 `LibroService.java`**

El Servicio es el corazón de la Arquitectura Limpia. Modifique su servicio para que hable en el lenguaje de los DTOs. Ahora recibe un DTO, usa el Mapper para convertirlo en Entidad, resuelve manualmente las relaciones buscando la `Editorial` y los `Generos` en la base de datos usando los IDs, y finalmente guarda la entidad. El retorno vuelve a ser un DTO, asegurando que la capa web nunca toque una Entidad JPA.
```java
// ... imports ...

@Service
@RequiredArgsConstructor // Si usa Lombok para inyectar dependencias
public class LibroService {

    private final LibroRepository libroRepository;
    private final EditorialRepository editorialRepository;
    private final GeneroRepository generoRepository;
    private final LibroMapper libroMapper; // Inyectamos MapStruct

    public LibroResponseDTO obtenerPorId(Long id) {
        Libro libro = libroRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Libro no encontrado"));
        return libroMapper.toResponseDTO(libro);
    }

    @Transactional
    public LibroResponseDTO crearLibro(LibroCreateDTO dto) {
        // 1. Mapear DTO a Entidad base
        Libro libro = libroMapper.toEntity(dto);

        // 2. Resolver la Editorial desde la BD
        Editorial editorial = editorialRepository.findById(dto.editorialId())
            .orElseThrow(() -> new RuntimeException("Editorial no existe"));
        libro.setEditorial(editorial);

        // 3. Resolver los Géneros desde la BD
        Set<Genero> generos = generoRepository.findAllById(dto.generoIds()).stream().collect(Collectors.toSet());
        libro.setGeneros(generos);

        // 4. Guardar y retornar el Response DTO
        Libro libroGuardado = libroRepository.save(libro);
        return libroMapper.toResponseDTO(libroGuardado);
    }
}
```

**4.2 `LibroController.java`**

Gracias a los DTOs, el Controlador se vuelve un mero despachador. Solo recibe peticiones HTTP con formato JSON, las delega al Servicio y retorna la respuesta. Ya no hay riesgo de serialización infinita ni de exponer métodos de Hibernate al exterior.
```java
@RestController
@RequestMapping("/api/libros")
@RequiredArgsConstructor
public class LibroController {

    private final LibroService libroService;

    @GetMapping("/{id}")
    public ResponseEntity<LibroResponseDTO> obtenerLibro(@PathVariable Long id) {
        return ResponseEntity.ok(libroService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<LibroResponseDTO> crearLibro(@RequestBody LibroCreateDTO dto) {
        LibroResponseDTO nuevoLibro = libroService.crearLibro(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoLibro);
    }
}
```

---

## 🏋️ Retos para el Coder

### Reto 1 — Listado y Paginación con DTOs
Actualice su método `GET /api/libros` para que retorne `Slice<LibroResponseDTO>` en lugar de la entidad o el DTO antiguo. Pista: la interfaz `Slice` también posee un método `.map()` que le permitirá convertir cómodamente un `Slice<Libro>` a `Slice<LibroResponseDTO>` usando su Mapper, manteniendo el alto rendimiento logrado en la Semana 4.

### Reto 2 — DTO para Actualización
Cree `LibroUpdateDTO`. A diferencia del `CreateDTO`, algunos campos podrían ser omitidos al actualizar. Modifique el servicio y el controlador para el endpoint `PUT`. Indíquele a MapStruct que actualice una entidad existente usando `@MappingTarget`.

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| El controlador no importa ni expone la clase `Libro` | ☐ |
| Se utiliza `Record` para los DTOs de entrada y salida | ☐ |
| MapStruct compila correctamente generando `LibroMapperImpl` | ☐ |
| El `LibroResponseDTO` "aplana" el objeto `Editorial` devolviendo solo el nombre | ☐ |
| Se resuelven las entidades relacionadas (`Editorial`, `Genero`) en el Service a partir de los IDs enviados en el DTO | ☐ |
