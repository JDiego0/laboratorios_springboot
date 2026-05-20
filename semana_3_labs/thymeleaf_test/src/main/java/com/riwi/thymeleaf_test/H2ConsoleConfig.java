package com.riwi.thymeleaf_test;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class H2ConsoleConfig {

    @Bean
    ServletRegistrationBean<?> h2servletRegistration() {

        var registrationBean =
                new ServletRegistrationBean<>(new JakartaWebServlet());

        registrationBean.addUrlMappings("/h2-console/*");

        return registrationBean;
    }
}
