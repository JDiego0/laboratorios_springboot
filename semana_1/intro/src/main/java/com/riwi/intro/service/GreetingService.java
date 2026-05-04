package com.riwi.intro.service;

import org.springframework.stereotype.Service;

@Service
public class GreetingService {

    public String getPersonalizedGreeting(String name) {
        return "¡Hola, " + name + "! Bienvenido al entrenamiento de Spring Boot.";
    }
}
