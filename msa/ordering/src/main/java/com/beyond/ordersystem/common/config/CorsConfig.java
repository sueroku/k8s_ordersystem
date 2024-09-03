package com.beyond.ordersystem.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // 허용정책
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 어떤 url이든 허용
                .allowedOrigins("http://localhost:8081") // 허용 url 명시 (나중에 우리 서버에 들어올 url 여기에 추가)
                .allowedMethods("*") // 어떤 메서드든 허용
                .allowedHeaders("*")
                .allowCredentials(true); // 보안처리 할거냐 말거냐
    }
}
