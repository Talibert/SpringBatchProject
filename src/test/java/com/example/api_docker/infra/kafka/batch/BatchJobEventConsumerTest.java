package com.example.api_docker.infra.kafka.batch;

import com.example.api_docker.domain.batch.JobType;
import com.example.api_docker.domain.batch.TargetCalculatorContext;
import com.example.api_docker.domain.batch.event.BatchJobCompletedEvent;
import com.example.api_docker.infra.batch.service.BatchOrchestratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

@DisplayName("Testes Unitários do BatchJobEventConsumer")
class BatchJobEventConsumerTest {

    private final BatchOrchestratorService orchestratorService = mock(BatchOrchestratorService.class);
    private final BatchJobEventConsumer consumer = new BatchJobEventConsumer(orchestratorService);

    @Test
    @DisplayName("Deve delegar o evento recebido diretamente para orchestratorService.handleJobCompletedEvent")
    void shouldDelegateEventToOrchestratorService() {
        String jobName = "collectorJob";
        String dataReferencia = "2026-10-17";
        TargetCalculatorContext targetContext = TargetCalculatorContext.of("calculatorJob", Map.of("param1", "valor1"));
        BatchJobCompletedEvent event = BatchJobCompletedEvent.of(
                jobName, JobType.COLLECTOR, dataReferencia, 123L, "COMPLETED", targetContext
        );

        consumer.onJobCompleted(event);

        verify(orchestratorService, times(1)).handleJobCompletedEvent(event);
    }
}
