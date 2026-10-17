package com.example.api_docker.infra.controller.batch.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Schema(description = "Resposta assíncrona da solicitação de execução do Job Batch")
public record JobExecutionResponse(
        @Schema(description = "Nome do Job solicitado", example = "calculatorJob")
        String jobName,

        @Schema(description = "Data de referência do processamento", example = "2026-10-17")
        String dataReferencia,

        @Schema(description = "Status da solicitação no momento da resposta HTTP", example = "ACCEPTED")
        String status,

        @Schema(description = "Informa se o job principal já iniciou de imediato ou se está aguardando dependências", example = "false")
        boolean startedImmediately,

        @Schema(description = "Lista de jobs coletores disparados como dependência necessária para destravar o cálculo", example = "[\"collectorJob\"]")
        List<String> pendingDependencies,

        @Schema(description = "Identificador físico da execução no Spring Batch se já iniciado, ou null se aguardando dependências", example = "105")
        Long executionId,

        @Schema(description = "Mensagem descritiva detalhada sobre o agendamento em background", example = "Solicitação aceita. O job calculador possui dependências pendentes.")
        String mensagem,

        @Schema(description = "Data/hora exata em que a requisição foi aceita pela API", example = "2026-10-17T14:50:00Z")
        Instant timestamp
) {
    public JobExecutionResponse {
        pendingDependencies = (pendingDependencies != null)
                ? Collections.unmodifiableList(pendingDependencies)
                : Collections.emptyList();
    }

    public static JobExecutionResponse of(
            String jobName,
            String dataReferencia,
            boolean startedImmediately,
            List<String> pendingDependencies,
            Long executionId,
            String mensagem
    ) {
        return new JobExecutionResponse(
                jobName,
                dataReferencia,
                "ACCEPTED",
                startedImmediately,
                pendingDependencies,
                executionId,
                mensagem,
                Instant.now()
        );
    }
}
