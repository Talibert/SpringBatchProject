package com.example.api_docker.infra.kafka;

import com.example.api_docker.domain.shared.DomainEvent;
import com.example.api_docker.domain.shared.EventType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Vamos usar essa classe para mapear os eventos. Isso evita que o domínio conheça o kafka
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaTopicRegistry {

    private static final Map<EventType, String> TOPICS = Map.of(
            EventType.USER_CREATED_EVENT, "user.created",
            EventType.USER_PASSWORD_CHANGED_EVENT, "admin.password.changed",
            EventType.COLLECTOR_COMPLETED_EVENT, "batch.collector.completed"
    );

    public String topicFor(DomainEvent event) {
        String topic = TOPICS.get(event.eventType());
        if (topic == null) {
            throw new IllegalArgumentException(
                    "Evento sem tópico mapeado: " + event.eventType()
            );
        }
        return topic;
    }
}

