package com.example.api_docker.infra.kafka;

import com.example.api_docker.domain.shared.DomainEvent;
import com.example.api_docker.domain.shared.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Essa classe vai publicar todos os eventos do projeto
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicRegistry topicRegistry;

    @Override
    public void publish(DomainEvent event) {
        String topic = topicRegistry.topicFor(event);
        String partitionKey = event.partitionKey();
        if (partitionKey != null) {
            kafkaTemplate.send(topic, partitionKey, event);
        } else {
            kafkaTemplate.send(topic, event);
        }
    }
}
