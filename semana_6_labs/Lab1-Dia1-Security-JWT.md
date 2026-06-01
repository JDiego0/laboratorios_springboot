# 🧪 LABORATORIO DÍA 1 — Autenticación Stateless (JWT) y Gestión de Rol Único

**Módulo 6.1 · Semana 6 · Proyecto LibroTech 📚**  
**Duración estimada:** 3 horas  
**Nivel:** Avanzado  

---

## 📋 Objetivo del Laboratorio

Al finalizar esta práctica, el estudiante será capaz de:

1. Integrar **Spring Security 6** en un proyecto existente.
2. Definir una entidad de `Usuario` con un **Rol único** implementado como un `Enum`.
3. Implementar un proveedor de usuarios (`UserDetailsService`) validado contra la base de datos con contraseñas encriptadas (`BCrypt`).
4. Generar **JSON Web Tokens (JWT)** para autenticación Stateless.
5. Devolver el JWT en una cabecera segura (`HttpOnly Cookie`) para proteger el frontend web contra ataques XSS.

---

## 📖 Contexto de Negocio — LibroTech

El sistema de gestión de bibliotecas LibroTech es público. Cualquiera con acceso a la URL `/admin/libros` puede crear o descatalogar libros. Esto es un riesgo masivo de seguridad.

La directiva ha decidido que LibroTech debe tener un sistema de seguridad híbrido:
- **API Externa:** Ciertas aplicaciones (ej. kioscos interactivos) consumirán la API REST y deberán enviar un token JWT en el header.
- **Panel Web (Thymeleaf):** Los bibliotecarios usarán el panel web. Como Thymeleaf no es una SPA (Single Page Application) tipo React/Angular, no puede almacenar fácilmente un JWT en memoria. Además, almacenarlo en *LocalStorage* es vulnerable a robo por scripts maliciosos (XSS).

**La Solución:** Implementaremos JWT para todos. Pero, cuando un usuario inicie sesión desde la web, el servidor guardará el JWT dentro de una **Cookie HttpOnly**. El navegador enviará esta cookie automáticamente en cada petición de forma segura, y nuestro filtro de Spring Security la interceptará como si fuera un token normal.

---

## 📝 Actividades

### Actividad 1 — Configurar el Modelo de Seguridad (Usuario y Enum)

**1.1 Agregar las dependencias al `pom.xml`:**

Spring Security es el motor principal que interceptará las peticiones. La librería `jjwt` (Java JWT) nos proporciona las herramientas criptográficas para generar, firmar y parsear los tokens JWT siguiendo el estándar RFC 7519.
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<!-- Librería para manejar JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

**1.2 Crear el Enum `Rol`:**

En lugar de usar una tabla separada para roles, un `Enum` es la forma más limpia y eficiente de manejar un Rol único por usuario. Limita las opciones posibles en tiempo de compilación y evita costosos JOINs en la base de datos.
```java
package com.librotech.model;

public enum Rol {
    LECTOR,         // Solo puede ver el catálogo
    BIBLIOTECARIO,  // Puede crear/editar libros
    ADMIN           // Acceso total
}
```

**1.3 Crear la Entidad `Usuario`:**

Al implementar la interfaz `UserDetails`, convertimos nuestra entidad JPA en un ciudadano de primera clase para Spring Security. El método más importante es `getAuthorities()`, donde transformamos nuestro Enum `Rol` en un `GrantedAuthority` que Spring entiende (agregándole el prefijo 'ROLE_').
```java
package com.librotech.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    // === MÉTODOS DE USERDETAILS ===

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security requiere que los roles tengan el prefijo "ROLE_"
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getUsername() { return email; } // Usaremos el email como username
    
    @Override
    public String getPassword() { return password; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
    
    // Getters y Setters estándar...
}
```

**1.4 Crear el Repositorio y el UserDetailsService:**

El `UserDetailsService` es el puente entre Spring Security y nuestra base de datos. Cuando un usuario intente iniciar sesión, Spring llamará al método `loadUserByUsername` pasando el email ingresado, y nosotros debemos devolver el objeto `UserDetails` (nuestra entidad `Usuario`) correspondiente.
```java
// Repositorio
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
}

// Servicio
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + username));
    }
}
```

---

### Actividad 2 — Generación del JWT (`JwtService`)

