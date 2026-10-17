package com.example.api_docker.infra.kafka.batch;

import com.example.api_docker.domain.batch.JobType;
import com.example.api_docker.domain.batch.event.BatchJobCompletedEvent;
import com.example.api_docker.infra.batch.service.BatchOrchestratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

@DisplayName("Testes Unitários do BatchJobEventConsumer")
class BatchJobEventConsumerTest {

    private final BatchOrchestratorService orchestratorService = mock(BatchOrchestratorService.class);
    private final BatchJobEventConsumer consumer = new BatchJobEventConsumer(orchestratorService);

    @Test
    @DisplayName("Deve delegar para orchestratorService.onCollectorCompleted quando coletor tiver targetCalculatorJob")
    void shouldDelegateToOrchestratorWhenCollectorHasTargetCalculator() {
        String jobName = "collectorJob";
        String dataReferencia = "2026-10-17";
        String targetCalculator = "calculatorJob";
        BatchJobCompletedEvent event = BatchJobCompletedEvent.of(
                jobName, JobType.COLLECTOR, dataReferencia, 123L, "COMPLETED", targetCalculator
        );

        consumer.onJobCompleted(event);

        verify(orchestratorService, times(1)).onCollectorCompleted(jobName, dataReferencia, targetCalculator);
    }

    @Test
    @DisplayName("NÃO deve delegar para orchestratorService quando coletor for disparado avulso (sem targetCalculatorJob)")
    void shouldNotDelegateToOrchestratorWhenCollectorHasNoTarget() {
        String jobName = "collectorJob";
        String dataReferencia = "2026-10-17";
        BatchJobCompletedEvent event = BatchJobCompletedEvent.of(
                jobName, JobType.COLLECTOR, dataReferencia, 123L, "COMPLETED"
        );

        consumer.onJobCompleted(event);

        verify(orchestratorService, never()).onCollectorCompleted(any(), any(), any());
    }

    @Test
    @DisplayName("NÃO deve delegar para orchestratorService quando o job concluído for um CALCULATOR")
    void shouldNotDelegateToOrchestratorWhenJobIsCalculator() {
        String jobName = "calculatorJob";
        String dataReferencia = "2026-10-17";
        BatchJobCompletedEvent event = BatchJobCompletedEvent.of(
                jobName, JobType.CALCULATOR, dataReferencia, 123L, "COMPLETED"
        );

        consumer.onJobCompleted(event);

        verify(orchestratorService, never()).onCollectorCompleted(any(), any(), any());
    }
}
