package com.example.api_docker.domain.batch.event;

import com.example.api_docker.domain.batch.JobType;
import com.example.api_docker.domain.batch.TargetCalculatorContext;
import com.example.api_docker.domain.shared.DomainEvent;
import com.example.api_docker.domain.shared.EventType;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento emitido quando qualquer Job do Spring Batch (Coletor ou Calculador) finaliza com sucesso.
 * <p>
 * Notifica o ecossistema e consumidores (como frontend em Vue / WebSocket / SSE) sobre a conclusão
 * de um processamento em lote. O campo {@code targetContext} só é preenchido quando o job concluído
 * for um Coletor disparado especificamente para satisfazer dependências de um Calculador.
 *
 * @param eventId        Identificador único do evento.
 * @param jobName        Nome lógico do Job concluído (ex: collectorJob, calculatorJob).
 * @param jobType        Classificação funcional tipada do job (COLLECTOR ou CALCULATOR).
 * @param dataReferencia Data de corte ou referência dos dados (ex: 2026-10-15).
 * @param executionId    Identificador físico da execução no Spring Batch.
 * @param status         Status de conclusão do Job (ex: COMPLETED).
 * @param targetContext  Contexto do calculador alvo (Value Object), nulo se disparo avulso.
 * @param occurredAt     Momento exato da conclusão.
 */
public record BatchJobCompletedEvent(
        UUID eventId,
        String jobName,
        JobType jobType,
        String dataReferencia,
        Long executionId,
        String status,
        TargetCalculatorContext targetContext,
        LocalDateTime occurredAt
) implements DomainEvent {

    public BatchJobCompletedEvent {
        Objects.requireNonNull(eventId, "eventId não pode ser nulo");
        Objects.requireNonNull(jobName, "jobName não pode ser nulo");
        Objects.requireNonNull(jobType, "jobType não pode ser nulo");
        Objects.requireNonNull(dataReferencia, "dataReferencia não pode ser nulo");
        Objects.requireNonNull(executionId, "executionId não pode ser nulo");
        Objects.requireNonNull(status, "status não pode ser nulo");
        Objects.requireNonNull(occurredAt, "occurredAt não pode ser nulo");
    }

    public static BatchJobCompletedEvent of(
            String jobName,
            JobType jobType,
            String dataReferencia,
            Long executionId,
            String status,
            TargetCalculatorContext targetContext
    ) {
        return new BatchJobCompletedEvent(
                UUID.randomUUID(),
                jobName,
                jobType,
                dataReferencia,
                executionId,
                status,
                targetContext,
                LocalDateTime.now()
        );
    }

    public static BatchJobCompletedEvent of(
            String jobName,
            JobType jobType,
            String dataReferencia,
            Long executionId,
            String status
    ) {
        return of(jobName, jobType, dataReferencia, executionId, status, null);
    }

    public boolean isCollector() {
        return jobType == JobType.COLLECTOR;
    }

    public boolean isCalculator() {
        return jobType == JobType.CALCULATOR;
    }

    public boolean hasTargetContext() {
        return targetContext != null;
    }

    @Override
    public EventType eventType() {
        return EventType.BATCH_JOB_COMPLETED_EVENT;
    }

    @Override
    public String partitionKey() {
        return dataReferencia;
    }
}
