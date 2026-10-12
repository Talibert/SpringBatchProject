package com.example.api_docker.infra.batch.service;

import com.example.api_docker.BatchIntegrationAbstractTests;
import com.example.api_docker.infra.batch.job.collector.CollectorJobConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para validação do serviço de lançamento e consulta de jobs.
 */
class BatchJobLauncherServiceTest extends BatchIntegrationAbstractTests {

    @Autowired
    private BatchJobLauncherService jobLauncherService;

    @Test
    @DisplayName("Deve lançar exceção ao tentar disparar um job inexistente")
    void shouldThrowExceptionWhenJobDoesNotExist() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jobLauncherService.runJob("jobInexistente", Map.of())
        );

        assertTrue(exception.getMessage().contains("Job não encontrado"));
    }

    @Test
    @DisplayName("Deve disparar job passando diferentes tipos de parâmetros")
    void shouldLaunchJobWithDifferentParameterTypes() throws Exception {
        Map<String, Object> params = Map.of(
                "paramTexto", "valorTexto",
                "paramLong", 12345L,
                "paramDouble", 99.95
        );

        JobExecution execution = jobLauncherService.runJob(CollectorJobConfig.COLLECTOR_JOB_NAME, params);

        assertNotNull(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals("valorTexto", execution.getJobParameters().getString("paramTexto"));
        assertEquals(12345L, execution.getJobParameters().getLong("paramLong"));
        assertEquals(99.95, execution.getJobParameters().getDouble("paramDouble"));
    }

    @Test
    @DisplayName("Deve consultar JobExecution por id")
    void shouldRetrieveJobExecutionById() throws Exception {
        JobExecution execution = jobLauncherService.runCollectorJob(Map.of());
        assertNotNull(execution);

        JobExecution found = jobLauncherService.getJobExecution(execution.getId());
        assertNotNull(found);
        assertEquals(execution.getId(), found.getId());
    }
}
