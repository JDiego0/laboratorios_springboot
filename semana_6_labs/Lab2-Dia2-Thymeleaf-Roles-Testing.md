# 🧪 LABORATORIO DÍA 2 — Autorización en UI (Thymeleaf) y Testing Profesional

**Módulo 6.1 · Semana 6 · Proyecto LibroTech 📚**  
**Duración estimada:** 3 horas  
**Nivel:** Avanzado  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Restringir endpoints de la API usando `@PreAuthorize` según el rol único del usuario (`Enum`).
2. Integrar **Thymeleaf Extras Spring Security 6** para renderizar componentes de interfaz dinámicamente según el usuario logueado.
3. Testear la capa Web y la Seguridad utilizando `MockMvc` y `@WithMockUser`.
4. Implementar pruebas de integración contra una base de datos real utilizando **Testcontainers**.

---

## 📖 Contexto de Negocio — LibroTech

Tenemos el JWT y las contraseñas encriptadas. Un bibliotecario puede iniciar sesión. Sin embargo, un usuario con el rol de `LECTOR` todavía puede enviar una petición `DELETE` a `/api/libros/1` o ver el botón "Descatalogar Libro" en el panel web.

Necesitamos imponer **Autorización Basada en Roles (RBAC)** en ambas capas:
- **Backend (API):** Rechazar la petición si el JWT no contiene el rol adecuado.
- **Frontend (UI):** Ocultar visualmente las acciones destructivas para evitar que un lector intente darle clic a un botón que no le corresponde.

Finalmente, el equipo de QA requiere validar que el sistema funciona perfectamente mediante pruebas automatizadas, pero han prohibido el uso de la base de datos en memoria H2 porque "no se comporta igual que nuestro PostgreSQL de producción".

---

## 📝 Actividades

### Actividad 1 — Autorización a Nivel de Método (@PreAuthorize)

**1.1 Habilitar la seguridad de métodos**

Para proteger métodos individuales, Spring Security necesita ser configurado explícitamente. La anotación `@EnableMethodSecurity` activa el procesamiento de anotaciones como `@PreAuthorize` o `@Secured` en toda la aplicación. Abra su clase `SecurityConfig` y agréguele la anotación correspondiente:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // <--- Habilita el uso de @PreAuthorize
public class SecurityConfig { ... }
```

**1.2 Proteger el Controlador de Libros**

Al usar `@PreAuthorize`, la expresión se evalúa antes de que el método siquiera comience a ejecutarse. Si la condición es falsa (el usuario no tiene el rol), Spring lanza un `AccessDeniedException` que resulta en un HTTP 403 Forbidden. Esto proporciona un control de acceso de grano fino. Vaya a `LibroController` (o a su servicio) e indique quién puede ejecutar cada acción. Note que Spring espera el prefijo `ROLE_` por debajo, pero `@PreAuthorize` lo asume si usamos `hasRole()`.
```java
@RestController
@RequestMapping("/api/libros")
public class LibroController {

    // Cualquier usuario logueado puede ver libros
    @GetMapping
    public ResponseEntity<?> listarLibros() { ... }

    // Solo los administradores y bibliotecarios pueden crear
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
    public ResponseEntity<?> crearLibro(...) { ... }

    // Solo un ADMIN puede descatalogar (borrado lógico)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> descatalogarLibro(@PathVariable Long id) { ... }
}
```

---

### Actividad 2 — Interfaz Dinámica con Thymeleaf Extras

**2.1 Dependencia en `pom.xml`:**

Thymeleaf por sí solo no entiende de usuarios autenticados. Necesitamos el módulo de integración oficial `thymeleaf-extras-springsecurity6`, el cual nos proporciona dialectos especiales (como `sec:authorize`) para evaluar el contexto de seguridad directamente desde la plantilla HTML.
```xml
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```

**2.2 Modificar las vistas HTML:**

El namespace `sec` nos permite condicionar el renderizado del DOM. En lugar de enviar un HTML que oculta elementos vía CSS (lo cual es inseguro porque un atacante podría quitar el `display: none`), Thymeleaf evaluará la regla de seguridad en el servidor. Si la regla no se cumple, el nodo HTML simplemente no se incluye en la respuesta enviada al navegador. Abra su archivo principal (ej. `listado.html`). Asegúrese de incluir el *namespace* de seguridad en la etiqueta `<html>`:
```html
<html xmlns:th="http://www.thymeleaf.org" 
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

Usando `sec:authentication`, podemos extraer propiedades directamente del objeto `Authentication` inyectado en el SecurityContext, como el nombre de usuario o los roles, para crear un perfil dinámico en la barra de navegación superior:
```html
<nav class="navbar navbar-dark bg-dark">
    <div class="container-fluid">
        <span class="navbar-brand">LibroTech Admin</span>
        
        <!-- Renderizar solo si el usuario está autenticado -->
        <div sec:authorize="isAuthenticated()" class="text-white">
            Hola, <span sec:authentication="name">Usuario</span> 
            [<span sec:authentication="principal.authorities">Roles</span>]
            
            <form th:action="@{/logout}" method="post" class="d-inline">
                <button class="btn btn-sm btn-outline-light ms-2">Salir</button>
            </form>
        </div>
    </div>
</nav>
```

