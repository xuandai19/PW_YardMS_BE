package com.yms.yms.infrastructure.messaging;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
@ConditionalOnProperty(prefix = "yms.tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttGpsBridge {
    private static final Logger log = LoggerFactory.getLogger(MqttGpsBridge.class);

    private final KafkaTemplate<String, String> kafka;
    private final String brokerUrl;
    private final String clientId;
    private final String subscription;
    private final String kafkaTopic;
    private MqttClient client;

    public MqttGpsBridge(KafkaTemplate<String, String> kafka,
            @Value("${yms.tracking.mqtt.broker-url:tcp://localhost:1883}") String brokerUrl,
            @Value("${yms.tracking.mqtt.client-id:yms-gps-ingestion}") String clientId,
            @Value("${yms.tracking.mqtt.topic:yms/gps/+}") String subscription,
            @Value("${yms.kafka.topic:yms.gps}") String kafkaTopic) {
        this.kafka = kafka;
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.subscription = subscription;
        this.kafkaTopic = kafkaTopic;
    }

    @Scheduled(fixedDelay = 10_000, initialDelay = 1_000)
    public synchronized void connectAndSubscribe() {
        try {
            if (client == null) {
                client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            }
            if (client.isConnected()) {
                return;
            }
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(3);
            client.connect(options);
            client.subscribe(subscription, 1, (topic, message) -> {
                String payload = new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
                kafka.send(kafkaTopic, payload).whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Unable to forward MQTT GPS event to Kafka", error);
                    }
                });
            });
            log.info("Subscribed to MQTT GPS topic {}", subscription);
        } catch (MqttException exception) {
            log.warn("MQTT broker {} is unavailable; GPS bridge will retry: {}", brokerUrl, exception.getMessage());
            if (client != null && client.isConnected()) {
                try {
                    client.disconnect();
                } catch (MqttException disconnectException) {
                    log.warn("Unable to reset failed MQTT subscription: {}", disconnectException.getMessage());
                }
            }
        }
    }

    @PreDestroy
    public synchronized void close() {
        if (client != null) {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
            } catch (MqttException exception) {
                log.warn("Unable to close MQTT GPS client cleanly", exception);
            }
        }
    }
}
