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

        OrchestratedJobTriggerResult result = orchestratorService.triggerJob("calculadorGeral", dataRef, Map.of());

        assertNotNull(result);
        assertTrue(result.startedImmediately());
        assertNotNull(result.jobExecution());
        assertTrue(result.triggeredDependencies().isEmpty());
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

        OrchestratedJobTriggerResult result = orchestratorService.triggerJob("calculadorGeral", dataRef, Map.of());

        assertNotNull(result);
        assertFalse(result.startedImmediately(), "Não deve disparar o calculador enquanto houver dependências pendentes");
        assertNull(result.jobExecution());
        assertEquals(List.of("coletorB"), result.triggeredDependencies());

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

    @Test
    @DisplayName("triggerJob: Deve acionar diretamente job coletor e indicar startedImmediately=true")
    void shouldTriggerCollectorDirectlyViaLauncher() throws Exception {
        String dataRef = "2026-10-17";
        JobExecution mockExecution = mock(JobExecution.class);
        when(jobLauncherService.runJob(eq("coletorA"), any())).thenReturn(mockExecution);

        OrchestratedJobTriggerResult result = orchestratorService.triggerJob("coletorA", dataRef, Map.of("origem", "bacen"));

        assertNotNull(result);
        assertTrue(result.startedImmediately());
        assertTrue(result.triggeredDependencies().isEmpty());
        assertSame(mockExecution, result.jobExecution());
        verify(jobLauncherService, times(1)).runJob(eq("coletorA"), eq(Map.of("origem", "bacen", "dataReferencia", dataRef)));
    }

    @Test
    @DisplayName("triggerJob: Deve disparar coletas pendentes e indicar startedImmediately=false para calculador com dependências")
    void shouldTriggerCalculatorWithPendingDependencies() throws Exception {
        String dataRef = "2026-10-17";
        when(jobExplorer.getJobInstances("coletorA", 0, 100)).thenReturn(List.of());
        when(jobExplorer.getJobInstances("coletorB", 0, 100)).thenReturn(List.of());

        OrchestratedJobTriggerResult result = orchestratorService.triggerJob("calculadorGeral", dataRef, Map.of());

        assertNotNull(result);
        assertFalse(result.startedImmediately());
        assertNull(result.jobExecution());
        assertEquals(List.of("coletorA", "coletorB"), result.triggeredDependencies());
        assertTrue(result.message().contains("possui 2 coleta(s) pendente(s)"));

        verify(jobLauncherService, times(1)).runJob(eq("coletorA"), any());
        verify(jobLauncherService, times(1)).runJob(eq("coletorB"), any());
        verify(jobLauncherService, never()).runJob(eq("calculadorGeral"), any());
    }

    @Test
    @DisplayName("triggerJob: Deve lançar IllegalArgumentException quando o job não existir no catálogo")
    void shouldThrowExceptionWhenTriggeringUnknownJob() {
        assertThrows(IllegalArgumentException.class, () ->
                orchestratorService.triggerJob("jobInexistente", "2026-10-17", Map.of())
        );
    }

    @Test
    @DisplayName("getAvailableDescriptors e getDescriptor: Deve retornar catálogo de descritores registrados")
    void shouldReturnDescriptors() {
        List<JobDescriptor> descriptors = orchestratorService.getAvailableDescriptors();
        assertEquals(3, descriptors.size());

        JobDescriptor descriptor = orchestratorService.getDescriptor("coletorA");
        assertNotNull(descriptor);
        assertEquals(JobType.COLLECTOR, descriptor.type());
    }

    @Test
    @DisplayName("getRunningJobExecutions: Deve retornar execuções em andamento ordenadas por ID decrescente")
    void shouldReturnRunningJobExecutions() {
        JobExecution exec1 = mock(JobExecution.class);
        when(exec1.getId()).thenReturn(100L);

        JobExecution exec2 = mock(JobExecution.class);
        when(exec2.getId()).thenReturn(200L);

        when(jobExplorer.findRunningJobExecutions("coletorA")).thenReturn(java.util.Set.of(exec1));
        when(jobExplorer.findRunningJobExecutions("coletorB")).thenReturn(java.util.Set.of(exec2));
        when(jobExplorer.findRunningJobExecutions("calculadorGeral")).thenReturn(java.util.Set.of());

        List<JobExecution> running = orchestratorService.getRunningJobExecutions();

        assertEquals(2, running.size());
        assertEquals(200L, running.get(0).getId());
        assertEquals(100L, running.get(1).getId());
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
