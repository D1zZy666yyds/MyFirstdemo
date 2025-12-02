// WebConfig.java - 修复静态资源映射版本
package com.dzy666.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:8080", "http://127.0.0.1:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保静态资源能够被访问
        registry.addResourceHandler("/**")
                .addResourceLocations(
                        "classpath:/static/",
                        "classpath:/public/",
                        "classpath:/resources/",
                        "classpath:/META-INF/resources/",
                        "file:./src/main/resources/static/",
                        "file:./frontend/"
                );

        // 单独处理HTML文件，确保Vue路由能正常工作
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/");

        // 处理JS、CSS等资源
        registry.addResourceHandler("/js/**", "/css/**", "/images/**")
                .addResourceLocations(
                        "classpath:/static/js/",
                        "classpath:/static/css/",
                        "classpath:/static/images/"
                );
    }
}