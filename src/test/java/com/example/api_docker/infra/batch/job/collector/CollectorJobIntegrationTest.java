package com.example.api_docker.infra.batch.job.collector;

import com.example.api_docker.BatchIntegrationAbstractTests;
import com.example.api_docker.infra.batch.service.BatchJobLauncherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Test
    @DisplayName("Deve disparar e executar o collectorJob com sucesso persistindo metadados no banco")
    void shouldExecuteCollectorJobSuccessfully() throws Exception {
        JobExecution execution = jobLauncherService.runCollectorJob(Map.of("origem", "teste-integracao"));

        assertNotNull(execution);
        assertNotNull(execution.getId());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(ExitStatus.COMPLETED.getExitCode(), execution.getExitStatus().getExitCode());

        // Valida que o step foi executado com sucesso
        assertFalse(execution.getStepExecutions().isEmpty());
        StepExecution stepExecution = execution.getStepExecutions().iterator().next();
        assertEquals("collectorStep", stepExecution.getStepName());
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

        // Valida persistência e consulta via JobExplorer
        JobExecution persistedExecution = jobExplorer.getJobExecution(execution.getId());
        assertNotNull(persistedExecution);
        assertEquals(execution.getId(), persistedExecution.getId());
        assertEquals(CollectorJobConfig.COLLECTOR_JOB_NAME, persistedExecution.getJobInstance().getJobName());
    }
}
