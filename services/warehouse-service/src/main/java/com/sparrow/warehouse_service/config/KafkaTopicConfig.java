package com.sparrow.warehouse_service.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic warehouseEventsTopic() {
        return TopicBuilder.name("warehouse-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic warehouseCapacityEventsTopic() {
        return TopicBuilder.name("warehouse-capacity-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic warehouseStatusEventsTopic() {
        return TopicBuilder.name("warehouse-status-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic parcelWarehouseAssignmentTopic() {
        return TopicBuilder.name("parcel-warehouse-assignment")
                .partitions(3)
                .replicas(1)
                .build();
    }
}