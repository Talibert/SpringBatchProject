package com.example.api_docker.domain.shared;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    LocalDateTime occurredAt();
    EventType eventType();

    /**
     * Chave de partição opcional para envio no Apache Kafka.
     * <p>
     * Garante que eventos com a mesma chave (ex: mesma dataReferencia ou aggregateId)
     * sejam direcionados para a mesma partição e consumidos sequencialmente (FIFO).
     */
    default String partitionKey() {
        return null;
    }
}
