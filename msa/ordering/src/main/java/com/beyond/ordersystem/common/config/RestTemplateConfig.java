package com.beyond.ordersystem.common.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    @LoadBalanced // eureka 등록된 서비스명을 사용해서 내부서비스 호출(내부통신)
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
