package com.example.api_docker.infra.controller.batch.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.batch.core.JobExecution;

import java.time.LocalDateTime;

@Schema(description = "Detalhes e status de monitoramento de uma execução do Spring Batch")
public record JobExecutionDetailResponse(
        @Schema(description = "Identificador único da execução", example = "105")
        Long executionId,

        @Schema(description = "Nome do Job", example = "collectorJob")
        String jobName,

        @Schema(description = "Status atual no Spring Batch", example = "COMPLETED")
        String status,

        @Schema(description = "Código de saída da execução", example = "COMPLETED")
        String exitCode,

        @Schema(description = "Descrição detalhada de saída ou erro", example = "")
        String exitDescription,

        @Schema(description = "Data e hora de início", example = "2026-10-17T14:50:00")
        LocalDateTime startTime,

        @Schema(description = "Data e hora de término (null se ainda em execução)", example = "2026-10-17T14:50:03")
        LocalDateTime endTime
) {
    public static JobExecutionDetailResponse from(JobExecution execution) {
        if (execution == null) {
            return null;
        }

        return new JobExecutionDetailResponse(
                execution.getId(),
                execution.getJobInstance() != null ? execution.getJobInstance().getJobName() : "UNKNOWN",
                execution.getStatus() != null ? execution.getStatus().name() : "UNKNOWN",
                execution.getExitStatus() != null ? execution.getExitStatus().getExitCode() : "UNKNOWN",
                execution.getExitStatus() != null ? execution.getExitStatus().getExitDescription() : "",
                execution.getStartTime(),
                execution.getEndTime()
        );
    }
}
