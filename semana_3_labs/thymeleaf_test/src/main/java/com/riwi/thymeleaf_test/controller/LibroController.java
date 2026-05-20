package com.riwi.thymeleaf_test.controller;

import com.riwi.thymeleaf_test.model.Libro;
import com.riwi.thymeleaf_test.service.LibroService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/libros")
public class LibroController {

    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping
    public ResponseEntity<Page<Libro>> listar(
            @PageableDefault(size = 10, sort = "titulo") Pageable pageable) {
        Page<Libro> libros = libroService.listarPaginado(pageable);
        return ResponseEntity.ok(libros);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        Optional<Libro> libro = libroService.obtenerPorId(id);
        if (libro.isPresent()) {
            return ResponseEntity.ok(libro.get()); // 200 OK
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Libro con ID " + id + " no encontrado."); // 404
    }

    @PostMapping
    public ResponseEntity<Libro> crear(@RequestBody Libro libro) {
        Libro nuevoLibro = libroService.guardar(libro);
        return new ResponseEntity<>(nuevoLibro, HttpStatus.CREATED); // 201
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Libro libro) {
        Optional<Libro> actualizado = libroService.actualizar(id, libro);
        if (actualizado.isPresent()) {
            return ResponseEntity.ok(actualizado.get()); // 200
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("No se pudo actualizar. Libro no encontrado."); // 404
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (libroService.eliminar(id)) {
            return ResponseEntity.noContent().build(); // 204 No Content
        }
        return ResponseEntity.notFound().build(); // 404
    }

}
