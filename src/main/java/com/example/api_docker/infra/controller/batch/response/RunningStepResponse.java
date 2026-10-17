package com.example.api_docker.infra.controller.batch.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.batch.core.StepExecution;

@Schema(description = "Detalhes e métricas de um Step de execução do Spring Batch")
public record RunningStepResponse(
        @Schema(description = "Nome do Step", example = "collectorStep")
        String stepName,

        @Schema(description = "Status do Step", example = "STARTED")
        String status,

        @Schema(description = "Quantidade de itens lidos", example = "100")
        long readCount,

        @Schema(description = "Quantidade de itens gravados", example = "100")
        long writeCount
) {
    public static RunningStepResponse from(StepExecution stepExecution) {
        if (stepExecution == null) {
            return null;
        }
        return new RunningStepResponse(
                stepExecution.getStepName(),
                stepExecution.getStatus() != null ? stepExecution.getStatus().name() : "UNKNOWN",
                stepExecution.getReadCount(),
                stepExecution.getWriteCount()
        );
    }
}