Aquí aplicamos el control de acceso visual en la tabla de libros. El atributo `sec:authorize` evalúa expresiones de Spring Expression Language (SpEL). Al coincidir con las reglas de `@PreAuthorize` del backend, garantizamos que el frontend y el backend estén sincronizados en sus restricciones:
```html
<tbody>
    <tr th:each="libro : ${libros}">
        <td th:text="${libro.titulo}"></td>
        <td>
            <!-- Visible para ADMIN y BIBLIOTECARIO -->
            <a sec:authorize="hasAnyRole('ADMIN', 'BIBLIOTECARIO')" 
               class="btn btn-warning btn-sm">Editar</a>
            
            <!-- Visible SOLAMENTE para ADMIN -->
            <button sec:authorize="hasRole('ADMIN')" 
                    class="btn btn-danger btn-sm">Descatalogar</button>
        </td>
    </tr>
</tbody>
```

---

### Actividad 3 — Testing de Seguridad (MockMvc)

Vamos a probar nuestra API sin necesidad de levantar el servidor.

**3.1 Test de Autorización en `LibroControllerTest`:**

Las pruebas de seguridad requieren simular el contexto de autenticación. Usando `@WebMvcTest`, cargamos únicamente la capa web. La anotación `@WithMockUser` es clave aquí: inyecta un `Authentication` falso en el `SecurityContext` con los roles especificados, permitiéndonos probar las reglas de autorización sin tener que generar tokens JWT reales ni tocar la base de datos.
```java
@WebMvcTest(LibroController.class)
// Importamos la configuración de seguridad para que el test la use
@Import(SecurityConfig.class) 
class LibroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LibroService libroService;
    
    // Suponemos que también necesitamos mockear el JwtAuthFilter u otras dependencias de seguridad

    @Test
    @WithMockUser(roles = "LECTOR") // Simulamos un usuario con rol LECTOR
    void lectorNoPuedeEliminarLibro_DebeRetornar403() throws Exception {
        mockMvc.perform(delete("/api/libros/1"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN") // Simulamos un administrador
    void adminPuedeEliminarLibro_DebeRetornar200() throws Exception {
        // Configuramos el mock
        doNothing().when(libroService).descatalogarLibro(1L);

        mockMvc.perform(delete("/api/libros/1"))
               .andExpect(status().isOk());
    }
}
```

---

### Actividad 4 — Pruebas de Integración con Testcontainers

H2 no es confiable para pruebas de base de datos serias. Testcontainers levanta un contenedor Docker real (MySQL o PostgreSQL) al ejecutar los tests, aplica Flyway y lo destruye al terminar.

**4.1 Dependencias `pom.xml`:**

Testcontainers requiere que Docker esté instalado. La dependencia base proporciona el motor de orquestación, mientras que la dependencia específica (ej. `mysql`) provee la clase de contenedor preconfigurada para esa base de datos.
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <!-- Use mysql o postgresql según su proyecto -->
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
</dependency>
```

**4.2 Clase de Test de Integración:**

Esta es una prueba de integración verdadera (`@SpringBootTest`). Testcontainers descarga la imagen oficial de la base de datos, levanta el contenedor, expone un puerto aleatorio, y mediante `@DynamicPropertySource` inyectamos la URL de conexión generada directamente en las propiedades de Spring, logrando un entorno de pruebas idéntico a producción.
```java
@SpringBootTest
@Testcontainers // Habilita el ciclo de vida de Testcontainers
class LibroRepositoryIntegrationTest {

    // Levanta un contenedor MySQL dinámicamente antes de los tests
    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("librotech_test")
            .withUsername("test")
            .withPassword("test");

    // Inyecta dinámicamente las credenciales del contenedor en el contexto de Spring
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        // Flyway ejecutará automáticamente los scripts V1, V2, V3 aquí
    }

    @Autowired
    private LibroRepository libroRepository;

    @Test
    void testGuardarLibroYVerificarBaseDeDatosReal() {
        // Compruebe que la BD arrancó correctamente
        assertTrue(mysqlContainer.isRunning());

        // El test accederá a la base de datos real del contenedor
        long conteoInicial = libroRepository.count(); // Ya tendrá datos por Flyway (V3)
        assertTrue(conteoInicial > 0, "Flyway debió insertar el seed de datos");
    }
}
```

> ⚠️ **Requisito:** Docker debe estar corriendo en su máquina local para que las pruebas pasen.

---

## 🏋️ Retos para el Coder

### Reto 1 — Manejo Expresivo de Errores Web
Si un usuario autenticado (`LECTOR`) intenta forzar la URL de una vista de administrador (Ej. `GET /admin/usuarios`), Spring devolverá un "Whitelabel 403 Forbidden". Configure Spring Security (usando `exceptionHandling().accessDeniedPage("/acceso-denegado")`) para que lo redirija a una plantilla Thymeleaf personalizada y amigable.

### Reto 2 — Mockear JWT en Pruebas Completas
Las pruebas de `@WebMvcTest` con `@WithMockUser` se saltan la validación del JWT real. Intente escribir un test usando `@SpringBootTest` (que carga toda la app) donde genere un JWT real usando su `JwtService`, lo añada al header de la petición `MockMvc` y verifique la respuesta.

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| Se utiliza `@PreAuthorize` para proteger al menos un método del controlador | ☐ |
| Las vistas de Thymeleaf muestran u ocultan elementos usando el namespace de seguridad | ☐ |
| Las peticiones con roles insuficientes retornan HTTP 403 | ☐ |
| Existe un test con `@WebMvcTest` validando el rol | ☐ |
| Testcontainers levanta exitosamente una base de datos real, corre Flyway y ejecuta el test | ☐ |
