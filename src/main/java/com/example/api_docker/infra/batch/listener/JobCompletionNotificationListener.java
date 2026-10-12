package com.example.api_docker.infra.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Listener responsável pelo monitoramento e auditoria do ciclo de vida dos jobs do Spring Batch.
 * <p>
 * Registra logs informativos no momento em que um job é iniciado (incluindo parâmetros de execução)
 * e calcula a duração total do processamento ao término, logando o status final (sucesso ou falha)
 * juntamente com as descrições de saída.
 */
@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

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
        } else {
            log.error("Job [{}] finalizado com status [{}] em {} ms. Descrição de saída: {}",
                    jobName, status, duracaoMs, jobExecution.getExitStatus().getExitDescription());
        }
    }
}
