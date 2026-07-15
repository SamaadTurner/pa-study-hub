package com.pastudyhub.flashcard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${progress.service.url:http://study-progress-service:8083}")
    private String progressServiceUrl;

    @Bean
    public WebClient progressServiceClient() {
        return WebClient.builder()
                .baseUrl(progressServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
