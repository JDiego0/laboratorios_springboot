package com.riwi.intro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class AppConfig {

    @Bean // Definición manual de un Bean que no usa estereotipos
    public LocalDateTime serverStartTime() {
        return LocalDateTime.now();
    }
}
