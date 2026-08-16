package com.rally.participation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DealServiceWebClientConfig {

    @Bean
    public WebClient dealServiceWebClient(WebClient.Builder builder,
                                           @Value("${rally.deal-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
