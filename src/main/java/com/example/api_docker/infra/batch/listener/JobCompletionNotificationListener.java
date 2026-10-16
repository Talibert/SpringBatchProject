package com.example.api_docker.infra.batch.listener;

import com.example.api_docker.domain.batch.event.CollectorCompletedEvent;
import com.example.api_docker.domain.shared.DomainEventPublisher;
import com.example.api_docker.infra.batch.descriptor.BatchJobDescriptorProvider;
import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.example.api_docker.infra.batch.descriptor.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Listener responsável pelo monitoramento e auditoria do ciclo de vida dos jobs do Spring Batch.
 * <p>
 * Além de registrar métricas de início e fim, publica um evento de domínio no Apache Kafka
 * quando um Job do tipo {@link JobType#COLLECTOR} finaliza com {@link BatchStatus#COMPLETED}.
 * Esse evento é consumido pelo orquestrador para destravar jobs calculadores dependentes.
 */
@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

    private final DomainEventPublisher eventPublisher;
    private final Map<String, JobDescriptor> descriptorsByName;

    public JobCompletionNotificationListener(
            DomainEventPublisher eventPublisher,
            List<BatchJobDescriptorProvider> descriptorProviders
    ) {
        this.eventPublisher = eventPublisher;
        this.descriptorsByName = descriptorProviders.stream()
                .map(BatchJobDescriptorProvider::getDescriptor)
                .collect(Collectors.toMap(JobDescriptor::jobName, java.util.function.Function.identity(), (d1, d2) -> d1));
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Iniciando Job [{}], ID da Execução: {}, Parâmetros: {}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getId(),
                jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        BatchStatus status = jobExecution.getStatus();
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();

        long duracaoMs = (startTime != null && endTime != null)
                ? Duration.between(startTime, endTime).toMillis()
                : 0L;

        if (status == BatchStatus.COMPLETED) {
            log.info("Job [{}] finalizado com sucesso em {} ms.", jobName, duracaoMs);
            publishCollectorCompletionEventIfApplicable(jobExecution);
        } else {
            log.error("Job [{}] finalizado com status [{}] em {} ms. Descrição de saída: {}",
                    jobName, status, duracaoMs, jobExecution.getExitStatus().getExitDescription());
        }
    }

    private void publishCollectorCompletionEventIfApplicable(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        JobDescriptor descriptor = descriptorsByName.get(jobName);

        // Apenas Jobs classificados como COLLECTOR emitem notificação para orquestração
        if (descriptor != null && descriptor.type() == JobType.COLLECTOR) {
            String dataReferencia = jobExecution.getJobParameters().getString("dataReferencia");
            if (dataReferencia == null || dataReferencia.isBlank()) {
                dataReferencia = LocalDate.now().toString();
            }

            CollectorCompletedEvent event = CollectorCompletedEvent.of(
                    jobName,
                    dataReferencia,
                    jobExecution.getId(),
                    jobExecution.getStatus().name()
            );

            log.info("📢 Publicando CollectorCompletedEvent no Kafka: Job=[{}], DataRef=[{}]", jobName, dataReferencia);
            eventPublisher.publish(event);
        }
    }
}
