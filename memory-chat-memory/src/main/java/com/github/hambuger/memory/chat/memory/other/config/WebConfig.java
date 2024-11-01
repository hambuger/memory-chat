package com.github.hambuger.memory.chat.memory.other.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:8080","http://127.0.0.1:8080","https://hamburgerhan.com/")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}