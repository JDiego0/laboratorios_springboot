package com.riwi.thymeleaf_test.controller.ui;

import com.riwi.thymeleaf_test.model.Libro;
import com.riwi.thymeleaf_test.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("ui/libros")
public class LibroUIController {

    private final LibroService libroService;

    public LibroUIController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping
    public String listarLibrosUI(Model model) {
        List<Libro> libros = libroService.obtenerTodos();

        // Pasamos la lista de libros a la vista con el nombre "libros"
        model.addAttribute("libros", libros);
        model.addAttribute("tituloPantalla", "Catálogo de Libros - Dashboard");

        // Retornamos el nombre del archivo HTML (sin la extensión .html)
        return "libros/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        // Pasamos una instancia vacía que el formulario llenará
        model.addAttribute("libro", new Libro());
        model.addAttribute("tituloPantalla", "Registrar Nuevo Libro");

        return "libros/formulario";
    }

    @PostMapping("/guardar")
    public String guardarLibro(@ModelAttribute("libro") Libro libro) {
        // Guardamos el libro usando el servicio
        libroService.guardar(libro);

        // Patrón PRG: Redirigimos a la lista de libros (GET)
        // La palabra clave "redirect:" le dice a Spring que emita un HTTP 302
        return "redirect:/admin/libros";
    }
}
