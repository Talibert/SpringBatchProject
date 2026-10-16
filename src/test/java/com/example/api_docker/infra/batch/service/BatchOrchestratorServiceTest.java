package com.example.api_docker.infra.batch.service;

import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.example.api_docker.infra.batch.descriptor.JobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para validar a lógica de checagem de dependências,
 * disparo de coletas pendentes e barreira de sincronização no BatchOrchestratorService.
 */
class BatchOrchestratorServiceTest {

    private BatchJobLauncherService jobLauncherService;
    private JobExplorer jobExplorer;
    private BatchOrchestratorService orchestratorService;

    private final JobDescriptor collectorA = new JobDescriptor("coletorA", JobType.COLLECTOR);
    private final JobDescriptor collectorB = new JobDescriptor("coletorB", JobType.COLLECTOR);
    private final JobDescriptor calculatorJob = new JobDescriptor(
            "calculadorGeral",
            JobType.CALCULATOR,
            List.of("coletorA", "coletorB")
    );

    @BeforeEach
    void setUp() {
        jobLauncherService = mock(BatchJobLauncherService.class);
        jobExplorer = mock(JobExplorer.class);

        orchestratorService = new BatchOrchestratorService(
                jobLauncherService,
                jobExplorer,
                List.of(() -> collectorA, () -> collectorB, () -> calculatorJob)
        );
    }

    @Test
    @DisplayName("Deve disparar o calculador imediatamente quando todas as dependências da data já estiverem concluídas")
    void shouldLaunchCalculatorImmediatelyWhenAllDependenciesAreCompleted() throws Exception {
        String dataRef = "2026-10-15";

        // Simula que coletorA e coletorB já concluíram com sucesso para 2026-10-15
        mockJobCompletedForDate("coletorA", dataRef);
        mockJobCompletedForDate("coletorB", dataRef);

        JobExecution mockExecution = mock(JobExecution.class);
        when(jobLauncherService.runJob(eq("calculadorGeral"), any())).thenReturn(mockExecution);

        JobExecution result = orchestratorService.requestCalculation("calculadorGeral", dataRef, Map.of());

        assertNotNull(result);
        verify(jobLauncherService, times(1)).runJob(eq("calculadorGeral"), any());
        verify(jobLauncherService, never()).runJob(eq("coletorA"), any());
        verify(jobLauncherService, never()).runJob(eq("coletorB"), any());
    }

    @Test
    @DisplayName("Deve disparar apenas as coletas pendentes em paralelo quando faltarem dependências para a data")
    void shouldLaunchOnlyPendingCollectorsWhenDependenciesAreMissing() throws Exception {
        String dataRef = "2026-10-15";

        // coletorA já rodou, mas coletorB ainda NÃO rodou
        mockJobCompletedForDate("coletorA", dataRef);
        when(jobExplorer.getJobInstances("coletorB", 0, 100)).thenReturn(List.of());

        JobExecution result = orchestratorService.requestCalculation("calculadorGeral", dataRef, Map.of());

        assertNull(result, "Não deve disparar o calculador enquanto houver dependências pendentes");

        // coletorA não precisa ser redisparado
        verify(jobLauncherService, never()).runJob(eq("coletorA"), any());
        // coletorB deve ser disparado
        verify(jobLauncherService, times(1)).runJob(eq("coletorB"), any());
        // calculadorGeral não deve rodar ainda
        verify(jobLauncherService, never()).runJob(eq("calculadorGeral"), any());
    }

    @Test
    @DisplayName("Deve disparar o calculador automaticamente apenas quando o ÚLTIMO coletor dependente finalizar")
    void shouldTriggerCalculatorOnlyWhenLastCollectorFinishes() throws Exception {
        String dataRef = "2026-10-15";

        // Cenário 1: coletorA finaliza, mas coletorB ainda não concluiu
        when(jobExplorer.getJobInstances("coletorA", 0, 100)).thenReturn(List.of());
        when(jobExplorer.getJobInstances("coletorB", 0, 100)).thenReturn(List.of());

        orchestratorService.onCollectorCompleted("coletorA", dataRef);
        verify(jobLauncherService, never()).runJob(eq("calculadorGeral"), any());

        // Cenário 2: coletorA já está no banco como COMPLETED, mas coletorB AINDA NÃO comitou no JobExplorer
        mockJobCompletedForDate("coletorA", dataRef);
        when(jobExplorer.getJobInstances("coletorB", 0, 100)).thenReturn(List.of());

        // coletorB emite seu evento de conclusão
        orchestratorService.onCollectorCompleted("coletorB", dataRef);

        // Como coletorB veio no próprio evento (em memória) e coletorA já estava no banco, o calculador é acionado sem race condition!
        verify(jobLauncherService, times(1)).runJob(eq("calculadorGeral"), eq(Map.of("dataReferencia", dataRef)));
    }

    private void mockJobCompletedForDate(String jobName, String dataReferencia) {
        JobInstance instance = new JobInstance(1L, jobName);
        JobParameters params = new JobParametersBuilder()
                .addString("dataReferencia", dataReferencia)
                .toJobParameters();

        JobExecution execution = new JobExecution(instance, 10L, params);
        execution.setStatus(BatchStatus.COMPLETED);

        when(jobExplorer.getJobInstances(jobName, 0, 100)).thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(execution));
    }
}
