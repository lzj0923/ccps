package com.ccps.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ccps.backend.service.PortalSessionService;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final PortalSessionService portalSessionService;

    public WebConfig(PortalSessionService portalSessionService) {
        this.portalSessionService = portalSessionService;
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAuthInterceptor(portalSessionService))
                .addPathPatterns("/api/admin/**", "/api/properties/**");
        registry.addInterceptor(new OwnerAuthInterceptor(portalSessionService))
                .addPathPatterns("/api/owner/**");
        registry.addInterceptor(new AuthInterceptor(portalSessionService))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/health", "/api/admin/**", "/api/owner/**",
                        "/api/properties/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173", "http://47.108.39.84", "http://47.108.39.84:8080")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
