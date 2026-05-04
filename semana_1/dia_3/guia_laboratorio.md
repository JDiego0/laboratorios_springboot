# 🧱 Laboratorio Día 3: Arquitectura en Capas y Estructura Profesional

Hoy cerraremos la semana organizando nuestro código bajo el estándar de la industria, separando las responsabilidades y entendiendo cómo viajan los datos entre capas.

## 🎯 Objetivos de Aprendizaje
1.  Implementar la separación total de responsabilidades (Controller -> Service -> Repository).
2.  Introducir el concepto de **DTO (Data Transfer Object)** para desacoplar el modelo.
3.  Comprender los **Scopes de los Beans** (Singleton vs Prototype).

---

## 🛠️ Parte 1: El Repositorio (Simulando la Persistencia)
En lugar de tener la lista en el controlador, la moveremos a un componente especializado.

1.  Crear paquete `com.riwi.intro.repositories`.
2.  Clase `CoderRepository.java`:

```java
package com.riwi.intro.repositories;

import com.riwi.intro.models.Coder;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CoderRepository {
    private List<Coder> coders = new ArrayList<>();

    public List<Coder> findAll() {
        return coders;
    }

    public void save(Coder coder) {
        coders.add(coder);
    }
}
```

---

## 🏗️ Parte 2: El Servicio (Lógica de Negocio)
El servicio será el puente entre el controlador y el repositorio.

1.  Crear clase `CoderService.java` en el paquete de services.

```java
@Service
public class CoderService {
    private final CoderRepository repository;

    public CoderService(CoderRepository repository) {
        this.repository = repository;
    }

    public List<Coder> getAll() {
        return repository.findAll();
    }

    public Coder create(Coder coder) {
        // Aquí irían validaciones de negocio:
        if (coder.getName() == null || coder.getName().isEmpty()) {
            throw new RuntimeException("El nombre es obligatorio");
        }
        repository.save(coder);
        return coder;
    }
}
```

---

## 🔬 Parte 3: Scopes de Beans
Spring, por defecto, crea una sola instancia de cada clase (**Singleton**). Vamos a experimentar con esto.

1.  Añade un log o un `System.out.println` en el constructor de tu `CoderService`.
2.  Reinicia la app. Observa cuántas veces aparece el mensaje.
3.  Cambia la anotación a:
```java
@Service
@Scope("prototype")
public class CoderService { ... }
```
4.  Inyecta el servicio en dos controladores diferentes y observa qué pasa.

---

## 🧪 Parte 4: Desafío del Día
**Reto "Clean Controller":**
1.  Limpia tu `CoderController` del Día 2.
2.  Borra la lista `ArrayList` que tenía el controlador.
3.  Inyecta el `CoderService` y delega todas las operaciones a él.
4.  Asegúrate de que la API siga funcionando igual que ayer, pero ahora con una estructura profesional.

---

## 🔍 Temas de Investigación y Aprendizaje Continuo
La tecnología avanza rápido. Investiga estos temas para estar a la vanguardia:

1.  **Project Loom (Virtual Threads):** Spring Boot 3.2 introdujo soporte para hilos virtuales. ¿Cómo cambia esto la forma en que manejamos miles de peticiones simultáneas sin consumir tanta RAM?
2.  **GraalVM & Native Images:** Investiga cómo convertir una aplicación de Spring Boot en un archivo binario ejecutable que arranca en milisegundos.
3.  **Spring Boot 3.3 Features:** ¿Qué hay de nuevo en la última versión estable de Spring Boot? (Busca sobre "Service Connections" en Docker Compose).

---
> [!IMPORTANT]
> Has finalizado la Semana 1. Ya tienes las bases para construir cualquier aplicación en Spring. La próxima semana entraremos al mundo de las Bases de Datos reales.
