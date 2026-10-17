package com.example.api_docker.infra.batch.listener;

import com.example.api_docker.domain.batch.event.BatchJobCompletedEvent;
import com.example.api_docker.domain.batch.JobType;
import com.example.api_docker.domain.batch.TargetCalculatorContext;
import com.example.api_docker.domain.shared.DomainEventPublisher;
import com.example.api_docker.infra.batch.descriptor.BatchJobDescriptorProvider;
import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * quando qualquer Job registrado (seja {@link JobType#COLLECTOR} ou {@link JobType#CALCULATOR})
 * finaliza com {@link BatchStatus#COMPLETED}.
 * Esse evento é consumido pelo orquestrador e fica disponível para notificação do frontend.
 */
@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

    private final DomainEventPublisher eventPublisher;
    private final Map<String, JobDescriptor> descriptorsByName;
    private final ObjectMapper objectMapper;

    public JobCompletionNotificationListener(
            DomainEventPublisher eventPublisher,
            List<BatchJobDescriptorProvider> descriptorProviders,
            ObjectMapper objectMapper
    ) {
        this.eventPublisher = eventPublisher;
        this.descriptorsByName = descriptorProviders.stream()
                .map(BatchJobDescriptorProvider::getDescriptor)
                .collect(Collectors.toMap(JobDescriptor::jobName, java.util.function.Function.identity(), (d1, d2) -> d1));
        this.objectMapper = objectMapper;
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
            publishJobCompletionEventIfApplicable(jobExecution);
        } else {
            log.error("Job [{}] finalizado com status [{}] em {} ms. Descrição de saída: {}",
                    jobName, status, duracaoMs, jobExecution.getExitStatus().getExitDescription());
        }
    }

    private void publishJobCompletionEventIfApplicable(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        JobDescriptor descriptor = descriptorsByName.get(jobName);

        // Notifica a conclusão com sucesso de qualquer job registrado (COLLECTOR ou CALCULATOR)
        if (descriptor != null) {
            String dataReferencia = jobExecution.getJobParameters().getString("dataReferencia");
            if (dataReferencia == null || dataReferencia.isBlank()) {
                dataReferencia = LocalDate.now().toString();
            }

            String targetContextJson = jobExecution.getJobParameters().getString("targetCalculatorContext");
            TargetCalculatorContext targetContext = parseTargetContext(targetContextJson, jobName);

            BatchJobCompletedEvent event = BatchJobCompletedEvent.of(
                    jobName,
                    descriptor.type(),
                    dataReferencia,
                    jobExecution.getId(),
                    jobExecution.getStatus().name(),
                    targetContext
            );

            log.info("📢 Publicando BatchJobCompletedEvent no Kafka: Job=[{}], Tipo=[{}], DataRef=[{}], Target=[{}]",
                    jobName, descriptor.type(), dataReferencia, targetContext != null ? targetContext.targetJobName() : null);
            eventPublisher.publish(event);
        }
    }

    private TargetCalculatorContext parseTargetContext(String json, String jobName) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, TargetCalculatorContext.class);
        } catch (Exception e) {
            log.error("Erro ao desserializar targetCalculatorContext para o job [{}]", jobName, e);
            return null;
        }
    }
}
