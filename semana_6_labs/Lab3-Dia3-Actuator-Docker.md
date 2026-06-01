# 🧪 LABORATORIO DÍA 3 — Observabilidad, Actuator y Docker Multi-Stage

**Módulo 6.1 · Semana 6 · Proyecto LibroTech 📚**  
**Duración estimada:** 3 horas  
**Nivel:** Avanzado  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Integrar **Spring Boot Actuator** para exponer métricas y estado de salud de la aplicación.
2. Formatear las métricas de Actuator con **Micrometer** para su consumo en sistemas como Prometheus.
3. Crear un **Dockerfile Multi-Stage** optimizado que separe la compilación (Maven) de la ejecución (JRE).
4. Orquestar la aplicación Spring Boot y la base de datos MySQL/PostgreSQL usando **Docker Compose** en una red interna segura.

---

## 📖 Contexto de Negocio — LibroTech

El código de LibroTech está listo, seguro y testeado. Ha llegado el momento del despliegue a los servidores de producción (o la nube).
Sin embargo, el equipo de Infraestructura y Operaciones (DevOps) de la biblioteca nos ha devuelto la aplicación con dos bloqueos principales:

1. **La aplicación es una "caja negra":** Operaciones necesita saber si la aplicación está viva (`health check`), si puede conectarse a la base de datos, y métricas como memoria usada y tiempo de respuesta de los endpoints (`metrics`).
2. **Dependencia del entorno:** El equipo no instalará Java ni Maven en los servidores de producción. La aplicación debe ser entregada como un contenedor (Docker) independiente, que incluya su propio entorno de ejecución sin depender del servidor host, y todo debe arrancar con un solo comando junto con la base de datos.

---

## 📝 Actividades

### Actividad 1 — Actuator y Métricas Prometheus

**1.1 Agregar las dependencias al `pom.xml`:**

Spring Boot Actuator proporciona endpoints de grado de producción integrados. Micrometer es una fachada de métricas (como SLF4J lo es para los logs) que formatea los datos de Actuator en el formato específico que requiere Prometheus, el estándar de la industria para monitorización.
```xml
<!-- Observabilidad y Endpoints de Salud -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<!-- Formateador de métricas estándar de industria -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**1.2 Configurar `application.properties` (o `.yml`):**

Por seguridad, Spring Boot oculta casi toda la información de Actuator. Aquí instruimos explícitamente qué endpoints exponer a nivel web y configuramos detalles personalizados de nuestra aplicación. Habilitar la exposición total (`*`) es una mala práctica a menos que la red esté altamente securizada.
```properties
# Exponer endpoints específicos (nunca exponer todo con '*' en producción sin seguridad)
management.endpoints.web.exposure.include=health,info,prometheus,metrics

# Mostrar detalles de salud siempre y cuando se tenga permiso
management.endpoint.health.show-details=always

# Datos personalizados para el endpoint /actuator/info
info.app.name=LibroTech Library System
info.app.description=Plataforma de gestion del catalogo de LibroTech
info.app.version=1.0.0
```

**1.3 Proteger Actuator (Requisito Crítico)**

Exponer las métricas públicamente es un fallo de seguridad (puede revelar versiones de librerías y estructuras internas). Una API expuesta sin control es el primer vector de ataque. Por ello, delegamos la protección de estos endpoints a nuestro filtro JWT ya existente en `SecurityConfig`, asegurando que solo personal autorizado (ADMIN) pueda acceder a las métricas.
```java
http.authorizeHttpRequests(auth -> auth
    // ... sus otras reglas ...
    .requestMatchers("/actuator/**").hasRole("ADMIN") // Solo administradores
    .anyRequest().authenticated()
)
```

> 💡 **Pruebe:** Inicie sesión como ADMIN, obtenga su token, y haga un GET a `http://localhost:8080/actuator/health`. Verá detalles como la disponibilidad de la base de datos o el disco duro.

---

### Actividad 2 — Escribir el Dockerfile Multi-Stage

Para no incluir Maven y el código fuente en la imagen final de Docker (lo que la haría inmensa e insegura), usaremos dos etapas: "builder" y "runtime".

El patrón Multi-Stage consiste en usar múltiples instrucciones `FROM`. La primera etapa, pesada y con herramientas de desarrollo (JDK, Maven), compila el código. La segunda etapa, extremadamente ligera y sin herramientas (solo JRE), copia únicamente el archivo `.jar` resultante de la primera. Esto reduce drásticamente la superficie de ataque y el peso de la imagen.

