package com.yms.yms.infrastructure.messaging;

import com.yms.yms.domain.tracking.service.TrackingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "yms.tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GpsKafkaConsumer {
    private final TrackingService trackingService;

    public GpsKafkaConsumer(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @KafkaListener(topics = "${yms.kafka.topic:yms.gps}")
    public void persist(String payload) {
        trackingService.ingest(trackingService.parse(payload));
    }
}
