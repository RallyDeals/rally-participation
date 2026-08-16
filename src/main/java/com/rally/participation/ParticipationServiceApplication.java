package com.rally.participation;

import com.rally.participation.config.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class ParticipationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParticipationServiceApplication.class, args);
    }
}
