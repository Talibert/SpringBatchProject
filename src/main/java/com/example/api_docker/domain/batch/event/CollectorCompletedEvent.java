package com.example.api_docker.domain.batch.event;

import com.example.api_docker.domain.shared.DomainEvent;
import com.example.api_docker.domain.shared.EventType;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento emitido quando um Job Coletor finaliza sua execução com sucesso.
 * <p>
 * Notifica o ecossistema e os orquestradores de que os dados brutos daquela data
 * foram ingeridos e já estão disponíveis para processamento analítico e consolidação.
 *
 * @param eventId         Identificador único do evento.
 * @param jobName         Nome lógico do Job Coletor concluído (ex: collectorJob).
 * @param dataReferencia  Data de corte ou referência dos dados coletados (ex: 2026-10-15).
 * @param executionId     Identificador físico da execução no Spring Batch.
 * @param status          Status de conclusão do Job (ex: COMPLETED).
 * @param occurredAt      Momento exato da conclusão.
 */
public record CollectorCompletedEvent(
        UUID eventId,
        String jobName,
        String dataReferencia,
        Long executionId,
        String status,
        LocalDateTime occurredAt
) implements DomainEvent {

    public CollectorCompletedEvent {
        Objects.requireNonNull(eventId, "eventId não pode ser nulo");
        Objects.requireNonNull(jobName, "jobName não pode ser nulo");
        Objects.requireNonNull(dataReferencia, "dataReferencia não pode ser nulo");
        Objects.requireNonNull(executionId, "executionId não pode ser nulo");
        Objects.requireNonNull(status, "status não pode ser nulo");
        Objects.requireNonNull(occurredAt, "occurredAt não pode ser nulo");
    }

    public static CollectorCompletedEvent of(String jobName, String dataReferencia, Long executionId, String status) {
        return new CollectorCompletedEvent(
                UUID.randomUUID(),
                jobName,
                dataReferencia,
                executionId,
                status,
                LocalDateTime.now()
        );
    }

    @Override
    public EventType eventType() {
        return EventType.COLLECTOR_COMPLETED_EVENT;
    }

    @Override
    public String partitionKey() {
        return dataReferencia;
    }
}
