package com.ccps.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ccps.backend.service.PortalSessionService;
import com.ccps.backend.service.AdminPermissionService;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final PortalSessionService portalSessionService;
    private final AdminPermissionService adminPermissionService;

    public WebConfig(PortalSessionService portalSessionService, AdminPermissionService adminPermissionService) {
        this.portalSessionService = portalSessionService;
        this.adminPermissionService = adminPermissionService;
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAuthInterceptor(portalSessionService, adminPermissionService))
                .addPathPatterns("/api/admin/**", "/api/properties/**");
        registry.addInterceptor(new OwnerAuthInterceptor(portalSessionService))
                .addPathPatterns("/api/owner/**");
        registry.addInterceptor(new AuthInterceptor(portalSessionService))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/health", "/api/admin/**", "/api/owner/**",
                        "/api/properties/**", "/api/public/signatures/**", "/api/webhooks/whatsapp");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Match the public signing route before the generic API rule. Do not expand admin origins.
        registry.addMapping("/api/public/signatures/**")
                .allowedOrigins("https://lzj.mydream.tw", "http://localhost:5173", "http://127.0.0.1:5173",
                        "http://47.108.39.84", "http://47.108.39.84:8080")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173", "http://47.108.39.84", "http://47.108.39.84:8080")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
