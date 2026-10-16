package com.example.api_docker;

import com.example.api_docker.domain.batch.event.CollectorCompletedEvent;
import com.example.api_docker.infra.batch.job.calculator.CalculatorJobConfig;
import com.example.api_docker.infra.batch.job.collector.CollectorJobConfig;
import com.example.api_docker.infra.batch.service.BatchOrchestratorService;
import com.example.api_docker.infra.kafka.batch.BatchJobEventConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Teste End-to-End (E2E) com Embedded Kafka validando o fluxo reativo completo:
 * <ol>
 *   <li>Solicitação de cálculo sem dados coletados prévios.</li>
 *   <li>Disparo automático do Job Coletor faltante.</li>
 *   <li>Emissão do evento de término via Kafka (JobCompletionNotificationListener -> Kafka).</li>
 *   <li>Consumo do evento pelo BatchJobEventConsumer.</li>
 *   <li>Destravamento e disparo automático do Job Calculador!</li>
 * </ol>
 */
class BatchOrchestrationKafkaIntegrationTest extends IntegrationAbstractTests {

    @Autowired
    private BatchOrchestratorService orchestratorService;

    @Autowired
    private JobExplorer jobExplorer;

    @MockitoSpyBean
    private BatchJobEventConsumer batchJobEventConsumer;

    @Test
    @DisplayName("E2E: Deve solicitar cálculo, disparar coleta pendente, notificar via Kafka e acionar calculador")
    void shouldOrchestrateCollectorAndCalculatorViaKafka() throws Exception {
        String dataReferencia = "2026-10-15";

        // 1. Solicita o cálculo para o calculatorJob (que depende do collectorJob)
        JobExecution initialResult = orchestratorService.requestCalculation(
                CalculatorJobConfig.CALCULATOR_JOB_NAME,
                dataReferencia,
                Map.of("origem", "e2e-kafka-test")
        );

        // Como a coleta ainda não existia, o retorno inicial deve ser null (coleta disparada em background)
        assertNull(initialResult, "Cálculo não deve ser executado de imediato antes da coleta");

        // 2. Aguarda o Kafka entregar o evento no BatchJobEventConsumer (timeout de 10s)
        ArgumentCaptor<CollectorCompletedEvent> eventCaptor = ArgumentCaptor.forClass(CollectorCompletedEvent.class);
        verify(batchJobEventConsumer, timeout(10000).atLeastOnce()).onCollectorCompleted(eventCaptor.capture());

        CollectorCompletedEvent event = eventCaptor.getValue();
        assertEquals(CollectorJobConfig.COLLECTOR_JOB_NAME, event.jobName());
        assertEquals(dataReferencia, event.dataReferencia());
        assertEquals("COMPLETED", event.status());

        // 3. Valida no JobExplorer que o calculatorJob foi disparado automaticamente e concluiu com sucesso
        boolean calculatorCompleted = false;
        long deadline = System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < deadline) {
            if (orchestratorService.isJobCompletedForDate(CalculatorJobConfig.CALCULATOR_JOB_NAME, dataReferencia)) {
                calculatorCompleted = true;
                break;
            }
            Thread.sleep(200);
        }

        assertTrue(calculatorCompleted, "O calculatorJob deveria ter sido disparado pelo Kafka e concluído com sucesso!");
    }
}
