package com.example.api_docker;

import com.example.api_docker.domain.batch.event.BatchJobCompletedEvent;
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
 *   <li>Disparo automático do Job Coletor faltante contendo targetCalculatorJob.</li>
 *   <li>Emissão do BatchJobCompletedEvent do coletor via Kafka.</li>
 *   <li>Consumo do evento pelo BatchJobEventConsumer e avaliação exclusiva do target.</li>
 *   <li>Destravamento e disparo automático do Job Calculador!</li>
 *   <li>Emissão do BatchJobCompletedEvent do calculador via Kafka para captura pelo front.</li>
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
        com.example.api_docker.infra.batch.service.OrchestratedJobTriggerResult initialResult = orchestratorService.triggerJob(
                CalculatorJobConfig.CALCULATOR_JOB_NAME,
                dataReferencia,
                Map.of("origem", "e2e-kafka-test")
        );

        // Como a coleta ainda não existia, o job não deve iniciar de imediato e deve acusar as dependências disparadas
        assertFalse(initialResult.startedImmediately(), "Cálculo não deve ser executado de imediato antes da coleta");
        assertNull(initialResult.jobExecution());
        assertEquals(List.of(CollectorJobConfig.COLLECTOR_JOB_NAME), initialResult.triggeredDependencies());

        // 2. Aguarda o Kafka entregar os eventos no BatchJobEventConsumer (timeout de 10s)
        // Deve receber tanto a conclusão do coletor quanto a conclusão subsequente do calculador
        ArgumentCaptor<BatchJobCompletedEvent> eventCaptor = ArgumentCaptor.forClass(BatchJobCompletedEvent.class);
        verify(batchJobEventConsumer, timeout(10000).atLeast(2)).onJobCompleted(eventCaptor.capture());

        List<BatchJobCompletedEvent> capturedEvents = eventCaptor.getAllValues();

        // Valida evento do Coletor (contém targetCalculatorJob preenchido)
        BatchJobCompletedEvent collectorEvent = capturedEvents.stream()
                .filter(BatchJobCompletedEvent::isCollector)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Evento de conclusão do coletor não recebido"));
        assertEquals(CollectorJobConfig.COLLECTOR_JOB_NAME, collectorEvent.jobName());
        assertEquals(dataReferencia, collectorEvent.dataReferencia());
        assertEquals("COMPLETED", collectorEvent.status());
        assertEquals(CalculatorJobConfig.CALCULATOR_JOB_NAME, collectorEvent.targetCalculatorJob());

        // Valida evento do Calculador (capturado para o front, sem target)
        BatchJobCompletedEvent calculatorEvent = capturedEvents.stream()
                .filter(BatchJobCompletedEvent::isCalculator)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Evento de conclusão do calculador não recebido"));
        assertEquals(CalculatorJobConfig.CALCULATOR_JOB_NAME, calculatorEvent.jobName());
        assertEquals(dataReferencia, calculatorEvent.dataReferencia());
        assertEquals("COMPLETED", calculatorEvent.status());
        assertNull(calculatorEvent.targetCalculatorJob());

        // 3. Valida no JobExplorer que o calculatorJob de fato concluiu com sucesso
        boolean calculatorCompleted = orchestratorService.isJobCompletedForDate(
                CalculatorJobConfig.CALCULATOR_JOB_NAME, dataReferencia
        );
        assertTrue(calculatorCompleted, "O calculatorJob deveria ter sido registrado no banco como COMPLETED!");
    }
}
