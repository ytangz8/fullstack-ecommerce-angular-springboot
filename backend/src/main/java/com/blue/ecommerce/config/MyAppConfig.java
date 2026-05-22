package com.blue.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyAppConfig implements WebMvcConfigurer{

    @Value("${allowed.origins}")
    private String[] theAllowedOrigins;
    @Value("${spring.data.rest.base-path}")
    private String basePath;
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // // CORS for REST API
        // registry.addMapping(basePath + "/**")
        //         .allowedOrigins(theAllowedOrigins);
        
        // // CORS for Chat API (SSE streaming)
        // registry.addMapping("/api/chat/**")
        //         .allowedOrigins(theAllowedOrigins)
        //         .allowedMethods("GET", "POST", "OPTIONS")
        //         .allowedHeaders("*")
        //         .allowCredentials(true);
        
        WebMvcConfigurer.super.addCorsMappings(registry);
    }

    

}
