package com.example.api_docker.infra.kafka.batch;

import com.example.api_docker.domain.batch.event.CollectorCompletedEvent;
import com.example.api_docker.infra.batch.service.BatchOrchestratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

@DisplayName("Testes Unitários do BatchJobEventConsumer")
class BatchJobEventConsumerTest {

    private final BatchOrchestratorService orchestratorService = mock(BatchOrchestratorService.class);
    private final BatchJobEventConsumer consumer = new BatchJobEventConsumer(orchestratorService);

    @Test
    @DisplayName("Deve delegar para orchestratorService.onCollectorCompleted ao receber CollectorCompletedEvent")
    void shouldDelegateToOrchestratorWhenEventReceived() {
        String jobName = "collectorJob";
        String dataReferencia = "2026-10-17";
        CollectorCompletedEvent event = CollectorCompletedEvent.of(jobName, dataReferencia, 123L, "COMPLETED");

        consumer.onCollectorCompleted(event);

        verify(orchestratorService, times(1)).onCollectorCompleted(jobName, dataReferencia);
    }
}
