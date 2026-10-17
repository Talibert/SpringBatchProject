package com.example.api_docker.infra.batch.listener;

import com.example.api_docker.domain.batch.JobType;
import com.example.api_docker.domain.batch.event.BatchJobCompletedEvent;
import com.example.api_docker.domain.shared.DomainEventPublisher;
import com.example.api_docker.infra.batch.descriptor.BatchJobDescriptorProvider;
import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Testes Unitários do JobCompletionNotificationListener")
class JobCompletionNotificationListenerTest {

    private DomainEventPublisher eventPublisher;
    private JobCompletionNotificationListener listener;

    private static final String COLLECTOR_JOB_NAME = "collectorJob";
    private static final String CALCULATOR_JOB_NAME = "calculatorJob";

    @BeforeEach
    void setUp() {
        eventPublisher = mock(DomainEventPublisher.class);

        BatchJobDescriptorProvider collectorProvider = () ->
                new JobDescriptor(COLLECTOR_JOB_NAME, JobType.COLLECTOR);

        BatchJobDescriptorProvider calculatorProvider = () ->
                new JobDescriptor(CALCULATOR_JOB_NAME, JobType.CALCULATOR, List.of(COLLECTOR_JOB_NAME));

        listener = new JobCompletionNotificationListener(
                eventPublisher,
                List.of(collectorProvider, calculatorProvider)
        );
    }

    @Test
    @DisplayName("Deve publicar BatchJobCompletedEvent no Kafka quando um job COLLECTOR finaliza com COMPLETED")
    void shouldPublishEventWhenCollectorJobCompletesSuccessfully() {
        String dataRef = "2026-10-17";
        JobParameters params = new JobParametersBuilder()
                .addString("dataReferencia", dataRef)
                .toJobParameters();

        JobExecution execution = createExecution(COLLECTOR_JOB_NAME, params, BatchStatus.COMPLETED, ExitStatus.COMPLETED);

        listener.afterJob(execution);

        ArgumentCaptor<BatchJobCompletedEvent> captor = ArgumentCaptor.forClass(BatchJobCompletedEvent.class);
        verify(eventPublisher, times(1)).publish(captor.capture());

        BatchJobCompletedEvent event = captor.getValue();
        assertEquals(COLLECTOR_JOB_NAME, event.jobName());
        assertEquals(JobType.COLLECTOR, event.jobType());
        assertTrue(event.isCollector());
        assertFalse(event.isCalculator());
        assertEquals(dataRef, event.dataReferencia());
        assertEquals(execution.getId(), event.executionId());
        assertEquals("COMPLETED", event.status());
        assertNull(event.targetCalculatorJob(), "targetCalculatorJob deve ser nulo quando não fornecido nos parâmetros");
        assertFalse(event.hasTargetCalculator());
        assertEquals(dataRef, event.partitionKey(), "A partitionKey deve ser igual à dataReferencia");
    }

    @Test
    @DisplayName("Deve publicar BatchJobCompletedEvent com targetCalculatorJob preenchido quando informado nos parâmetros")
    void shouldPublishEventWithTargetCalculatorJobWhenPresent() {
        String dataRef = "2026-10-17";
        JobParameters params = new JobParametersBuilder()
                .addString("dataReferencia", dataRef)
                .addString("targetCalculatorJob", CALCULATOR_JOB_NAME)
                .toJobParameters();

        JobExecution execution = createExecution(COLLECTOR_JOB_NAME, params, BatchStatus.COMPLETED, ExitStatus.COMPLETED);

        listener.afterJob(execution);

        ArgumentCaptor<BatchJobCompletedEvent> captor = ArgumentCaptor.forClass(BatchJobCompletedEvent.class);
        verify(eventPublisher, times(1)).publish(captor.capture());

        BatchJobCompletedEvent event = captor.getValue();
        assertEquals(COLLECTOR_JOB_NAME, event.jobName());
        assertEquals(JobType.COLLECTOR, event.jobType());
        assertEquals(dataRef, event.dataReferencia());
        assertEquals(CALCULATOR_JOB_NAME, event.targetCalculatorJob());
        assertTrue(event.hasTargetCalculator());
    }

    @Test
    @DisplayName("Deve publicar BatchJobCompletedEvent no Kafka quando um job CALCULATOR finaliza com COMPLETED")
    void shouldPublishEventWhenCalculatorJobCompletesSuccessfully() {
        String dataRef = "2026-10-17";
        JobParameters params = new JobParametersBuilder()
                .addString("dataReferencia", dataRef)
                .toJobParameters();

        JobExecution execution = createExecution(CALCULATOR_JOB_NAME, params, BatchStatus.COMPLETED, ExitStatus.COMPLETED);

        listener.afterJob(execution);

        ArgumentCaptor<BatchJobCompletedEvent> captor = ArgumentCaptor.forClass(BatchJobCompletedEvent.class);
        verify(eventPublisher, times(1)).publish(captor.capture());

        BatchJobCompletedEvent event = captor.getValue();
        assertEquals(CALCULATOR_JOB_NAME, event.jobName());
        assertEquals(JobType.CALCULATOR, event.jobType());
        assertTrue(event.isCalculator());
        assertFalse(event.isCollector());
        assertEquals(dataRef, event.dataReferencia());
        assertNull(event.targetCalculatorJob());
        assertFalse(event.hasTargetCalculator());
    }

    @Test
    @DisplayName("Deve usar a data atual como fallback quando o parâmetro dataReferencia não for informado")
    void shouldUseCurrentDateWhenDataReferenciaParamIsMissing() {
        JobParameters emptyParams = new JobParametersBuilder().toJobParameters();
        JobExecution execution = createExecution(COLLECTOR_JOB_NAME, emptyParams, BatchStatus.COMPLETED, ExitStatus.COMPLETED);

        listener.afterJob(execution);

        ArgumentCaptor<BatchJobCompletedEvent> captor = ArgumentCaptor.forClass(BatchJobCompletedEvent.class);
        verify(eventPublisher, times(1)).publish(captor.capture());

        assertEquals(LocalDate.now().toString(), captor.getValue().dataReferencia());
    }

    @Test
    @DisplayName("NÃO deve publicar evento no Kafka quando o job falhar (status FAILED)")
    void shouldNotPublishEventWhenJobFails() {
        JobParameters params = new JobParametersBuilder()
                .addString("dataReferencia", "2026-10-17")
                .toJobParameters();

        JobExecution execution = createExecution(COLLECTOR_JOB_NAME, params, BatchStatus.FAILED, new ExitStatus("FAILED", "Erro de conexão externa"));

        listener.afterJob(execution);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Deve executar o hook beforeJob sem lançar exceções")
    void shouldExecuteBeforeJobWithoutExceptions() {
        JobParameters params = new JobParametersBuilder()
                .addString("dataReferencia", "2026-10-17")
                .toJobParameters();

        JobExecution execution = createExecution(COLLECTOR_JOB_NAME, params, BatchStatus.STARTING, ExitStatus.UNKNOWN);

        assertDoesNotThrow(() -> listener.beforeJob(execution));
    }

    private JobExecution createExecution(String jobName, JobParameters params, BatchStatus status, ExitStatus exitStatus) {
        JobInstance instance = new JobInstance(100L, jobName);
        JobExecution execution = new JobExecution(instance, 200L, params);
        execution.setStatus(status);
        execution.setExitStatus(exitStatus);
        execution.setStartTime(LocalDateTime.now().minusSeconds(2));
        execution.setEndTime(LocalDateTime.now());
        return execution;
    }
}
