package com.riwi.thymeleaf_test.repository;

import com.riwi.thymeleaf_test.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Long> {
    // Consulta derivada: buscar libros por autor
    List<Libro> findByAutor(String autor);
}
