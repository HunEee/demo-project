package com.example.authapp.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.authapp.domain.system.settings.service.WebSecuritySettingsService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class MvcConfig implements WebMvcConfigurer {

    private final WebSecuritySettingsService webSecuritySettingsService;

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        var policy = webSecuritySettingsService.current();
        corsRegistry.addMapping("/**")
                .allowedOrigins(csv(policy.getAllowedOrigins()))
                .allowedMethods(csv(policy.getAllowedMethods()))
                .allowCredentials(policy.isAllowCredentials())
                .allowedHeaders("*")
                .exposedHeaders("Set-Cookie", "Authorization");
    }

    private String[] csv(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toArray(String[]::new);
    }
    
}
