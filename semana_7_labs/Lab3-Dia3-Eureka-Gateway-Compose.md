# 🧪 LABORATORIO DÍA 3 — Orquestación, Eureka y API Gateway

**Módulo 6.1 · Semana 7 · Proyecto LibroTech 📚**  
**Duración estimada:** 3 horas  
**Nivel:** Experto  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Levantar un Service Registry utilizando **Netflix Eureka**.
2. Configurar microservicios como Eureka Clients para el descubrimiento dinámico.
3. Crear un **API Gateway** centralizado con Spring Cloud Gateway para enrutar el tráfico.
4. Orquestar un ecosistema distribuido completo (5+ contenedores) utilizando **Docker Compose** y aislar redes por seguridad.

---

## 📖 Contexto de Negocio — LibroTech

El experimento de microservicios de ayer fue un éxito: dividimos LibroTech en `ui-service`, `libro-service` (y opcionalmente `editorial-service`). Sin embargo, surgió un problema grave: **las URLs están quemadas (hardcodeadas) en el código**.

El `ui-service` apunta a `http://localhost:8081`. ¿Qué pasa si desplegamos 3 instancias de `libro-service` en 3 servidores diferentes con IPs dinámicas para soportar la carga del Día del Libro? El `ui-service` colapsaría.

Para resolver esto en la nube, implementaremos **Service Discovery** con Eureka: los servicios se buscarán por nombre (ej. `LIBRO-SERVICE`), no por IP. Además, crearemos un **API Gateway**, la puerta de entrada única a la biblioteca. A partir de ahora, ningún cliente o navegador podrá hablar directamente con los microservicios; todos tendrán que pasar por el "portero" (Gateway).

---

## 📝 Actividades

### Actividad 1 — Crear el Service Registry (Eureka Server)

**1.1 Crear el proyecto `eureka-server`:**
Use Spring Initializr. Dependencia: **Eureka Server**.

**1.2 Habilitar el servidor:**
Abra la clase principal (`EurekaServerApplication`) y añada `@EnableEurekaServer`.

**1.3 Configurar `application.yml`:**

El servidor Eureka (Service Registry) actúa como un directorio telefónico dinámico. Los microservicios llamarán a este servidor para registrarse y anunciar su disponibilidad. Configuramos `register-with-eureka: false` porque, al ser él mismo el directorio, no necesita registrarse en su propia base de datos de servicios.
```yaml
server:
  port: 8761 # Puerto estandar de Eureka

spring:
  application:
    name: eureka-server

eureka:
  instance:
    hostname: localhost
  client:
    # Un servidor Eureka no necesita registrarse consigo mismo
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```

> 💡 **Pruebe:** Levante la aplicación y vaya a `http://localhost:8761`. Verá el Dashboard naranja de Spring Eureka.

---

### Actividad 2 — Configurar Clientes Eureka

Tanto el `libro-service` como el `ui-service` deben registrarse en Eureka.

**2.1 Agregar la dependencia en AMBOS proyectos (`pom.xml`):**

Al incluir el starter de Eureka Client, Spring Boot activa automáticamente un subproceso en segundo plano al arrancar la aplicación. Este subproceso se comunicará constantemente con el servidor Eureka (enviando 'heartbeats') para decirle: "Sigo vivo en esta IP y Puerto".
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**2.2 Configurar `application.yml` (Ejemplo para `libro-service`):**

El nombre de la aplicación (`spring.application.name`) es crucial en un ecosistema de microservicios. Este será el alias o 'dominio interno' con el que otros microservicios buscarán a esta aplicación, independientemente de en qué IP o puerto se encuentre ejecutándose actualmente.
```yaml
spring:
  application:
    name: libro-service # ¡ESTE ES EL NOMBRE CON EL QUE SE REGISTRARÁ!

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/ # URL del Eureka Server
```
*Repita el proceso para el `ui-service`.*

---

### Actividad 3 — Crear el API Gateway Central

El API Gateway será el puerto 8000. Recibirá todo el tráfico y lo redirigirá.

**3.1 Crear el proyecto `api-gateway`:**
Use Spring Initializr. Dependencias: **Gateway** y **Eureka Discovery Client**.

**3.2 Habilitar descubrimiento:**
Añada `@EnableDiscoveryClient` en la clase principal.

**3.3 Configurar las Rutas en `application.yml`:**

El API Gateway utiliza 'Routes' y 'Predicates' para decidir a dónde enviar el tráfico entrante. Al utilizar la sintaxis `lb://NOMBRE-SERVICIO`, le instruimos al Gateway que no use una URL estática, sino que consulte a Eureka (Load Balancer interno) para obtener una IP válida del servicio solicitado y redirija la petición hacia allá.
```yaml
server:
  port: 8000

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true # Habilita enrutamiento automático basado en Eureka
      routes:
        # RUTA 1: Llamadas a la API del backend
        - id: libro-service-route
          uri: lb://LIBRO-SERVICE # "lb://" significa Load Balancer (resuelve por Eureka)
          predicates:
            - Path=/api/libros/**
            
        # RUTA 2: Frontend UI
        - id: ui-service-route
          uri: lb://UI-SERVICE
          predicates:
            - Path=/** # Todo lo demás (HTML, CSS, JS) va al frontend
```

