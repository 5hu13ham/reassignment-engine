package com.zycus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Development UI is allowed. Do not expose this unauthenticated API publicly. */
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer localCors() {
        return new WebMvcConfigurer() {
            @Override public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("http://localhost:5173")
                        .allowedMethods("GET","POST","PATCH","OPTIONS")
                        .allowedHeaders("Content-Type");
            }
        };
    }
}
