package com.sparrow.consolidation_service;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class ConsolidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsolidationServiceApplication.class, args);
    }
}
