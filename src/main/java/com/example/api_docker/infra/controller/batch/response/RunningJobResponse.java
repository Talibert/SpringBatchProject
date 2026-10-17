package com.example.api_docker.infra.controller.batch.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.batch.core.JobExecution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Representação detalhada de um Job do Spring Batch atualmente em execução")
public record RunningJobResponse(
        @Schema(description = "ID único da execução", example = "105")
        Long executionId,

        @Schema(description = "Nome do Job", example = "collectorJob")
        String jobName,

        @Schema(description = "Data de referência informada nos parâmetros", example = "2026-10-17")
        String dataReferencia,

        @Schema(description = "Status da execução no Spring Batch", example = "STARTED")
        String status,

        @Schema(description = "Data e hora de início da execução", example = "2026-10-17T15:00:00")
        LocalDateTime startTime,

        @Schema(description = "Tempo decorrido em segundos desde o início da execução", example = "12")
        Long durationSeconds,

        @Schema(description = "Lista de steps associados à execução e suas métricas")
        List<RunningStepResponse> activeSteps
) {
    public static RunningJobResponse from(JobExecution execution) {
        if (execution == null) {
            return null;
        }

        String dataRef = execution.getJobParameters().getString("dataReferencia");
        LocalDateTime startTime = execution.getStartTime();
        Long durationSec = startTime != null ? Duration.between(startTime, LocalDateTime.now()).toSeconds() : 0L;

        List<RunningStepResponse> steps = execution.getStepExecutions().stream()
                .map(RunningStepResponse::from)
                .toList();

        return new RunningJobResponse(
                execution.getId(),
                execution.getJobInstance() != null ? execution.getJobInstance().getJobName() : "UNKNOWN",
                dataRef != null ? dataRef : "N/A",
                execution.getStatus() != null ? execution.getStatus().name() : "UNKNOWN",
                startTime,
                durationSec,
                steps
        );
    }
}
