package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 配置类
 * @author
 */
@Configuration
public class OllamaConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}