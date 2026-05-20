package com.riwi.thymeleaf_test;

import com.riwi.thymeleaf_test.model.Libro;
import com.riwi.thymeleaf_test.repository.LibroRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class DataSeed implements CommandLineRunner {

    private final LibroRepository libroRepository;

    public DataSeed(LibroRepository libroRepository) {
        this.libroRepository = libroRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (libroRepository.count() == 0) {
            for (int i = 1; i <= 50; i++) {
                libroRepository.save(new Libro(null, "Libro de Prueba " + i, "Autor " + (i % 5), "ISBN-" + i, 2020 + (i % 5)));
            }
            System.out.println("✅ Datos de prueba insertados.");
        }

    }
}
