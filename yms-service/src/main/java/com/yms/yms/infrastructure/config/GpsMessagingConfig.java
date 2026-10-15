package com.yms.yms.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(prefix = "yms.tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GpsMessagingConfig {
    @Bean
    NewTopic gpsTopic(@Value("${yms.kafka.topic:yms.gps}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