Cree un archivo llamado `Dockerfile` (sin extensión) en la raíz del proyecto (junto a `pom.xml`).
```dockerfile
# ==========================================
# STAGE 1: Construcción (Builder)
# ==========================================
# Usamos una imagen que tenga Maven y el JDK
FROM maven:3.9-eclipse-temurin-17 AS builder

# Creamos y nos movemos a la carpeta de trabajo
WORKDIR /app

# Copiamos SOLO el pom.xml primero para descargar dependencias
# Esto optimiza el caché de Docker (no descarga todo si no cambia el pom)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamos el resto del código fuente
COPY src ./src

# Compilamos y generamos el JAR, omitiendo los tests para acelerar
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Ejecución (Runtime)
# ==========================================
# Usamos una imagen muy ligera que SOLO tiene JRE (Java Runtime Environment)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiamos el JAR generado desde la fase "builder" a esta imagen
COPY --from=builder /app/target/*.jar librotech-app.jar

# Exponemos el puerto de la aplicación
EXPOSE 8080

# Comando que se ejecutará al iniciar el contenedor
ENTRYPOINT ["java", "-jar", "librotech-app.jar"]
```

---

### Actividad 3 — Orquestar el Ecosistema con Docker Compose

La aplicación necesita su base de datos para funcionar. `docker-compose` permite levantar ambos con un solo comando. Docker Compose orquesta múltiples contenedores, definiendo servicios, redes y volúmenes persistentes. El bloque `app` indica cómo construir nuestra imagen Spring Boot, mientras que `db` descarga una base de datos oficial. Lo más importante es que ambos se comunican internamente a través de una red aislada, usando el nombre del servicio (`db`) como host.

Cree el archivo `docker-compose.yml` en la raíz del proyecto.
```yaml
version: '3.8'

services:
  # ==========================================
  # SERVICIO: BASE DE DATOS
  # ==========================================
  db:
    image: mysql:8.0 # Cambie a postgres:15 si usa PostgreSQL
    container_name: librotech_db
    environment:
      # Use POSTGRES_USER, etc si es postgres
      MYSQL_DATABASE: librotech
      MYSQL_USER: user_librotech
      MYSQL_PASSWORD: pass_secreto
      MYSQL_ROOT_PASSWORD: pass_root_secreto
    ports:
      - "3306:3306" # Mapeo puerto_host:puerto_contenedor
    volumes:
      - db_data:/var/lib/mysql # Persistencia de datos
    networks:
      - librotech-network

  # ==========================================
  # SERVICIO: APLICACIÓN SPRING BOOT
  # ==========================================
  app:
    build: . # Construye usando el Dockerfile de esta carpeta
    container_name: librotech_app
    ports:
      - "8080:8080"
    depends_on:
      - db # Espera a que el contenedor 'db' inicie primero
    environment:
      # Sobrescribe las variables de application.properties
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/librotech # El host es el nombre del servicio 'db'
      SPRING_DATASOURCE_USERNAME: user_librotech
      SPRING_DATASOURCE_PASSWORD: pass_secreto
      JWT_SECRET: EstaClaveVieneDesdeDockerComposeYDebeSerMasDe256bits
    networks:
      - librotech-network

volumes:
  db_data: # Define el volumen nombrado para que los datos sobrevivan reinicios

networks:
  librotech-network:
    driver: bridge # Red interna aislada
```

---

### Actividad 4 — Despliegue de la Aplicación

1. Detenga cualquier servicio MySQL local que tenga en el puerto 3306 y apague su IDE (Stop).
2. Abra una terminal en la raíz de su proyecto.
3. Ejecute: `docker-compose up -d --build`
   * `-d`: (Detached) Ejecuta en segundo plano.
   * `--build`: Fuerza a leer el Dockerfile y compilar el JAR.
4. Revise los logs de la aplicación usando: `docker-compose logs -f app`
5. Verificará cómo Flyway entra en acción y migra la base de datos recién creada.
6. Abra `http://localhost:8080` en su navegador. ¡LibroTech está vivo desde contenedores!

---

## 🏋️ Retos para el Coder

### Reto 1 — Cambiar Puertos Host vs Contenedor
Cambie la configuración en el `docker-compose.yml` para que la aplicación exponga en el puerto `9090` del *localhost*, pero sin modificar nada dentro del código Spring Boot (que seguirá corriendo en el 8080 internamente).

### Reto 2 — Custom Actuator Health Indicator (Avanzado)
Actuator permite crear "HealthChecks" propios. Crea una clase que implemente `HealthIndicator` que simule revisar la conexión con una API externa imaginaria (Ej. "Servicio Nacional de ISBN"). Si la conexión "falla" (puede simularlo con un random o un boolean), `/actuator/health` debe mostrar el estado general como "DOWN" o "OUT_OF_SERVICE".

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| El endpoint `/actuator/health` expone detalles de DB y Disco | ☐ |
| Las rutas de Actuator están protegidas y exigen Rol ADMIN | ☐ |
| El Dockerfile usa dos stages (`builder` y `runtime`) y minimiza el tamaño | ☐ |
| El `docker-compose.yml` sobrescribe la URL de BD (`jdbc:mysql://db...`) correctamente | ☐ |
| El entorno levanta limpiamente con un solo comando y aplica migraciones | ☐ |