**3.4 Actualizar RestClient en el UI Service:**

Vaya a su `ui-service` y cambie la URL quemada (`http://localhost:8081`). Ahora las peticiones deben hacerse hacia el Gateway, o resolverse directamente vía Eureka. Gracias al balanceo de carga automático provisto por Spring Cloud LoadBalancer, ya no necesitamos conocer la topología de red. Invocamos servicios usando su alias, y Spring resuelve la IP dinámicamente antes de enviar la petición HTTP. Si configuró RestClient con balanceo de carga (`@LoadBalanced`), la URL será simplemente:
`backend.libro-service.url=http://LIBRO-SERVICE`

---

### Actividad 4 — Orquestación Global con Docker Compose

Es imposible manejar 5 terminales abiertas a la vez. Crearemos el `docker-compose.yml` maestro.

Asegúrese de generar el `.jar` de los 4 proyectos (`mvn clean package -DskipTests`) y tener un `Dockerfile` estándar en cada carpeta. La orquestación de un ecosistema distribuido completo requiere un orden de inicio (`depends_on`), redes aisladas (`networks`) y variables de entorno dinámicas. Observe cómo los microservicios se comunican entre sí utilizando los nombres de los servicios Docker como hostnames internos (ej. `http://eureka-server:8761`). Además, limitamos drásticamente los puertos expuestos al host físico para maximizar la seguridad.

Cree el archivo maestro en un directorio padre:
```yaml
version: '3.8'

services:
  # 1. Base de datos
  db-libros:
    image: postgres:15
    environment:
      POSTGRES_DB: librotech
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
    networks:
      - libro-net

  # 2. Service Registry
  eureka-server:
    build: ./eureka-server
    ports:
      - "8761:8761" # Expuesto temporalmente para ver el dashboard
    networks:
      - libro-net

  # 3. Backend de Negocio (Múltiples instancias posibles)
  libro-service:
    build: ./libro-service
    depends_on:
      - eureka-server
      - db-libros
    environment:
      # Ocultamos la DB en la red de docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://db-libros:5432/librotech
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    networks:
      - libro-net

  # 4. Frontend Web (BFF)
  ui-service:
    build: ./ui-service
    depends_on:
      - eureka-server
      - api-gateway
    environment:
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      # Apunta al Gateway interno
      BACKEND_LIBRO-SERVICE_URL: http://api-gateway:8000
    networks:
      - libro-net

  # 5. Punto de entrada (Gateway)
  api-gateway:
    build: ./api-gateway
    depends_on:
      - eureka-server
    ports:
      - "8000:8000" # EL ÚNICO PUERTO DE LA API VISIBLE PARA EL MUNDO EXTERIOR
    environment:
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    networks:
      - libro-net

networks:
  libro-net:
    driver: bridge
```

> ⚠️ **Atención de Seguridad:** Observe que `libro-service` y `db-libros` NO tienen sección `ports:`. Están **totalmente aislados** del mundo exterior. Solo pueden ser accedidos desde dentro de la red Docker a través del `api-gateway` o del `ui-service`.

---

## 🏋️ Retos para el Coder

### Reto 1 — Filtro Global en el Gateway
En lugar de depender de que cada microservicio valide el JWT exhaustivamente, implemente un `GlobalFilter` en el `api-gateway`. Este filtro debe interceptar todas las peticiones hacia `/api/**`, extraer el JWT del header `Authorization`, validarlo (verificando la firma y que no haya expirado), e interrumpir la petición con HTTP 401 si es inválido, ANTES de que el tráfico llegue al `libro-service`.

### Reto 2 — Escalar el Backend
Modifique su comando de docker-compose para levantar **3 instancias** del `libro-service`:
`docker-compose up -d --scale libro-service=3`.
Vaya al Dashboard de Eureka y observe cómo aparecen las 3 réplicas. Haga peticiones al Gateway y revise los logs para notar cómo el "Load Balancer" (lb://) distribuye automáticamente las peticiones entre los 3 contenedores (Round Robin).

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| Eureka Server está activo en el puerto 8761 y muestra el dashboard | ☐ |
| API Gateway, UI Service y Libro Service se registran exitosamente en Eureka | ☐ |
| Las peticiones hacia `http://localhost:8000/api/libros` son enrutadas por el Gateway al backend correspondiente | ☐ |
| El archivo `docker-compose.yml` orquesta todo el ecosistema de 5 contenedores | ☐ |
| Los servicios internos de base de datos y backend NO exponen puertos al host (solo el gateway y opcionalmente eureka lo hacen) | ☐ |
