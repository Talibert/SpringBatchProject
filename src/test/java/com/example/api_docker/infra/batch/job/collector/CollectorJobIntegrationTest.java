package com.example.api_docker.infra.batch.job.collector;

import com.example.api_docker.BatchIntegrationAbstractTests;
import com.example.api_docker.infra.batch.job.collector.client.ExternalDataCollectorClient;
import com.example.api_docker.infra.batch.service.BatchJobLauncherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para validação do ciclo de vida e persistência do Job Coletor.
 */
class CollectorJobIntegrationTest extends BatchIntegrationAbstractTests {

    @Autowired
    private BatchJobLauncherService jobLauncherService;

    @Autowired
    private JobExplorer jobExplorer;

    /**
     * Demonstração arquitetural: O Spring substitui automaticamente o bean real
     * pelo Mockito mock no contexto da aplicação e o injeta dentro do StepCollector.
     */
    @MockitoBean
    private ExternalDataCollectorClient externalDataCollectorClientMock;

    @Test
    @DisplayName("Deve disparar e executar o collectorJob com sucesso persistindo metadados no banco e acionando mockito bean")
    void shouldExecuteCollectorJobSuccessfully() throws Exception {
        // Programa o comportamento do mock injetado pelo Spring
        Mockito.when(externalDataCollectorClientMock.fetchExternalData(Mockito.anyString()))
                .thenReturn(List.of("registro-mock-1", "registro-mock-2"));

        JobExecution execution = jobLauncherService.runJob(CollectorJobConfig.COLLECTOR_JOB_NAME, Map.of("origem", "teste-integracao"));

        assertNotNull(execution);
        assertNotNull(execution.getId());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(ExitStatus.COMPLETED.getExitCode(), execution.getExitStatus().getExitCode());

        // Valida que o step foi executado com sucesso
        assertFalse(execution.getStepExecutions().isEmpty());
        StepExecution stepExecution = execution.getStepExecutions().iterator().next();
        assertEquals("collectorStep", stepExecution.getStepName());
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

        // Valida que o mockito bean foi acionado durante a execução do Job
        Mockito.verify(externalDataCollectorClientMock, Mockito.times(1))
                .fetchExternalData("teste-integracao");

        // Valida persistência e consulta via JobExplorer
        JobExecution persistedExecution = jobExplorer.getJobExecution(execution.getId());
        assertNotNull(persistedExecution);
        assertEquals(execution.getId(), persistedExecution.getId());
        assertEquals(CollectorJobConfig.COLLECTOR_JOB_NAME, persistedExecution.getJobInstance().getJobName());
    }
}
