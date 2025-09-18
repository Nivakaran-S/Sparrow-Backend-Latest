package com.sparrow.consolidation_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic parcelCreatedTopic() {
        return TopicBuilder.name("parcel-created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic parcelConsolidatedTopic() {
        return TopicBuilder.name("parcel-consolidated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic consolidationStatusTopic() {
        return TopicBuilder.name("consolidation-status")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
