package com.sparrow.parcel_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic parcelCreatedTopic() {
        return TopicBuilder.name("parcel-created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic parcelStatusUpdatedTopic() {
        return TopicBuilder.name("parcel-status-updated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic consolidationParcelsTopic() {
        return TopicBuilder.name("consolidation-parcels")
                .partitions(3)
                .replicas(1)
                .build();
    }
}