El JWT consta de tres partes: Header, Payload (Claims) y Signature. En esta clase utilizamos el patrón Builder de `jjwt` para ensamblar el token. Inyectamos los datos del usuario (como su email y rol) en los Claims, y finalmente lo firmamos digitalmente usando un algoritmo HMAC SHA-256 (HS256) y nuestra clave secreta. Solo el servidor, que conoce la clave secreta, podrá generar o validar estos tokens.
```java
package com.librotech.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;

@Service
public class JwtService {

    // Secreto estático para el laboratorio (En producción, póngalo en application.properties)
    private static final String SECRET = "EstaEsUnaClaveSuperSecretaParaLibroTechQueDebeMedirMasDe256Bits";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10 horas

    public String generarToken(UserDetails userDetails) {
        // Aquí puede insertar Claims personalizados, como el Rol
        HashMap<String, Object> claims = new HashMap<>();
        String rol = userDetails.getAuthorities().iterator().next().getAuthority();
        claims.put("rol", rol);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extraerUsername(String token) {
        return extraerTodosLosClaims(token).getSubject();
    }

    public boolean esTokenValido(String token, UserDetails userDetails) {
        final String username = extraerUsername(token);
        return (username.equals(userDetails.getUsername())) && !esTokenExpirado(token);
    }

    private boolean esTokenExpirado(String token) {
        return extraerTodosLosClaims(token).getExpiration().before(new Date());
    }

    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody();
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }
}
```

---

### Actividad 3 — Implementar la Autenticación (Endpoint y Cookie)

Este es el único endpoint público que interactúa con las contraseñas. Tras validar que las credenciales son correctas (usando el `AuthenticationManager`), generamos el JWT. La magia de seguridad ocurre al crear la `Cookie`: al marcarla como `HttpOnly(true)`, el navegador web prohibirá que cualquier script de JavaScript (incluso el nuestro) la lea. Esto mitiga por completo los ataques XSS (Cross-Site Scripting) orientados al robo de tokens.
```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletResponse response) {
        // 1. Validar credenciales
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // 2. Cargar usuario y generar token
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String jwtToken = jwtService.generarToken(userDetails);

        // 3. Crear Cookie HttpOnly segura
        Cookie jwtCookie = new Cookie("jwt", jwtToken);
        jwtCookie.setHttpOnly(true); // JavaScript NO puede leerla (Previene XSS)
        jwtCookie.setSecure(false);  // Solo true si usamos HTTPS (false en localhost)
        jwtCookie.setPath("/");      // Disponible en toda la aplicación
        jwtCookie.setMaxAge(3600);   // Expira en 1 hora

        response.addCookie(jwtCookie);

        // Retornar éxito
        return ResponseEntity.ok().body("Autenticación exitosa. Cookie configurada.");
    }
}

// Record auxiliar
record AuthRequest(String email, String password) {}
```

---

### Actividad 4 — El Filtro y la Cadena de Seguridad

Para que todo funcione, Spring necesita:
1. Extraer el JWT de la petición (del header o de la cookie).
2. Establecer el `SecurityFilterChain` como *STATELESS*.

**4.1 El filtro (`JwtAuthFilter`)**
Deberá crear un `OncePerRequestFilter` que extraiga la cookie "jwt" mediante `request.getCookies()`, valide el token usando `JwtService` y pueble el `SecurityContextHolder`. *(Esta implementación se deja como Reto 1).*

**4.2 La Configuración (`SecurityConfig`)**

La clase de configuración define la política de seguridad global. Deshabilitamos CSRF (ya que usaremos tokens), marcamos los endpoints públicos (`/api/auth/**`), y lo más importante: configuramos la política de creación de sesiones como `STATELESS`. Esto le dice a Spring que no guarde el estado del usuario en la memoria del servidor (JSESSIONID), obligando a que cada petición sea autenticada mediante nuestro filtro JWT.
```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Deshabilitamos CSRF para la API
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/css/**", "/js/**").permitAll() // Rutas públicas
                .anyRequest().authenticated() // El resto requiere autenticación
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

---

## 🏋️ Retos para el Coder

### Reto 1 — Implementar el `JwtAuthFilter`
Investigue y programe la clase `JwtAuthFilter` extendiendo de `OncePerRequestFilter`. Debe iterar sobre `request.getCookies()`, buscar la cookie llamada `"jwt"`, extraer su valor y verificar si es válido. Si no hay cookie, verifique si hay un header `Authorization: Bearer`.

### Reto 2 — Script Flyway (V4)
Como Spring Security requiere contraseñas encriptadas, no puede insertar contraseñas en texto plano. Cree un script Flyway que inserte a un usuario "admin@librotech.com" con el Enum `ADMIN`. *Pista: Genere el hash BCrypt de "admin123" usando un testeador online e insértelo directamente en el SQL.*

---

## ✅ Criterios de Evaluación

| Criterio | Cumple |
|----------|--------|
| Existe el Enum `Rol` y está mapeado a la base de datos | ☐ |
| El sistema responde con un token JWT al hacer un POST con credenciales correctas | ☐ |
| El token JWT se envía encapsulado en una Cookie `HttpOnly` | ☐ |
| La política de creación de sesiones está configurada como `STATELESS` | ☐ |
| Se inyectó correctamente el PasswordEncoder (`BCryptPasswordEncoder`) en el contexto | ☐ |
