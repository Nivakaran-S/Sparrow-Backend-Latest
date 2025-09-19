package com.sparrow.auth_service.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {

    @Override
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "https://*.sparrow.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
            public boolean preHandle(
                    javax.servlet.http.HttpServletRequest request,
                    javax.servlet.http.HttpServletResponse response,
                    Object handler) {

                // Add security headers
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("X-Frame-Options", "DENY");
                response.setHeader("X-XSS-Protection", "1; mode=block");
                response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");

                return true;
            }
        });
    }
}